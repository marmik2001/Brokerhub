package com.marmik.brokerhub.service;

import com.marmik.brokerhub.broker.core.BrokerClient;
import com.marmik.brokerhub.broker.dto.HoldingItem;
import com.marmik.brokerhub.broker.dto.PositionItem;
import com.marmik.brokerhub.dto.AggregatedHolding;
import com.marmik.brokerhub.model.AccountMember;
import com.marmik.brokerhub.model.BrokerCredential;
import com.marmik.brokerhub.model.User;
import com.marmik.brokerhub.repository.AccountMemberRepository;
import com.marmik.brokerhub.repository.BrokerCredentialRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for AccountPortfolioService.
 *
 * Covers:
 * - Empty account behavior for aggregation APIs.
 * - Admin visibility override across member portfolios.
 * - Member privacy filtering for DETAILED/SUMMARY/PRIVATE rules.
 * - Position aggregation correctness (quantity, weighted average, pnl).
 *
 * Ensures that portfolio visibility and aggregation business constraints are
 * not broken.
 */
@ExtendWith(MockitoExtension.class)
class AccountPortfolioServiceTest {

        @Mock
        private AccountMemberRepository memberRepo;
        @Mock
        private BrokerCredentialRepository credentialRepo;
        @Mock
        private BrokerCredentialService credentialService;
        @Mock
        private BrokerHoldingsCacheService holdingsCacheService;
        @Mock
        private BrokerClient brokerClient;

        private AccountPortfolioService service;

        @BeforeEach
        void setUp() {
                service = new AccountPortfolioService(memberRepo, credentialRepo, credentialService,
                                holdingsCacheService,
                                List.of(brokerClient));

                lenient().when(holdingsCacheService.getCachedHoldings(anyString(), any())).thenReturn(Optional.empty());
        }

        @Test
        void shouldReturnEmptyResultWhenNoMembers() {
                UUID accountId = UUID.randomUUID();
                UUID callerId = UUID.randomUUID();
                when(memberRepo.findByAccountId(accountId)).thenReturn(List.of());

                Map<String, Object> out = service.aggregateHoldingsForAccount(accountId, callerId);

                assertEquals(List.of(), out.get("full"));
                assertEquals(List.of(), out.get("partial"));
        }

        @Test
        void shouldReturnAllFullDataForAdminCaller() throws Exception {
                UUID accountId = UUID.randomUUID();
                UUID adminUserId = UUID.randomUUID();
                UUID memberUserId = UUID.randomUUID();

                AccountMember admin = member(UUID.randomUUID(), accountId, adminUserId, "ADMIN",
                                "{\"privacy\":\"PRIVATE\"}");
                AccountMember m2 = member(UUID.randomUUID(), accountId, memberUserId, "MEMBER",
                                "{\"privacy\":\"PRIVATE\"}");
                when(memberRepo.findByAccountId(accountId)).thenReturn(List.of(admin, m2));

                BrokerCredential c1 = cred(admin.getId(), "DHAN");
                BrokerCredential c2 = cred(m2.getId(), "DHAN");
                when(credentialRepo.findByAccountMemberId(admin.getId())).thenReturn(List.of(c1));
                when(credentialRepo.findByAccountMemberId(m2.getId())).thenReturn(List.of(c2));

                when(credentialService.decryptCredentialToken(any(), eq(c1.getCredentialId())))
                                .thenReturn("tok-admin".getBytes(StandardCharsets.UTF_8));
                when(credentialService.decryptCredentialToken(any(), eq(c2.getCredentialId())))
                                .thenReturn("tok-member".getBytes(StandardCharsets.UTF_8));
                when(brokerClient.getBrokerType()).thenReturn("DHAN");
                when(brokerClient.getHoldings(eq("tok-admin"))).thenReturn(List.of(
                                HoldingItem.builder().exchange("NSE").tradingSymbol("INFY").quantity(10)
                                                .averagePrice(100).pnl(50)
                                                .build()));
                when(brokerClient.getHoldings(eq("tok-member"))).thenReturn(List.of(
                                HoldingItem.builder().exchange("NSE").tradingSymbol("INFY").quantity(10)
                                                .averagePrice(100).pnl(50)
                                                .build()));

                Map<String, Object> out = service.aggregateHoldingsForAccount(accountId, adminUserId);

                List<?> full = (List<?>) out.get("full");
                List<?> partial = (List<?>) out.get("partial");
                assertFalse(full.isEmpty());
                assertTrue(partial.isEmpty());
        }

        @Test
        void shouldApplyPrivacyRulesForMemberCaller() throws Exception {
                UUID accountId = UUID.randomUUID();
                UUID callerId = UUID.randomUUID();

                AccountMember caller = member(UUID.randomUUID(), accountId, callerId, "MEMBER",
                                "{\"privacy\":\"PRIVATE\"}");
                AccountMember detailed = member(UUID.randomUUID(), accountId, UUID.randomUUID(), "MEMBER",
                                "{\"privacy\":\"DETAILED\"}");
                AccountMember summary = member(UUID.randomUUID(), accountId, UUID.randomUUID(), "MEMBER",
                                "{\"privacy\":\"SUMMARY\"}");
                AccountMember priv = member(UUID.randomUUID(), accountId, UUID.randomUUID(), "MEMBER",
                                "{\"privacy\":\"PRIVATE\"}");
                when(memberRepo.findByAccountId(accountId)).thenReturn(List.of(caller, detailed, summary, priv));

                BrokerCredential c1 = cred(caller.getId(), "DHAN");
                BrokerCredential c2 = cred(detailed.getId(), "DHAN");
                BrokerCredential c3 = cred(summary.getId(), "DHAN");
                BrokerCredential c4 = cred(priv.getId(), "DHAN");
                when(credentialRepo.findByAccountMemberId(caller.getId())).thenReturn(List.of(c1));
                when(credentialRepo.findByAccountMemberId(detailed.getId())).thenReturn(List.of(c2));
                when(credentialRepo.findByAccountMemberId(summary.getId())).thenReturn(List.of(c3));
                when(credentialRepo.findByAccountMemberId(priv.getId())).thenReturn(List.of(c4));

                when(credentialService.decryptCredentialToken(any(), eq(c1.getCredentialId())))
                                .thenReturn("tok-caller".getBytes(StandardCharsets.UTF_8));
                when(credentialService.decryptCredentialToken(any(), eq(c2.getCredentialId())))
                                .thenReturn("tok-detail".getBytes(StandardCharsets.UTF_8));
                when(credentialService.decryptCredentialToken(any(), eq(c3.getCredentialId())))
                                .thenReturn("tok-summary".getBytes(StandardCharsets.UTF_8));
                when(credentialService.decryptCredentialToken(any(), eq(c4.getCredentialId())))
                                .thenReturn("tok-private".getBytes(StandardCharsets.UTF_8));
                when(brokerClient.getBrokerType()).thenReturn("DHAN");
                when(brokerClient.getHoldings(eq("tok-caller"))).thenReturn(
                                List.of(HoldingItem.builder().exchange("NSE").tradingSymbol("CALLER").quantity(1)
                                                .averagePrice(100).pnl(5).build()));
                when(brokerClient.getHoldings(eq("tok-detail"))).thenReturn(
                                List.of(HoldingItem.builder().exchange("NSE").tradingSymbol("DETAIL").quantity(1)
                                                .averagePrice(100).pnl(5).build()));
                when(brokerClient.getHoldings(eq("tok-summary"))).thenReturn(
                                List.of(HoldingItem.builder().exchange("NSE").tradingSymbol("SUMMARY1").quantity(1)
                                                .averagePrice(100).pnl(5).build()));
                when(brokerClient.getHoldings(eq("tok-private"))).thenReturn(
                                List.of(HoldingItem.builder().exchange("NSE").tradingSymbol("PRIVATE1").quantity(1)
                                                .averagePrice(100).pnl(5).build()));

                Map<String, Object> out = service.aggregateHoldingsForAccount(accountId, callerId);

                @SuppressWarnings("unchecked")
                List<AggregatedHolding> full = (List<AggregatedHolding>) out.get("full");
                @SuppressWarnings("unchecked")
                Set<String> partial = (Set<String>) out.get("partial");

                Set<String> fullSymbols = new HashSet<>(
                                full.stream().map(AggregatedHolding::getTradingSymbol).toList());
                assertTrue(fullSymbols.contains("CALLER"));
                assertTrue(fullSymbols.contains("DETAIL"));
                assertFalse(fullSymbols.contains("PRIVATE1"));
                assertTrue(partial.contains("SUMMARY1"));
        }

        @Test
        void shouldAggregatePositionsWithCorrectGrouping() throws Exception {
                UUID accountId = UUID.randomUUID();
                UUID callerId = UUID.randomUUID();
                AccountMember caller = member(UUID.randomUUID(), accountId, callerId, "ADMIN",
                                "{\"privacy\":\"DETAILED\"}");
                when(memberRepo.findByAccountId(accountId)).thenReturn(List.of(caller));

                BrokerCredential c = cred(caller.getId(), "DHAN");
                when(credentialRepo.findByAccountMemberId(caller.getId())).thenReturn(List.of(c));
                when(credentialService.decryptCredentialToken(any(), eq(c.getCredentialId())))
                                .thenReturn("tok-pos".getBytes(StandardCharsets.UTF_8));
                when(brokerClient.getBrokerType()).thenReturn("DHAN");
                when(brokerClient.getPositions(eq("tok-pos"))).thenReturn(List.of(
                                PositionItem.builder().exchange("NSE").tradingSymbol("INFY").quantity(10)
                                                .averagePrice(100).totalPnl(20)
                                                .build(),
                                PositionItem.builder().exchange("NSE").tradingSymbol("INFY").quantity(20)
                                                .averagePrice(130).totalPnl(30)
                                                .build()));

                Map<String, Object> out = service.aggregatePositionsForAccount(accountId, callerId);
                @SuppressWarnings("unchecked")
                List<com.marmik.brokerhub.dto.AggregatedPosition> full = (List<com.marmik.brokerhub.dto.AggregatedPosition>) out
                                .get("full");

                assertEquals(1, full.size());
                assertEquals(30, full.get(0).getQuantity());
                assertEquals(50.0, full.get(0).getPnl());
                assertEquals((10 * 100 + 20 * 130) / 30.0, full.get(0).getAveragePrice());
        }

        private AccountMember member(UUID memberId, UUID accountId, UUID userId, String role, String rules) {
                User u = new User();
                u.setId(userId);
                AccountMember m = new AccountMember();
                m.setId(memberId);
                m.setAccountId(accountId);
                m.setUser(u);
                m.setRole(role);
                m.setRules(rules);
                return m;
        }

        private BrokerCredential cred(UUID memberId, String broker) {
                BrokerCredential c = new BrokerCredential();
                c.setCredentialId(UUID.randomUUID());
                c.setAccountMemberId(memberId);
                c.setBroker(broker);
                return c;
        }
}
