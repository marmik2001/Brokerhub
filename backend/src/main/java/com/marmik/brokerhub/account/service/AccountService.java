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

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepo;
    private final AccountMemberRepository memberRepo;
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    private static final Pattern EMAIL_REGEX = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    /**
     * Create a new account + global user + membership (ADMIN).
     */
    @Transactional
    public AccountMember createAccountWithAdmin(
            String accountName,
            String accountDesc,
            String loginId,
            String memberName,
            String email,
            String rawPassword) {
        // 1. Validate email
        if (email != null && !email.isBlank()) {
            if (!EMAIL_REGEX.matcher(email).matches()) {
                throw new IllegalArgumentException("Invalid email format");
            }
            email = email.toLowerCase();
        }

        // 2. Check if user already exists
        Optional<User> existingUser = userRepo.findByLoginIdOrEmailIgnoreCase(email != null ? email : loginId);

        if (existingUser.isPresent()) {
            throw new IllegalArgumentException("User already exists with same login ID or email");
        }

        // 3. Create new user
        User user = new User();
        user.setLoginId(loginId);
        user.setEmail(email);
        user.setMemberName(memberName);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        userRepo.save(user);

        // 4. Create account
        Account account = new Account();
        account.setName(accountName);
        account.setDescription(accountDesc);
        accountRepo.save(account);

        // 5. Create membership (ADMIN)
        AccountMember admin = new AccountMember();
        admin.setAccountId(account.getId());
        admin.setUser(user);
        admin.setRole("ADMIN");
        admin.setRules("{}");
        memberRepo.save(admin);

        return admin;
    }

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
