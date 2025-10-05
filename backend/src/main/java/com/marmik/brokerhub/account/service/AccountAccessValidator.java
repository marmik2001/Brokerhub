package com.marmik.brokerhub.account.service;

import com.marmik.brokerhub.account.model.AccountMember;
import com.marmik.brokerhub.account.repository.AccountMemberRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Utility service for verifying user membership and roles within accounts.
 * Use this instead of repeating membership checks in every controller.
 */
@Service
public class AccountAccessValidator {

    private final AccountMemberRepository memberRepo;

    public AccountAccessValidator(AccountMemberRepository memberRepo) {
        this.memberRepo = memberRepo;
    }

    /**
     * Ensures the given user belongs to the given account.
     * Throws AccessDeniedException if not.
     */
    public AccountMember requireMembership(UUID userId, UUID accountId) {
        return memberRepo.findByUserIdAndAccountId(userId, accountId)
                .orElseThrow(
                        () -> new AccessDeniedException("User " + userId + " is not a member of account " + accountId));
    }

    /**
     * Ensures user has at least the required role for this account.
     * Roles are case-insensitive (ADMIN/MEMBER).
     */
    public void requireRole(UUID userId, UUID accountId, String requiredRole) {
        AccountMember member = requireMembership(userId, accountId);
        if (!member.getRole().equalsIgnoreCase(requiredRole)) {
            throw new AccessDeniedException("Requires role " + requiredRole);
        }
    }

    /**
     * Returns true if user is a member of the account.
     */
    public boolean isMember(UUID userId, UUID accountId) {
        return memberRepo.findByUserIdAndAccountId(userId, accountId).isPresent();
    }
}
