package com.marmik.brokerhub.service;

import com.marmik.brokerhub.model.AccountMember;
import com.marmik.brokerhub.model.BrokerCredential;
import com.marmik.brokerhub.model.User;
import com.marmik.brokerhub.repository.AccountMemberRepository;
import com.marmik.brokerhub.repository.BrokerCredentialRepository;
import com.marmik.brokerhub.security.EnvelopeEncryptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for BrokerCredentialService.
 *
 * Covers:
 * - Credential storage authorization for owner paths.
 * - Access-denied behavior for non-owner/non-admin callers.
 * - Token decryption flow for authorized callers.
 *
 * Ensures that broker credential access-control and decryption constraints are
 * not broken.
 */
@ExtendWith(MockitoExtension.class)
class BrokerCredentialServiceTest {

        @Mock
        private BrokerCredentialRepository repo;
        @Mock
        private AccountMemberRepository accountMemberRepo;
        @Mock
        private EnvelopeEncryptionService envelope;

        @InjectMocks
        private BrokerCredentialService service;

        @Test
        void shouldStoreCredentialWhenCallerIsOwner() throws Exception {
                UUID callerId = UUID.randomUUID();
                UUID accountId = UUID.randomUUID();
                UUID memberId = UUID.randomUUID();

                AccountMember target = membership(memberId, accountId, UUID.randomUUID(), "MEMBER");
                AccountMember caller = membership(memberId, accountId, callerId, "MEMBER");

                when(accountMemberRepo.findById(memberId)).thenReturn(Optional.of(target));
                when(accountMemberRepo.findByUserIdAndAccountId(callerId, accountId)).thenReturn(Optional.of(caller));
                when(envelope.generateDek()).thenReturn(new byte[] { 1, 2, 3 });
                when(envelope.encryptWithDek(any(), any()))
                                .thenReturn(new EnvelopeEncryptionService.EncryptionResult(new byte[] { 9 },
                                                new byte[] { 8 }));
                when(envelope.wrapDek(any())).thenReturn(new byte[] { 7 });
                when(repo.save(any(BrokerCredential.class))).thenAnswer(inv -> inv.getArgument(0));

                BrokerCredential saved = service.storeCredential(callerId, memberId, "DHAN", "nick",
                                "token".getBytes(StandardCharsets.UTF_8));

                assertEquals("DHAN", saved.getBroker());
                assertEquals("local-master-v1", saved.getTokenKeyId());
        }

        @Test
        void shouldThrowWhenCallerIsNeitherOwnerNorAdmin() {
                UUID callerId = UUID.randomUUID();
                UUID accountId = UUID.randomUUID();
                UUID targetMemberId = UUID.randomUUID();
                UUID callerMemberId = UUID.randomUUID();

                when(accountMemberRepo.findById(targetMemberId))
                                .thenReturn(Optional.of(
                                                membership(targetMemberId, accountId, UUID.randomUUID(), "MEMBER")));
                when(accountMemberRepo.findByUserIdAndAccountId(callerId, accountId))
                                .thenReturn(Optional.of(membership(callerMemberId, accountId, callerId, "MEMBER")));

                assertThrows(AccessDeniedException.class,
                                () -> service.storeCredential(callerId, targetMemberId, "DHAN", "nick",
                                                new byte[] { 1 }));
        }

        @Test
        void shouldDecryptTokenWhenAuthorized() throws Exception {
                UUID callerId = UUID.randomUUID();
                UUID accountId = UUID.randomUUID();
                UUID targetUser = UUID.randomUUID();
                UUID memberId = UUID.randomUUID();
                UUID credentialId = UUID.randomUUID();

                BrokerCredential cred = new BrokerCredential();
                cred.setCredentialId(credentialId);
                cred.setAccountMemberId(memberId);
                cred.setTokenEncryptedDek(new byte[] { 1 });
                cred.setTokenIv(new byte[] { 2 });
                cred.setTokenCipher(new byte[] { 3 });

                when(repo.findById(credentialId)).thenReturn(Optional.of(cred));
                when(accountMemberRepo.findById(memberId))
                                .thenReturn(Optional.of(membership(memberId, accountId, targetUser, "MEMBER")));
                when(accountMemberRepo.findByUserIdAndAccountId(callerId, accountId))
                                .thenReturn(Optional.of(membership(UUID.randomUUID(), accountId, callerId, "ADMIN")));
                when(envelope.unwrapDek(any())).thenReturn(new byte[] { 10, 11 });
                when(envelope.decryptWithDek(any(), any(), any()))
                                .thenReturn("plain-token".getBytes(StandardCharsets.UTF_8));

                byte[] out = service.decryptCredentialToken(callerId, credentialId);
                assertEquals("plain-token", new String(out, StandardCharsets.UTF_8));
        }

        private AccountMember membership(UUID memberId, UUID accountId, UUID userId, String role) {
                User user = new User();
                user.setId(userId);
                AccountMember m = new AccountMember();
                m.setId(memberId);
                m.setAccountId(accountId);
                m.setUser(user);
                m.setRole(role);
                return m;
        }
}
