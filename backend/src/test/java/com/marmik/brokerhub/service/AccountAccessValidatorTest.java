package com.marmik.brokerhub.service;

import com.marmik.brokerhub.model.AccountMember;
import com.marmik.brokerhub.repository.AccountMemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests for AccountAccessValidator.
 *
 * Covers:
 * - Membership enforcement for account-scoped access.
 * - Role enforcement with case-insensitive matching.
 * - AccessDeniedException behavior for unauthorized access.
 *
 * Ensures that account membership and role guardrails are not broken.
 */
@ExtendWith(MockitoExtension.class)
class AccountAccessValidatorTest {

    @Mock
    private AccountMemberRepository memberRepo;

    @InjectMocks
    private AccountAccessValidator validator;

    @Test
    void shouldReturnMembershipWhenUserBelongsToAccount() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        AccountMember membership = new AccountMember();

        when(memberRepo.findByUserIdAndAccountId(userId, accountId)).thenReturn(Optional.of(membership));

        AccountMember out = validator.requireMembership(userId, accountId);

        assertSame(membership, out);
    }

    @Test
    void shouldThrowWhenUserNotMember() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        when(memberRepo.findByUserIdAndAccountId(userId, accountId)).thenReturn(Optional.empty());

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> validator.requireMembership(userId, accountId));

        assertTrue(ex.getMessage().contains("is not a member of account"));
    }

    @Test
    void shouldAllowWhenRoleMatchesIgnoringCase() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        AccountMember membership = new AccountMember();
        membership.setRole("admin");

        when(memberRepo.findByUserIdAndAccountId(userId, accountId)).thenReturn(Optional.of(membership));

        assertDoesNotThrow(() -> validator.requireRole(userId, accountId, "ADMIN"));
    }

    @Test
    void shouldThrowWhenRoleDoesNotMatch() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        AccountMember membership = new AccountMember();
        membership.setRole("MEMBER");

        when(memberRepo.findByUserIdAndAccountId(userId, accountId)).thenReturn(Optional.of(membership));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> validator.requireRole(userId, accountId, "ADMIN"));

        assertEquals("Requires role ADMIN", ex.getMessage());
    }
}
