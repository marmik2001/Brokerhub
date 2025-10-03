package com.marmik.brokerhub.account.service;

import com.marmik.brokerhub.account.model.AccountMember;
import com.marmik.brokerhub.account.repository.AccountMemberRepository;

import lombok.AllArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

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

    public boolean changePasswordById(String memberId, String oldPassword, String newPassword) {
        Optional<AccountMember> maybe = memberRepo.findById(UUID.fromString(memberId));
        if (maybe.isEmpty()) {
            return false;
        }
        AccountMember m = maybe.get();
        if (!passwordEncoder.matches(oldPassword, m.getPasswordHash())) {
            return false;
        }
        m.setPasswordHash(passwordEncoder.encode(newPassword));
        memberRepo.save(m);
        return true;
    }

}
