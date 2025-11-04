package com.marmik.brokerhub.service;

import com.marmik.brokerhub.model.AccountMember;
import com.marmik.brokerhub.model.User;
import com.marmik.brokerhub.repository.AccountMemberRepository;
import com.marmik.brokerhub.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class UserService {

    private final UserRepository userRepo;
    private final AccountMemberRepository memberRepo;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepo,
            AccountMemberRepository memberRepo,
            PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.memberRepo = memberRepo;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Returns user profile + all memberships.
     */
    public Optional<Map<String, Object>> getUserProfile(UUID userId) {
        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty())
            return Optional.empty();

        User user = userOpt.get();
        List<AccountMember> memberships = memberRepo.findByUserId(userId);

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("loginId", user.getLoginId());
        profile.put("email", user.getEmail());
        profile.put("name", user.getMemberName());
        profile.put("accounts", memberships.stream().map(m -> Map.of(
                "accountId", m.getAccountId(),
                "role", m.getRole())).toList());

        return Optional.of(profile);
    }

    /**
     * New: Register a new user (no account creation).
     */
    @Transactional
    public User registerUser(String loginId, String memberName, String email, String rawPassword) {
        if (loginId == null || loginId.isBlank())
            throw new IllegalArgumentException("loginId is required");
        if (memberName == null || memberName.isBlank())
            throw new IllegalArgumentException("memberName is required");
        if (rawPassword == null || rawPassword.length() < 6)
            throw new IllegalArgumentException("Password must be at least 6 characters");

        // Validate duplicates
        if (email != null && userRepo.findByEmail(email).isPresent())
            throw new IllegalArgumentException("Email already in use");
        if (userRepo.findByLoginId(loginId).isPresent())
            throw new IllegalArgumentException("Login ID already exists");

        User user = new User();
        user.setLoginId(loginId);
        user.setEmail(email != null ? email.toLowerCase() : null);
        user.setMemberName(memberName);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));

        return userRepo.save(user);
    }
}
