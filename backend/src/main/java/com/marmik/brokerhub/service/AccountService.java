package com.marmik.brokerhub.service;

import com.marmik.brokerhub.model.Account;
import com.marmik.brokerhub.model.AccountMember;
import com.marmik.brokerhub.model.User;
import com.marmik.brokerhub.repository.AccountMemberRepository;
import com.marmik.brokerhub.repository.AccountRepository;
import com.marmik.brokerhub.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepo;
    private final AccountMemberRepository memberRepo;
    private final UserRepository userRepo;

    /**
     * Add member to an existing account.
     * If user not found => IllegalArgumentException("User does not exist...")
     * If membership already exists => IllegalArgumentException("Already a member")
     */
    @Transactional
    public AccountMember addMember(
            String accountId,
            String loginId,
            String email) {

        // Find user by loginId or email (case insensitive)
        Optional<User> userOpt = userRepo.findByLoginIdOrEmailIgnoreCase(email != null ? email : loginId);

        // If user not found -> throw exception
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User does not exist. Please ask them to register first.");
        }

        User user = userOpt.get();
        UUID accUuid = UUID.fromString(accountId);

        // Check existing membership
        if (memberRepo.findByUserIdAndAccountId(user.getId(), accUuid).isPresent()) {
            throw new IllegalArgumentException("User is already a member of this account");
        }

        // Create AccountMember entry
        AccountMember member = new AccountMember();
        member.setAccountId(accUuid);
        member.setUser(user);
        member.setRole("MEMBER");
        member.setRules("{}");

        return memberRepo.save(member);
    }

    /**
     * Create a new account and link an existing user as ADMIN.
     */
    @Transactional
    public AccountMember createAccountForUser(UUID userId, String accountName, String accountDesc) {
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
        admin.setRules("{}");
        memberRepo.save(admin);

        return admin;
    }

    /**
     * List all AccountMember entries for the account.
     */
    @Transactional(readOnly = true)
    public List<AccountMember> listMembers(UUID accountId) {
        return memberRepo.findByAccountId(accountId);
    }

    /**
     * Remove a member from an account.
     * Throws IllegalArgumentException if member not found for the account.
     */
    @Transactional
    public void removeMember(UUID accountId, UUID memberId) {
        AccountMember member = memberRepo.findByIdAndAccountId(memberId, accountId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found for this account"));

        memberRepo.delete(member);
    }

    /**
     * Update role for a member in the account.
     * Throws IllegalArgumentException if member not found or invalid role.
     */
    @Transactional
    public AccountMember updateMemberRole(UUID accountId, UUID memberId, String role) {
        if (role == null || !(role.equalsIgnoreCase("ADMIN") || role.equalsIgnoreCase("MEMBER"))) {
            throw new IllegalArgumentException("Invalid role");
        }

        AccountMember member = memberRepo.findByIdAndAccountId(memberId, accountId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found for this account"));

        member.setRole(role.toUpperCase());
        return memberRepo.save(member);
    }
}
