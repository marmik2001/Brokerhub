package com.marmik.brokerhub.service;

import com.marmik.brokerhub.model.Account;
import com.marmik.brokerhub.model.AccountMember;
import com.marmik.brokerhub.model.User;
import com.marmik.brokerhub.repository.AccountMemberRepository;
import com.marmik.brokerhub.repository.AccountRepository;
import com.marmik.brokerhub.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for AccountService.
 *
 * Covers:
 * - Member onboarding constraints (existing user and duplicate prevention).
 * - Membership safety rules (no self-removal, no self-demotion).
 * - Role and privacy validation with access-control checks.
 *
 * Ensures that account membership and role/visibility business rules are not
 * broken.
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepo;
    @Mock
    private AccountMemberRepository memberRepo;
    @Mock
    private UserRepository userRepo;

    @InjectMocks
    private AccountService accountService;

    @Test
    void shouldThrowWhenAddMemberUserDoesNotExist() {
        when(userRepo.findByLoginIdOrEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> accountService.addMember(UUID.randomUUID().toString(), null, "missing@example.com"));

        assertTrue(ex.getMessage().contains("User does not exist"));
    }

    @Test
    void shouldThrowWhenAddMemberAlreadyExists() {
        UUID accountId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);

        when(userRepo.findByLoginIdOrEmailIgnoreCase("john")).thenReturn(Optional.of(user));
        when(memberRepo.findByUserIdAndAccountId(userId, accountId)).thenReturn(Optional.of(new AccountMember()));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> accountService.addMember(accountId.toString(), "john", null));

        assertEquals("User is already a member of this account", ex.getMessage());
    }

    @Test
    void shouldThrowWhenRemovingSelfFromAccount() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        AccountMember actor = new AccountMember();
        actor.setId(memberId);

        when(memberRepo.findByUserIdAndAccountId(userId, accountId)).thenReturn(Optional.of(actor));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> accountService.removeMember(userId, accountId, memberId));

        assertEquals("Cannot remove yourself from the account", ex.getMessage());
    }

    @Test
    void shouldThrowWhenUpdatingRoleWithInvalidRole() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> accountService.updateMemberRole(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        "OWNER"));

        assertEquals("Invalid role", ex.getMessage());
    }

    @Test
    void shouldThrowWhenAdminDemotesSelf() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        AccountMember actor = new AccountMember();
        actor.setId(memberId);

        when(memberRepo.findByUserIdAndAccountId(userId, accountId)).thenReturn(Optional.of(actor));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> accountService.updateMemberRole(userId, accountId, memberId, "MEMBER"));

        assertEquals("Cannot demote yourself", ex.getMessage());
    }

    @Test
    void shouldThrowWhenUpdatingPrivacyForAnotherUser() {
        UUID actorUserId = UUID.randomUUID();
        UUID targetUserId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        User targetUser = new User();
        targetUser.setId(targetUserId);
        AccountMember targetMembership = new AccountMember();
        targetMembership.setUser(targetUser);

        when(memberRepo.findByIdAndAccountId(memberId, accountId)).thenReturn(Optional.of(targetMembership));

        assertThrows(AccessDeniedException.class,
                () -> accountService.updateOwnMemberPrivacy(actorUserId, accountId, memberId, "SUMMARY"));
    }

    @Test
    void shouldPersistUppercasePrivacyWhenValid() {
        UUID userId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        AccountMember membership = new AccountMember();
        membership.setUser(user);

        when(memberRepo.findByIdAndAccountId(memberId, accountId)).thenReturn(Optional.of(membership));
        when(memberRepo.save(any(AccountMember.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountMember out = accountService.updateOwnMemberPrivacy(userId, accountId, memberId, "summary");

        assertEquals(Map.of("privacy", "SUMMARY"), out.getRules());
    }

    @Test
    void shouldCreateAccountAndAdminMembership() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(accountRepo.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));
        when(memberRepo.save(any(AccountMember.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountMember out = accountService.createAccountForUser(userId, "Main", "desc");

        assertEquals("ADMIN", out.getRole());
        assertEquals(user, out.getUser());
    }
}
