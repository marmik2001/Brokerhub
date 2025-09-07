package com.marmik.brokerhub.account.service;

import com.marmik.brokerhub.account.model.AccountMember;
import com.marmik.brokerhub.account.repository.AccountMemberRepository;

import lombok.AllArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@AllArgsConstructor
@Service
public class AuthService {
    private final AccountMemberRepository memberRepo;
    private final PasswordEncoder passwordEncoder;

    public AccountMember authenticate(String loginId, String rawPassword) {
        Optional<AccountMember> maybe = memberRepo.findByLoginId(loginId);
        if (maybe.isEmpty())
            return null;
        AccountMember m = maybe.get();
        if (passwordEncoder.matches(rawPassword, m.getPasswordHash()))
            return m;
        return null;
    }
}
