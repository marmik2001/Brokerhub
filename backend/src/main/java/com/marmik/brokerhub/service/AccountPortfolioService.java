package com.marmik.brokerhub.service;

import com.marmik.brokerhub.dto.AggregatedHolding;
import com.marmik.brokerhub.dto.AggregatedPosition;
import com.marmik.brokerhub.broker.dto.HoldingItem;
import com.marmik.brokerhub.broker.dto.PositionItem;
import com.marmik.brokerhub.broker.core.BrokerClient;
import com.marmik.brokerhub.model.AccountMember;
import com.marmik.brokerhub.model.BrokerCredential;
import com.marmik.brokerhub.repository.AccountMemberRepository;
import com.marmik.brokerhub.repository.BrokerCredentialRepository;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

import org.springframework.core.task.TaskExecutor;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Aggregates holdings and positions across all members in an account.
 *
 * Design notes:
 * - Fetches broker data concurrently with a bounded executor.
 * - Uses BrokerCredentialService for authorization + token decryption.
 * - Applies member privacy rules before producing caller-visible output.
 * - Avoids logging secret values and zeroes decrypted token bytes after use.
 */
@Service
@RequiredArgsConstructor
public class AccountPortfolioService {

    private static final Logger log = LoggerFactory.getLogger(AccountPortfolioService.class);
    private static final Map<String, Object> EMPTY = Map.of(
            "full", Collections.emptyList(),
            "partial", Collections.emptyList());

    private final AccountMemberRepository memberRepo;
    private final BrokerCredentialRepository credentialRepo;
    private final BrokerCredentialService credentialService;
    private final BrokerHoldingsCacheService holdingsCacheService;
    private final List<BrokerClient> brokerClients;
    private final TaskExecutor taskExecutor;

    private static final class CredentialBundle {
        final List<BrokerCredential> creds;
        final Map<UUID, AccountMember> credOwner;

        private CredentialBundle(List<BrokerCredential> creds, Map<UUID, AccountMember> credOwner) {
            this.creds = creds;
            this.credOwner = credOwner;
        }
    }

    private static final class MemberItems<T> {
        final AccountMember member;
        final List<T> items;

        private MemberItems(AccountMember member, List<T> items) {
            this.member = member;
            this.items = items;
        }
    }

    private enum PrivacyLevel {
        DETAILED,
        SUMMARY,
        PRIVATE
    }

    private static final class VisibilityResult<T> {
        final List<T> fullItems;
        final Set<String> partialTickers;

        private VisibilityResult(List<T> fullItems, Set<String> partialTickers) {
            this.fullItems = fullItems;
            this.partialTickers = partialTickers;
        }
    }

    private CredentialBundle collectCredentials(List<AccountMember> members) {
        Map<UUID, AccountMember> credOwner = new HashMap<>();
        List<BrokerCredential> creds = new ArrayList<>();

        for (AccountMember m : members) {
            try {
                List<BrokerCredential> list = credentialRepo.findByAccountMemberId(m.getId());
                if (list != null) {
                    creds.addAll(list);
                    for (BrokerCredential bc : list) {
                        credOwner.put(bc.getCredentialId(), m);
                    }
                }
            } catch (Exception ex) {
                log.warn("Failed to list credentials for member {}", m.getId());
            }
        }

        return new CredentialBundle(creds, credOwner);
    }

    private <R> List<R> awaitAll(List<CompletableFuture<R>> futures, long timeout, TimeUnit unit) {
        List<R> fetched = new ArrayList<>();

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(timeout, unit);
            for (CompletableFuture<R> cf : futures) {
                try {
                    R r = cf.getNow(null);
                    if (r != null) {
                        fetched.add(r);
                    }
                } catch (Exception ignored) {
                }
            }
            return fetched;
        } catch (Exception ex) {
            for (CompletableFuture<R> cf : futures) {
                if (cf.isDone() && !cf.isCompletedExceptionally()) {
                    try {
                        R r = cf.get();
                        if (r != null) {
                            fetched.add(r);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
            futures.forEach(f -> {
                if (!f.isDone()) {
                    f.cancel(true);
                }
            });
            return fetched;
        }
    }

    private <T> Map<AccountMember, List<T>> groupByMember(List<? extends MemberItems<T>> fetched) {
        Map<AccountMember, List<T>> byMember = new HashMap<>();
        for (MemberItems<T> mi : fetched) {
            if (mi == null || mi.member == null) {
                continue;
            }
            byMember.computeIfAbsent(mi.member, k -> new ArrayList<>())
                    .addAll(mi.items);
        }
        return byMember;
    }

    private <T> List<T> fetchItems(
            BrokerCredential cred,
            UUID callerUserId,
            BiFunction<BrokerClient, String, List<T>> brokerCall) {
        byte[] plain = null;
        try {
            UUID credId = cred.getCredentialId();
            plain = credentialService.decryptCredentialToken(callerUserId, credId);
            if (plain == null || plain.length == 0) {
                return Collections.emptyList();
            }

            String token = new String(plain, StandardCharsets.UTF_8);
            BrokerClient client = findClientForBroker(cred.getBroker());
            if (client == null) {
                log.warn("No broker client for broker {}", cred.getBroker());
                return Collections.emptyList();
            }

            List<T> out = brokerCall.apply(client, token);
            return out == null ? Collections.emptyList() : out;
        } catch (Exception e) {
            log.warn("Failed to fetch data for credential {}", safeIdString(cred));
            return Collections.emptyList();
        } finally {
            if (plain != null) {
                Arrays.fill(plain, (byte) 0);
            }
        }
    }

    private <T> Map<AccountMember, List<T>> fetchByMember(
            List<BrokerCredential> creds,
            Map<UUID, AccountMember> credOwner,
            UUID callerUserId,
            BiFunction<BrokerClient, String, List<T>> brokerCall) {

        List<CompletableFuture<MemberItems<T>>> futures = new ArrayList<>();

        for (BrokerCredential cred : creds) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                UUID credId = cred.getCredentialId();
                List<T> items = fetchItems(cred, callerUserId, brokerCall);
                return new MemberItems<>(credOwner.get(credId), items);
            }, taskExecutor));
        }

        List<MemberItems<T>> fetched = awaitAll(futures, 30, TimeUnit.SECONDS);
        if (fetched.isEmpty()) {
            return Collections.emptyMap();
        }

        return groupByMember(fetched);
    }

    private List<HoldingItem> fetchHoldingsWithCache(BrokerCredential cred, UUID callerUserId) {
        UUID credId = cred.getCredentialId();
        Optional<List<HoldingItem>> cached = holdingsCacheService.getCachedHoldings(cred.getBroker(), credId);
        if (cached.isPresent()) {
            return cached.get();
        }

        List<HoldingItem> fetched = fetchItems(cred, callerUserId, BrokerClient::getHoldings);
        if (fetched != null && !fetched.isEmpty()) {
            holdingsCacheService.cacheHoldings(cred.getBroker(), credId, fetched);
        }
        return fetched == null ? Collections.emptyList() : fetched;
    }

    private Map<AccountMember, List<HoldingItem>> fetchHoldingsByMember(
            List<BrokerCredential> creds,
            Map<UUID, AccountMember> credOwner,
            UUID callerUserId) {

        List<CompletableFuture<MemberItems<HoldingItem>>> futures = new ArrayList<>();

        for (BrokerCredential cred : creds) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                UUID credId = cred.getCredentialId();
                List<HoldingItem> items = fetchHoldingsWithCache(cred, callerUserId);
                return new MemberItems<>(credOwner.get(credId), items);
            }, taskExecutor));
        }

        List<MemberItems<HoldingItem>> fetched = awaitAll(futures, 30, TimeUnit.SECONDS);
        if (fetched.isEmpty()) {
            return Collections.emptyMap();
        }

        return groupByMember(fetched);
    }

    private AccountMember findCallerMember(List<AccountMember> members, UUID callerUserId) {
        return members.stream()
                .filter(m -> m.getUser().getId().equals(callerUserId))
                .findFirst()
                .orElse(null);
    }

    private boolean isAdmin(AccountMember callerMember) {
        String callerRole = callerMember == null ? "MEMBER" : callerMember.getRole();
        return "ADMIN".equalsIgnoreCase(callerRole);
    }

    private <T> VisibilityResult<T> applyPrivacyRules(
            Map<AccountMember, List<T>> byMember,
            AccountMember callerMember,
            java.util.function.Function<T, String> symbolExtractor) {

        List<T> fullInput = new ArrayList<>();
        Set<String> partialTickers = new HashSet<>();

        for (Map.Entry<AccountMember, List<T>> entry : byMember.entrySet()) {
            AccountMember m = entry.getKey();
            List<T> items = entry.getValue();

            PrivacyLevel privacy = extractPrivacyLevel(m.getRules());
            boolean detailedForCaller = m.equals(callerMember) || privacy == PrivacyLevel.DETAILED;

            if (detailedForCaller) {
                fullInput.addAll(items);
                continue;
            }

            if (privacy == PrivacyLevel.SUMMARY) {
                for (T item : items) {
                    String symbol = symbolExtractor.apply(item);
                    if (symbol != null) {
                        partialTickers.add(symbol);
                    }
                }
            }
        }

        return new VisibilityResult<>(fullInput, partialTickers);
    }

    // ---------- Holdings flow ----------
    public Map<String, Object> aggregateHoldingsForAccount(UUID accountId, UUID callerUserId) {

        // 1) Load members.
        List<AccountMember> members = memberRepo.findByAccountId(accountId);
        if (members.isEmpty()) {
            return EMPTY;
        }

        // 2) Load broker credentials and map each credential to its owner member.
        CredentialBundle bundle = collectCredentials(members);
        List<BrokerCredential> creds = bundle.creds;
        Map<UUID, AccountMember> credOwner = bundle.credOwner;

        if (creds.isEmpty()) {
            return EMPTY;
        }

        // 3) Fetch holdings concurrently and regroup by member.
        Map<AccountMember, List<HoldingItem>> byMember = fetchHoldingsByMember(
                creds,
                credOwner,
                callerUserId);

        if (byMember.isEmpty()) {
            return EMPTY;
        }

        // 4) Resolve caller membership in this account.
        AccountMember callerMember = findCallerMember(members, callerUserId);

        // 5) Admins see full data for everyone.
        if (isAdmin(callerMember)) {

            // Flatten all holdings before aggregation.
            List<HoldingItem> all = new ArrayList<>();
            for (List<HoldingItem> list : byMember.values()) {
                all.addAll(list);
            }

            // Aggregate full holdings.
            List<AggregatedHolding> aggregated = aggregateHoldings(all);

            return Map.of(
                    "full", aggregated,
                    "partial", Collections.emptyList());
        }

        // 6) Non-admins are filtered by privacy rules.
        VisibilityResult<HoldingItem> visible = applyPrivacyRules(
                byMember,
                callerMember,
                HoldingItem::getTradingSymbol);

        // 7) Aggregate only caller-visible full items.
        List<AggregatedHolding> aggregatedFull = aggregateHoldings(visible.fullItems);

        // Preserve response contract: full aggregated data + partial symbols.
        Map<String, Object> finalResult = Map.of(
                "full", aggregatedFull,
                "partial", visible.partialTickers);

        return finalResult;
    }

    // ---------- Positions flow (same pipeline as holdings) ----------
    public Map<String, Object> aggregatePositionsForAccount(UUID accountId, UUID callerUserId) {

        // 1) Load members.
        List<AccountMember> members = memberRepo.findByAccountId(accountId);
        if (members.isEmpty()) {
            return EMPTY;
        }

        // 2) Load broker credentials and map each credential to its owner member.
        CredentialBundle bundle = collectCredentials(members);
        List<BrokerCredential> creds = bundle.creds;
        Map<UUID, AccountMember> credOwner = bundle.credOwner;

        if (creds.isEmpty()) {
            return EMPTY;
        }

        // 3) Fetch positions concurrently and regroup by member.
        Map<AccountMember, List<PositionItem>> byMember = fetchByMember(
                creds,
                credOwner,
                callerUserId,
                BrokerClient::getPositions);

        if (byMember.isEmpty()) {
            return EMPTY;
        }

        // 4) Resolve caller membership.
        AccountMember callerMember = findCallerMember(members, callerUserId);

        // 5) Admins see full data for everyone.
        if (isAdmin(callerMember)) {

            List<PositionItem> all = new ArrayList<>();
            for (List<PositionItem> list : byMember.values()) {
                all.addAll(list);
            }

            List<AggregatedPosition> aggregated = aggregatePositions(all);

            return Map.of(
                    "full", aggregated,
                    "partial", Collections.emptyList());
        }

        // 6) Non-admins are filtered by privacy rules.
        VisibilityResult<PositionItem> visible = applyPrivacyRules(
                byMember,
                callerMember,
                PositionItem::getTradingSymbol);

        List<AggregatedPosition> aggregatedFull = aggregatePositions(visible.fullItems);

        return Map.of(
                "full", aggregatedFull,
                "partial", visible.partialTickers);
    }

    /**
     * Parses privacy level from rules JSON.
     *
     * Expected values: DETAILED, SUMMARY, PRIVATE.
     * Defaults:
     * - missing/blank/malformed key -> PRIVATE
     * - unrecognized value or parser error -> DETAILED
     */
    private PrivacyLevel extractPrivacyLevel(Map<String, Object> rules) {
        if (rules == null || !rules.containsKey("privacy"))
            return PrivacyLevel.PRIVATE;

        try {
            String value = String.valueOf(rules.get("privacy")).trim().toUpperCase();
            return switch (value) {
                case "DETAILED" -> PrivacyLevel.DETAILED;
                case "SUMMARY" -> PrivacyLevel.SUMMARY;
                case "PRIVATE" -> PrivacyLevel.PRIVATE;
                default -> PrivacyLevel.DETAILED;
            };
        } catch (Exception ex) {
            return PrivacyLevel.DETAILED;
        }
    }

    // ---------- Holdings aggregation ----------
    private List<AggregatedHolding> aggregateHoldings(List<HoldingItem> list) {
        if (list == null || list.isEmpty())
            return Collections.emptyList();

        Map<String, Accumulator> acc = new HashMap<>();

        for (HoldingItem h : list) {
            String key = (h.getExchange() == null ? "" : h.getExchange()) + "::" + h.getTradingSymbol();

            Accumulator a = acc.computeIfAbsent(key,
                    k -> new Accumulator(h.getExchange(), h.getTradingSymbol(), h.getIsin()));

            a.add(h);
        }

        return acc.values().stream()
                .map(Accumulator::toAggregatedHolding)
                .collect(Collectors.toList());
    }

    // ---------- Positions aggregation ----------
    private List<AggregatedPosition> aggregatePositions(List<PositionItem> list) {
        if (list == null || list.isEmpty())
            return Collections.emptyList();

        Map<String, PositionAccumulator> acc = new HashMap<>();

        for (PositionItem p : list) {
            String key = (p.getExchange() == null ? "" : p.getExchange()) + "::" + p.getTradingSymbol();

            PositionAccumulator a = acc.computeIfAbsent(key,
                    k -> new PositionAccumulator(p.getExchange(), p.getTradingSymbol()));

            a.add(p);
        }

        return acc.values().stream()
                .map(PositionAccumulator::toAggregatedPosition)
                .collect(Collectors.toList());
    }

    // Resolves broker implementation from injected broker clients.
    private BrokerClient findClientForBroker(String broker) {
        if (broker == null)
            return null;

        for (BrokerClient c : brokerClients) {
            try {
                if (broker.equalsIgnoreCase(c.getBrokerType())) {
                    return c;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    // Safe helper for loggable credential identifier.
    private static String safeIdString(BrokerCredential cred) {
        try {
            UUID id = cred.getCredentialId();
            return id == null ? "unknown" : id.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }

    // Internal accumulator for grouped holding math.
    private static class Accumulator {
        private final String exchange;
        private final String tradingSymbol;
        private final String isin;

        private long totalQty = 0;
        private double totalValue = 0.0;
        private double pnlSum = 0.0;

        private double lastPrice = 0.0;
        private double dayChange = 0.0;
        private double dayChangePercentage = 0.0;

        Accumulator(String exchange, String tradingSymbol, String isin) {
            this.exchange = exchange;
            this.tradingSymbol = tradingSymbol;
            this.isin = isin;
        }

        void add(HoldingItem h) {
            long qty = h.getQuantity();
            double avgPrice = h.getAveragePrice();

            totalQty += qty;
            totalValue += qty * avgPrice;
            pnlSum += h.getPnl();

            if (h.getLastPrice() != 0.0) {
                lastPrice = h.getLastPrice();
                dayChange = h.getDayChange();
                dayChangePercentage = h.getDayChangePercentage();
            }
        }

        AggregatedHolding toAggregatedHolding() {
            double avg = totalQty == 0 ? 0.0 : (totalValue / (double) totalQty);
            return AggregatedHolding.builder()
                    .exchange(exchange)
                    .tradingSymbol(tradingSymbol)
                    .isin(isin)
                    .quantity(totalQty)
                    .averagePrice(avg)
                    .pnl(pnlSum)
                    .lastPrice(lastPrice)
                    .dayChange(dayChange)
                    .dayChangePercentage(dayChangePercentage)
                    .build();
        }
    }

    // Internal accumulator for grouped position math.
    private static class PositionAccumulator {

        private final String exchange;
        private final String tradingSymbol;

        private long totalQty = 0;
        private double totalValue = 0.0;
        private double pnlSum = 0.0;
        private double lastPrice = 0.0;
        private double dayChange = 0.0;
        private double dayChangePercentage = 0.0;

        PositionAccumulator(String exchange, String tradingSymbol) {
            this.exchange = exchange;
            this.tradingSymbol = tradingSymbol;
        }

        void add(PositionItem p) {
            long qty = p.getQuantity();
            double avgPrice = p.getAveragePrice();
            totalQty += qty;
            totalValue += qty * avgPrice;
            pnlSum += p.getTotalPnl();

            if (p.getLastPrice() != 0.0) {
                lastPrice = p.getLastPrice();
            }
        }

        AggregatedPosition toAggregatedPosition() {
            double avg = totalQty == 0 ? 0.0 : (totalValue / (double) totalQty);
            return AggregatedPosition.builder()
                    .exchange(exchange)
                    .tradingSymbol(tradingSymbol)
                    .quantity(totalQty)
                    .averagePrice(avg)
                    .pnl(pnlSum)
                    .lastPrice(lastPrice)
                    .dayChange(dayChange)
                    .dayChangePercentage(dayChangePercentage)
                    .build();
        }
    }
}
