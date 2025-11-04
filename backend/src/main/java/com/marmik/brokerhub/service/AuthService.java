package com.marmik.brokerhub.service;

import com.marmik.brokerhub.model.User;
import com.marmik.brokerhub.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    /**
     * Authenticate by loginId or email.
     * Returns the User if valid, else null.
     */
    public User authenticate(String identifier, String rawPassword) {
        Optional<User> maybe = userRepo.findByLoginIdOrEmailIgnoreCase(identifier);
        if (maybe.isEmpty())
            return null;

        User user = maybe.get();
        if (passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            return user;
        }
        return null;
    }

    /**
     * Change password by userId (global, not per-account).
     */
    public boolean changePasswordById(String userId, String oldPassword, String newPassword) {
        Optional<User> maybe = userRepo.findById(UUID.fromString(userId));
        if (maybe.isEmpty())
            return false;

        User user = maybe.get();
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            return false;
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepo.save(user);
        return true;
    }
}
