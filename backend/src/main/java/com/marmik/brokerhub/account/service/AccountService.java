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

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepo;
    private final AccountMemberRepository memberRepo;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AccountMember createAccountWithAdmin(String accountName, String accountDesc, String loginId,
            String memberName, String rawPassword) {
        Optional<AccountMember> existing = memberRepo.findByLoginId(loginId);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("loginId is already taken, please choose another!");
        }

        Account account = new Account();
        account.setName(accountName);
        account.setDescription(accountDesc);
        accountRepo.save(account);

        AccountMember admin = new AccountMember();
        admin.setLoginId(loginId);
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
