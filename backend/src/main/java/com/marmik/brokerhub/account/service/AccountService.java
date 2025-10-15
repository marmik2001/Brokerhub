package com.marmik.brokerhub.account.service;

import com.marmik.brokerhub.account.model.Account;
import com.marmik.brokerhub.account.model.AccountMember;
import com.marmik.brokerhub.account.model.User;
import com.marmik.brokerhub.account.repository.AccountMemberRepository;
import com.marmik.brokerhub.account.repository.AccountRepository;
import com.marmik.brokerhub.account.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        // Create AccountMember entry
        AccountMember member = new AccountMember();
        member.setAccountId(UUID.fromString(accountId));
        member.setUser(user);
        member.setRole("MEMBER");
        member.setRules("{}");

        return memberRepo.save(member);
    }

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

}
