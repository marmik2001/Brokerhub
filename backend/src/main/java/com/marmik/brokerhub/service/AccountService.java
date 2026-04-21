package com.marmik.brokerhub.service;

import com.marmik.brokerhub.model.Account;
import com.marmik.brokerhub.model.AccountMember;
import com.marmik.brokerhub.model.User;
import com.marmik.brokerhub.repository.AccountMemberRepository;
import com.marmik.brokerhub.repository.AccountRepository;
import com.marmik.brokerhub.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing user accounts, memberships, and roles.
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepo;
    private final AccountMemberRepository memberRepo;
    private final UserRepository userRepo;

    /**
     * Add member to an existing account.
     */
    @Transactional
    public AccountMember addMember(
            String accountId,
            String loginId,
            String email) {

        Optional<User> userOpt = userRepo.findByLoginIdOrEmailIgnoreCase(email != null ? email : loginId);

        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException(
                    "User does not exist. Please ask them to register first.");
        }

        User user = userOpt.get();
        UUID accUuid = UUID.fromString(accountId);

        if (memberRepo.findByUserIdAndAccountId(user.getId(), accUuid).isPresent()) {
            throw new IllegalArgumentException("User is already a member of this account");
        }

        AccountMember member = new AccountMember();
        member.setAccountId(accUuid);
        member.setUser(user);
        member.setRole("MEMBER");
        member.setRules(new HashMap<>(Map.of("privacy", "DETAILED")));

        return memberRepo.save(member);
    }

    /**
     * Create a new account and link an existing user as ADMIN.
     */
    @Transactional
    public AccountMember createAccountForUser(
            UUID userId,
            String accountName,
            String accountDesc) {

        if (accountName == null || accountName.isBlank()) {
            throw new IllegalArgumentException("Account name is required");
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Account account = new Account();
        account.setName(accountName);
        account.setDescription(accountDesc);
        accountRepo.save(account);

        AccountMember admin = new AccountMember();
        admin.setAccountId(account.getId());
        admin.setUser(user);
        admin.setRole("ADMIN");
        admin.setRules(new HashMap<>(Map.of("privacy", "DETAILED")));

        return memberRepo.save(admin);
    }

    /**
     * List all members for an account.
     */
    @Transactional(readOnly = true)
    public List<AccountMember> listMembers(UUID accountId) {
        return memberRepo.findByAccountId(accountId);
    }

    /**
     * Aggregated account view for "My Accounts" API.
     * Controller should call ONLY this.
     */
    @Transactional(readOnly = true)
    public List<HashMap<String, Object>> listUserAccountViews(UUID userId) {
        return memberRepo.findByUserId(userId).stream().map(m -> {
            Optional<Account> accOpt = accountRepo.findById(m.getAccountId());

            HashMap<String, Object> map = new HashMap<>();
            map.put("accountId", m.getAccountId());
            map.put("name", accOpt.map(Account::getName).orElse(null));
            map.put("description", accOpt.map(Account::getDescription).orElse(null));
            map.put("role", m.getRole());
            map.put("accountMemberId", m.getId());
            map.put("rules", m.getRules());
            return map;
        }).toList();
    }

    /**
     * Remove a member from an account.
     * Prevents removing yourself.
     */
    @Transactional
    public void removeMember(
            UUID actorUserId,
            UUID accountId,
            UUID memberId) {

        AccountMember actor = memberRepo
                .findByUserIdAndAccountId(actorUserId, accountId)
                .orElseThrow(() -> new AccessDeniedException("Not a member of this account"));

        if (actor.getId().equals(memberId)) {
            throw new IllegalStateException("Cannot remove yourself from the account");
        }

        AccountMember target = memberRepo
                .findByIdAndAccountId(memberId, accountId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Member not found for this account"));

        memberRepo.delete(target);
    }

    /**
     * Update role for a member in the account.
     * Prevents self-demotion.
     */
    @Transactional
    public AccountMember updateMemberRole(
            UUID actorUserId,
            UUID accountId,
            UUID memberId,
            String role) {

        if (role == null ||
                !(role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("MEMBER"))) {
            throw new IllegalArgumentException("Invalid role");
        }

        AccountMember actor = memberRepo
                .findByUserIdAndAccountId(actorUserId, accountId)
                .orElseThrow(() -> new AccessDeniedException("Not a member of this account"));

        if (actor.getId().equals(memberId)
                && !"ADMIN".equalsIgnoreCase(role)) {
            throw new IllegalStateException("Cannot demote yourself");
        }

        AccountMember target = memberRepo
                .findByIdAndAccountId(memberId, accountId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Member not found for this account"));

        target.setRole(role.toUpperCase());
        return memberRepo.save(target);
    }

    /**
     * Update privacy for your own membership only.
     */
    @Transactional
    public AccountMember updateOwnMemberPrivacy(
            UUID actorUserId,
            UUID accountId,
            UUID memberId,
            String privacy) {

        if (privacy == null) {
            throw new IllegalArgumentException("privacy is required");
        }

        String up = privacy.toUpperCase();
        if (!(up.equals("DETAILED") || up.equals("SUMMARY") || up.equals("PRIVATE"))) {
            throw new IllegalArgumentException("Invalid privacy value");
        }

        AccountMember member = memberRepo
                .findByIdAndAccountId(memberId, accountId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Member not found for this account"));

        if (!member.getUser().getId().equals(actorUserId)) {
            throw new AccessDeniedException(
                    "Can only modify your own membership settings");
        }

        member.setRules(new HashMap<>(Map.of("privacy", up)));
        return memberRepo.save(member);
    }
}
