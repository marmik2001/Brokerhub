package com.marmik.brokerhub.service;

import com.marmik.brokerhub.model.User;
import com.marmik.brokerhub.repository.AccountMemberRepository;
import com.marmik.brokerhub.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for handling user authentication and password management.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final AccountMemberRepository memberRepo;
    private final PasswordEncoder passwordEncoder;

    /**
     * Authenticate by loginId or email.
     * Throws IllegalArgumentException on failure.
     */
    @Transactional(readOnly = true)
    public User authenticate(String identifier, String rawPassword) {

        User user = userRepo.findByLoginIdOrEmailIgnoreCase(identifier)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        return user;
    }

    /**
     * Change password by userId.
     * Throws IllegalArgumentException if old password is incorrect.
     */
    @Transactional
    public void changePasswordById(
            UUID userId,
            String oldPassword,
            String newPassword) {

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Old password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepo.save(user);
    }

    /**
     * Lightweight account summaries for login response.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUserAccountSummaries(UUID userId) {
        return memberRepo.findByUserId(userId).stream()
                .map(m -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("accountId", m.getAccountId());
                    map.put("role", m.getRole());
                    return map;
                })
                .toList();
    }
}
