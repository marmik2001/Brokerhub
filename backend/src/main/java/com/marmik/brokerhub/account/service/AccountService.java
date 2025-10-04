package com.marmik.brokerhub.account.service;

import com.marmik.brokerhub.account.model.Account;
import com.marmik.brokerhub.account.model.AccountMember;
import com.marmik.brokerhub.account.repository.AccountMemberRepository;
import com.marmik.brokerhub.account.repository.AccountRepository;

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
    private final PasswordEncoder passwordEncoder;

    private static final Pattern EMAIL_REGEX = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    @Transactional
    public AccountMember createAccountWithAdmin(String accountName, String accountDesc, String loginId,
            String memberName, String email, String rawPassword) {
        Optional<AccountMember> existing = memberRepo.findByLoginId(loginId);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("LoginId is already taken, please choose another!");
        }

        existing = memberRepo.findByLoginIdOrEmailIgnoreCase(email);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Email already in use!");
        }

        if (email != null && !email.isBlank()) {
            if (!EMAIL_REGEX.matcher(email).matches()) {
                throw new IllegalArgumentException("Invalid email format");
            }
            email = email.toLowerCase();
        }

        Account account = new Account();
        account.setName(accountName);
        account.setDescription(accountDesc);
        accountRepo.save(account);

        AccountMember admin = new AccountMember();
        admin.setLoginId(loginId);
        admin.setEmail(email);
        admin.setAccountId(account.getId());
        admin.setMemberName(memberName);
        admin.setPasswordHash(passwordEncoder.encode(rawPassword));
        admin.setRole("ADMIN");
        admin.setRules("{}");

        memberRepo.save(admin);
        return admin;
    }

    @Transactional
    public AccountMember addMember(String accountId,
            String loginId,
            String memberName,
            String rawPassword,
            String role) {
        if (memberRepo.findByLoginId(loginId).isPresent()) {
            throw new IllegalArgumentException("loginId already exists");
        }
        AccountMember member = new AccountMember();
        member.setLoginId(loginId);
        member.setAccountId(UUID.fromString(accountId));
        member.setMemberName(memberName);
        member.setPasswordHash(passwordEncoder.encode(rawPassword));
        member.setRole(role);
        member.setRules("{}");
        memberRepo.save(member);
        return member;
    }

}
