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

import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Concrete implementation of AccountHoldingsService.
 *
 * - Concurrently fetch holdings per credential using a bounded executor.
 * - Rely on BrokerCredentialService.decryptCredentialToken(...) to validate
 * access and decrypt.
 * - Never log tokens/cleartext; zero byte[] buffers after use.
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
    private final List<BrokerClient> brokerClients;

    private final ExecutorService executor = Executors.newFixedThreadPool(
            Math.max(8, 2 * Runtime.getRuntime().availableProcessors()));

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }

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

    // ================================================================
    // HOLDINGS (MAIN FLOW)
    // ================================================================
    public Map<String, Object> aggregateHoldingsForAccount(UUID accountId, UUID callerUserId) {

        // 1) Fetch members for account
        List<AccountMember> members = memberRepo.findByAccountId(accountId);
        if (members.isEmpty()) {
            return EMPTY;
        }

        // 2) Collect credentials for all members
        CredentialBundle bundle = collectCredentials(members);
        List<BrokerCredential> creds = bundle.creds;
        Map<UUID, AccountMember> credOwner = bundle.credOwner;

        if (creds.isEmpty()) {
            return EMPTY;
        }

        // 3) Fetch holdings concurrently for each credential
        List<CompletableFuture<MemberItems<HoldingItem>>> futures = new ArrayList<>();

        for (BrokerCredential cred : creds) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                UUID credId = cred.getCredentialId();
                List<HoldingItem> items = fetchItems(cred, callerUserId, BrokerClient::getHoldings);
                return new MemberItems<>(credOwner.get(credId), items);
            }, executor));
        }

        // 4) Wait for completions, gather results
        List<MemberItems<HoldingItem>> fetched = awaitAll(futures, 30, TimeUnit.SECONDS);

        if (fetched.isEmpty()) {
            return EMPTY;
        }

        // 5) Group: AccountMember → List<HoldingItem>
        Map<AccountMember, List<HoldingItem>> byMember = groupByMember(fetched);

        if (byMember.isEmpty()) {
            return EMPTY;
        }

        // ================================================================
        // 6) Determine caller’s own membership + role
        // ================================================================
        AccountMember callerMember = members.stream()
                .filter(m -> m.getUser().getId().equals(callerUserId))
                .findFirst()
                .orElse(null);

        String callerRole = callerMember == null
                ? "MEMBER"
                : callerMember.getRole();

        // ================================================================
        // 7) ADMIN → bypass all privacy; return everything as full
        // ================================================================
        if ("ADMIN".equalsIgnoreCase(callerRole)) {

            // Combine ALL member holdings into single list
            List<HoldingItem> all = new ArrayList<>();
            for (List<HoldingItem> list : byMember.values()) {
                all.addAll(list);
            }

            // Run aggregation on full list
            List<AggregatedHolding> aggregated = aggregateHoldings(all);

            return Map.of(
                    "full", aggregated,
                    "partial", Collections.emptyList());
        }

        // ================================================================
        // 8) MEMBER CALLER → apply privacy rules member-by-member
        // ================================================================
        List<HoldingItem> fullInput = new ArrayList<>();
        Set<String> partialTickers = new HashSet<>();

        for (Map.Entry<AccountMember, List<HoldingItem>> entry : byMember.entrySet()) {

            AccountMember m = entry.getKey();
            List<HoldingItem> theirItems = entry.getValue();

            // Determine privacy from rules JSON
            String privacy = extractPrivacy(m.getRules());

            boolean detailedForCaller = m.equals(callerMember) ||
                    "DETAILED".equalsIgnoreCase(privacy);

            if (detailedForCaller) {
                fullInput.addAll(theirItems);
                continue;
            }

            if ("SUMMARY".equalsIgnoreCase(privacy)) {
                for (HoldingItem h : theirItems) {
                    if (h.getTradingSymbol() != null) {
                        partialTickers.add(h.getTradingSymbol());
                    }
                }
                continue;
            }

            // PRIVATE → ignore entirely
            // no action
        }

        // ================================================================
        // 9) Aggregate the FULL holdings list (existing aggregation logic)
        // ================================================================
        List<AggregatedHolding> aggregatedFull = aggregateHoldings(fullInput);

        // Final output object shape
        Map<String, Object> finalResult = Map.of(
                "full", aggregatedFull,
                "partial", partialTickers);

        return finalResult;
    }

    // ================================================================
    // POSITIONS FLOW (MIRROR TO HOLDINGS)
    // ================================================================
    public Map<String, Object> aggregatePositionsForAccount(UUID accountId, UUID callerUserId) {

        // 1) Fetch members for the account
        List<AccountMember> members = memberRepo.findByAccountId(accountId);
        if (members.isEmpty()) {
            return EMPTY;
        }

        // 2) Collect creds for all members
        CredentialBundle bundle = collectCredentials(members);
        List<BrokerCredential> creds = bundle.creds;
        Map<UUID, AccountMember> credOwner = bundle.credOwner;

        if (creds.isEmpty()) {
            return EMPTY;
        }

        // 3) Concurrent fetch positions
        List<CompletableFuture<MemberItems<PositionItem>>> futures = new ArrayList<>();

        for (BrokerCredential cred : creds) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                UUID credId = cred.getCredentialId();
                List<PositionItem> items = fetchItems(cred, callerUserId, BrokerClient::getPositions);
                return new MemberItems<>(credOwner.get(credId), items);
            }, executor));
        }

        // 4) Collect results
        List<MemberItems<PositionItem>> fetched = awaitAll(futures, 30, TimeUnit.SECONDS);

        if (fetched.isEmpty()) {
            return EMPTY;
        }

        // 5) Group: AccountMember → List<PositionItem>
        Map<AccountMember, List<PositionItem>> byMember = groupByMember(fetched);

        if (byMember.isEmpty()) {
            return EMPTY;
        }

        // 6) Determine caller
        AccountMember callerMember = members.stream()
                .filter(m -> m.getUser().getId().equals(callerUserId))
                .findFirst()
                .orElse(null);

        String callerRole = callerMember == null
                ? "MEMBER"
                : callerMember.getRole();

        // 7) ADMIN bypass
        if ("ADMIN".equalsIgnoreCase(callerRole)) {

            List<PositionItem> all = new ArrayList<>();
            for (List<PositionItem> list : byMember.values()) {
                all.addAll(list);
            }

            List<AggregatedPosition> aggregated = aggregatePositions(all);

            return Map.of(
                    "full", aggregated,
                    "partial", Collections.emptyList());
        }

        // 8) MEMBER privacy filtering
        List<PositionItem> fullInput = new ArrayList<>();
        Set<String> partialTickers = new HashSet<>();

        for (Map.Entry<AccountMember, List<PositionItem>> entry : byMember.entrySet()) {

            AccountMember m = entry.getKey();
            List<PositionItem> items = entry.getValue();
            String privacy = extractPrivacy(m.getRules());

            boolean detailedForCaller = m.equals(callerMember) ||
                    "DETAILED".equalsIgnoreCase(privacy);

            if (detailedForCaller) {
                fullInput.addAll(items);
                continue;
            }

            if ("SUMMARY".equalsIgnoreCase(privacy)) {
                for (PositionItem p : items) {
                    if (p.getTradingSymbol() != null) {
                        partialTickers.add(p.getTradingSymbol());
                    }
                }
                continue;
            }

            // PRIVATE → skip
        }

        List<AggregatedPosition> aggregatedFull = aggregatePositions(fullInput);

        return Map.of(
                "full", aggregatedFull,
                "partial", partialTickers);
    }

    // ================================================================
    // PRIVACY PARSER
    // Extracts "privacy" from rules JSON ("DETAILED", "SUMMARY", "PRIVATE").
    // Defaults to DETAILED on errors or missing value.
    // ================================================================
    private String extractPrivacy(String rulesJson) {
        if (rulesJson == null || rulesJson.isBlank())
            return "PRIVATE";

        try {
            // Very minimal JSON parsing: look for "privacy":"VALUE"
            int keyIdx = rulesJson.indexOf("\"privacy\"");
            if (keyIdx < 0)
                return "PRIVATE";

            int colonIdx = rulesJson.indexOf(':', keyIdx);
            if (colonIdx < 0)
                return "PRIVATE";

            int firstQuote = rulesJson.indexOf('"', colonIdx + 1);
            if (firstQuote < 0)
                return "PRIVATE";

            int secondQuote = rulesJson.indexOf('"', firstQuote + 1);
            if (secondQuote < 0)
                return "PRIVATE";

            String value = rulesJson.substring(firstQuote + 1, secondQuote).trim().toUpperCase();
            return switch (value) {
                case "DETAILED", "SUMMARY", "PRIVATE" -> value;
                default -> "DETAILED";
            };

        } catch (Exception ex) {
            return "DETAILED";
        }
    }

    // ================================================================
    // HOLDINGS AGGREGATION
    // ================================================================
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

    // ================================================================
    // POSITIONS AGGREGATION
    // ================================================================
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

    // ================================================================
    // BROKER LOOKUP (unchanged)
    // ================================================================
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

    // ================================================================
    // SAFE ID LOGGER (unchanged)
    // ================================================================
    private static String safeIdString(BrokerCredential cred) {
        try {
            UUID id = cred.getCredentialId();
            return id == null ? "unknown" : id.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }

    // ================================================================
    // HOLDING ACCUMULATOR
    // ================================================================
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

    // ================================================================
    // POSITION ACCUMULATOR
    // ================================================================
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
