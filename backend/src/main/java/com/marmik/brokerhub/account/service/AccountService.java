package com.marmik.brokerhub.account.service;

import com.marmik.brokerhub.account.model.Account;
import com.marmik.brokerhub.account.model.AccountMember;
import com.marmik.brokerhub.account.model.User;
import com.marmik.brokerhub.account.repository.AccountMemberRepository;
import com.marmik.brokerhub.account.repository.AccountRepository;
import com.marmik.brokerhub.account.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepo;
    private final AccountMemberRepository memberRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    /**
     * Add member to an existing account.
     * If user exists, link them. If not, create new user.
     */
    @Transactional
    public AccountMember addMember(
            String accountId,
            String loginId,
            String memberName,
            String email,
            String rawPassword,
            String role) {
        User user = userRepo.findByLoginIdOrEmailIgnoreCase(email != null ? email : loginId)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setLoginId(loginId);
                    newUser.setEmail(email != null ? email.toLowerCase() : null);
                    newUser.setMemberName(memberName);
                    newUser.setPasswordHash(passwordEncoder.encode(rawPassword));
                    return userRepo.save(newUser);
                });

        AccountMember member = new AccountMember();
        member.setAccountId(UUID.fromString(accountId));
        member.setUser(user);
        member.setRole(role);
        member.setRules("{}");
        memberRepo.save(member);

        return member;
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
