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

    // A wrapper class to keep track of which member owns which holdings.
    private static class MemberHoldings {
        final AccountMember member;
        final List<HoldingItem> holdings;

        MemberHoldings(AccountMember m, List<HoldingItem> h) {
            this.member = m;
            this.holdings = h;
        }
    }

    // For positions
    private static class MemberPositions {
        final AccountMember member;
        final List<PositionItem> positions;

        MemberPositions(AccountMember m, List<PositionItem> p) {
            this.member = m;
            this.positions = p;
        }
    }

    // ================================================================
    // HOLDINGS (MAIN FLOW)
    // ================================================================
    public Map<String, Object> aggregateHoldingsForAccount(UUID accountId, UUID callerUserId) {

        // 1) Fetch members for account
        List<AccountMember> members = memberRepo.findByAccountId(accountId);
        if (members.isEmpty()) {
            return Map.of("full", Collections.emptyList(), "partial", Collections.emptyList());
        }

        // Build a lookup for each credential → its owning account member
        Map<UUID, AccountMember> credOwner = new HashMap<>();

        // 2) Collect credentials for all members
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

        if (creds.isEmpty()) {
            return Map.of("full", Collections.emptyList(), "partial", Collections.emptyList());
        }

        // 3) Fetch holdings concurrently for each credential
        List<CompletableFuture<MemberHoldings>> futures = new ArrayList<>();

        for (BrokerCredential cred : creds) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                byte[] plain = null;
                try {
                    UUID credId = cred.getCredentialId();

                    // decrypt token (validates access)
                    plain = credentialService.decryptCredentialToken(callerUserId, credId);
                    if (plain == null || plain.length == 0) {
                        return new MemberHoldings(credOwner.get(credId), Collections.emptyList());
                    }

                    String token = new String(plain, StandardCharsets.UTF_8);
                    BrokerClient client = findClientForBroker(cred.getBroker());

                    if (client == null) {
                        log.warn("No broker client for type {}", cred.getBroker());
                        return new MemberHoldings(credOwner.get(credId), Collections.emptyList());
                    }

                    List<HoldingItem> h = client.getHoldings(token);
                    if (h == null)
                        h = Collections.emptyList();

                    return new MemberHoldings(credOwner.get(credId), h);

                } catch (Exception e) {
                    log.warn("Failed to fetch holdings for credential {}", safeIdString(cred));
                    return new MemberHoldings(credOwner.get(cred.getCredentialId()), Collections.emptyList());
                } finally {
                    if (plain != null)
                        Arrays.fill(plain, (byte) 0);
                }
            }, executor));
        }

        // 4) Wait for completions, gather results
        List<MemberHoldings> fetched = new ArrayList<>();

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);

            for (CompletableFuture<MemberHoldings> cf : futures) {
                try {
                    MemberHoldings mh = cf.getNow(null);
                    if (mh != null)
                        fetched.add(mh);
                } catch (Exception ignored) {
                }
            }

        } catch (Exception ex) {
            // gather partial results
            for (CompletableFuture<MemberHoldings> cf : futures) {
                if (cf.isDone() && !cf.isCompletedExceptionally()) {
                    try {
                        MemberHoldings mh = cf.get();
                        if (mh != null)
                            fetched.add(mh);
                    } catch (Exception ignored) {
                    }
                }
            }
            futures.forEach(f -> {
                if (!f.isDone())
                    f.cancel(true);
            });
        }

        if (fetched.isEmpty()) {
            return Map.of("full", Collections.emptyList(), "partial", Collections.emptyList());
        }

        // 5) Group: AccountMember → List<HoldingItem>
        Map<AccountMember, List<HoldingItem>> byMember = new HashMap<>();

        for (MemberHoldings mh : fetched) {
            if (mh.member == null)
                continue;
            byMember.computeIfAbsent(mh.member, k -> new ArrayList<>())
                    .addAll(mh.holdings);
        }

        if (byMember.isEmpty()) {
            return Map.of("full", Collections.emptyList(), "partial", Collections.emptyList());
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
            return Map.of("full", Collections.emptyList(), "partial", Collections.emptyList());
        }

        // Lookup credential → owner
        Map<UUID, AccountMember> credOwner = new HashMap<>();

        // 2) Collect creds for all members
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

        if (creds.isEmpty()) {
            return Map.of("full", Collections.emptyList(), "partial", Collections.emptyList());
        }

        // 3) Concurrent fetch positions
        List<CompletableFuture<MemberPositions>> futures = new ArrayList<>();

        for (BrokerCredential cred : creds) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                byte[] plain = null;
                try {
                    UUID credId = cred.getCredentialId();
                    plain = credentialService.decryptCredentialToken(callerUserId, credId);

                    if (plain == null || plain.length == 0) {
                        return new MemberPositions(credOwner.get(credId), Collections.emptyList());
                    }

                    String token = new String(plain, StandardCharsets.UTF_8);
                    BrokerClient client = findClientForBroker(cred.getBroker());

                    if (client == null) {
                        log.warn("No broker client for broker {}", cred.getBroker());
                        return new MemberPositions(credOwner.get(credId), Collections.emptyList());
                    }

                    List<PositionItem> positions = client.getPositions(token);
                    if (positions == null)
                        positions = Collections.emptyList();

                    return new MemberPositions(credOwner.get(credId), positions);

                } catch (Exception e) {
                    log.warn("Failed to fetch positions for credential {}", safeIdString(cred));
                    return new MemberPositions(credOwner.get(cred.getCredentialId()), Collections.emptyList());
                } finally {
                    if (plain != null)
                        Arrays.fill(plain, (byte) 0);
                }

            }, executor));
        }

        // 4) Collect results
        List<MemberPositions> fetched = new ArrayList<>();

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);

            for (CompletableFuture<MemberPositions> cf : futures) {
                try {
                    MemberPositions mp = cf.getNow(null);
                    if (mp != null)
                        fetched.add(mp);
                } catch (Exception ignored) {
                }
            }

        } catch (Exception e) {

            for (CompletableFuture<MemberPositions> cf : futures) {
                if (cf.isDone() && !cf.isCompletedExceptionally()) {
                    try {
                        MemberPositions mp = cf.get();
                        if (mp != null)
                            fetched.add(mp);
                    } catch (Exception ignored) {
                    }
                }
            }

            futures.forEach(f -> {
                if (!f.isDone())
                    f.cancel(true);
            });
        }

        if (fetched.isEmpty()) {
            return Map.of("full", Collections.emptyList(), "partial", Collections.emptyList());
        }

        // 5) Group: AccountMember → List<PositionItem>
        Map<AccountMember, List<PositionItem>> byMember = new HashMap<>();

        for (MemberPositions mp : fetched) {
            if (mp.member == null)
                continue;
            byMember.computeIfAbsent(mp.member, k -> new ArrayList<>())
                    .addAll(mp.positions);
        }

        if (byMember.isEmpty()) {
            return Map.of("full", Collections.emptyList(), "partial", Collections.emptyList());
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
