package com.marmik.brokerhub.service;

import com.marmik.brokerhub.dto.AggregatedHolding;
import com.marmik.brokerhub.broker.dto.HoldingItem;
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
public class AccountHoldingsService {

    private static final Logger log = LoggerFactory.getLogger(AccountHoldingsService.class);

    private final AccountMemberRepository memberRepo;
    private final BrokerCredentialRepository credentialRepo;
    private final BrokerCredentialService credentialService;
    private final List<BrokerClient> brokerClients; // autodiscovered beans like DhanService

    // Executor for concurrent calls (bounded)
    private final ExecutorService executor = Executors
            .newFixedThreadPool(Math.max(8, 2 * Runtime.getRuntime().availableProcessors()));

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }

    public List<AggregatedHolding> aggregateHoldingsForAccount(UUID accountId, UUID callerUserId) {
        // 1) fetch members for account
        List<AccountMember> members = memberRepo.findByAccountId(accountId);
        if (members.isEmpty()) {
            return Collections.emptyList();
        }

        // 2) collect credentials for all members
        List<BrokerCredential> allCreds = new ArrayList<>();
        for (AccountMember m : members) {
            try {
                List<BrokerCredential> creds = credentialRepo.findByAccountMemberId(m.getId());
                if (creds != null && !creds.isEmpty())
                    allCreds.addAll(creds);
            } catch (Exception ex) {
                // don't fail entire flow for one member; log minimal info
                log.warn("Failed to list credentials for member {}", m.getId());
            }
        }

        if (allCreds.isEmpty()) {
            return Collections.emptyList();
        }

        // 3) For each credential, perform decrypt + broker call concurrently
        List<CompletableFuture<List<HoldingItem>>> futures = new ArrayList<>();
        for (BrokerCredential cred : allCreds) {
            CompletableFuture<List<HoldingItem>> f = CompletableFuture.supplyAsync(() -> {
                byte[] plain = null;
                try {
                    // decrypt token (validates caller membership internally)
                    // NOTE: use the actual getter name for credential id below
                    UUID credId = cred.getCredentialId(); // <--- use actual getter on your entity
                    plain = credentialService.decryptCredentialToken(callerUserId, credId);
                    if (plain == null || plain.length == 0) {
                        return Collections.<HoldingItem>emptyList();
                    }

                    String token = new String(plain, StandardCharsets.UTF_8);

                    // select broker client
                    BrokerClient client = findClientForBroker(cred.getBroker());
                    if (client == null) {
                        log.warn("No broker client for type {}", cred.getBroker());
                        return Collections.<HoldingItem>emptyList();
                    }

                    // call broker client to get holdings
                    List<HoldingItem> holdings = client.getHoldings(token);

                    return holdings == null ? Collections.emptyList() : holdings;
                } catch (Exception e) {
                    log.warn("Failed to fetch holdings for credential {}",
                            // avoid printing sensitive identifiers; print only non-secret id
                            // use cred.getCredentialId() which is just a UUID
                            safeIdString(cred));
                    return Collections.<HoldingItem>emptyList();
                } finally {
                    if (plain != null) {
                        Arrays.fill(plain, (byte) 0);
                    }
                }
            }, executor);
            futures.add(f);
        }

        // wait for completions (with timeout safety)
        List<HoldingItem> combined = new ArrayList<>();
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, TimeUnit.SECONDS);
            for (CompletableFuture<List<HoldingItem>> cf : futures) {
                try {
                    List<HoldingItem> list = cf.getNow(Collections.emptyList());
                    if (list != null && !list.isEmpty())
                        combined.addAll(list);
                } catch (CompletionException ce) {
                    // skip failed task
                }
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            // partial results allowed — collect what finished so far
            for (CompletableFuture<List<HoldingItem>> cf : futures) {
                if (cf.isDone() && !cf.isCompletedExceptionally()) {
                    try {
                        List<HoldingItem> list = cf.get();
                        if (list != null && !list.isEmpty())
                            combined.addAll(list);
                    } catch (Exception ignored) {
                    }
                }
            }
            futures.forEach(f -> {
                if (!f.isDone())
                    f.cancel(true);
            });
        }

        if (combined.isEmpty()) {
            return Collections.emptyList();
        }

        // 4) Aggregate by exchange+tradingSymbol (key)
        Map<String, Accumulator> acc = new HashMap<>();
        for (HoldingItem h : combined) {
            String key = (h.getExchange() == null ? "" : h.getExchange()) + "::" + h.getTradingSymbol();
            Accumulator a = acc.computeIfAbsent(key,
                    k -> new Accumulator(h.getExchange(), h.getTradingSymbol(), h.getIsin()));
            a.add(h);
        }

        // 5) Convert accumulators to DTOs
        List<AggregatedHolding> result = acc.values().stream()
                .map(Accumulator::toAggregatedHolding)
                .collect(Collectors.toList());

        return result;
    }

    private BrokerClient findClientForBroker(String broker) {
        if (broker == null)
            return null;
        for (BrokerClient c : brokerClients) {
            try {
                if (broker.equalsIgnoreCase(c.getBrokerType()))
                    return c;
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static String safeIdString(BrokerCredential cred) {
        try {
            UUID id = cred.getCredentialId();
            return id == null ? "unknown" : id.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Helper accumulator class for aggregation logic (weighted average etc.)
     */
    private static class Accumulator {
        private final String exchange;
        private final String tradingSymbol;
        private final String isin;

        private long totalQty = 0;
        private double totalValue = 0.0; // sum(qty * avgPrice)
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

            // prefer non-zero lastPrice: pick most recent non-zero encountered
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
}
