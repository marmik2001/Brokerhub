package com.marmik.brokerhub.service;

import com.marmik.brokerhub.model.User;
import com.marmik.brokerhub.repository.AccountMemberRepository;
import com.marmik.brokerhub.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for AuthService.
 *
 * Covers:
 * - Credential validation for login/email authentication.
 * - Password change flow for valid and invalid old passwords.
 * - Error paths when user records are missing.
 *
 * Ensures that authentication and password-update constraints are not broken.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepo;

    @Mock
    private AccountMemberRepository memberRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldAuthenticateByIdentifierWhenPasswordMatches() {
        User user = new User();
        user.setLoginId("john");
        user.setPasswordHash("hash");

        when(userRepo.findByLoginIdOrEmailIgnoreCase("john")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);

        User out = authService.authenticate("john", "secret");

        assertSame(user, out);
    }

    @Test
    void shouldFailAuthenticationWhenUserMissing() {
        when(userRepo.findByLoginIdOrEmailIgnoreCase("john")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.authenticate("john", "secret"));

        assertEquals("Invalid credentials", ex.getMessage());
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void shouldFailAuthenticationWhenPasswordDoesNotMatch() {
        User user = new User();
        user.setPasswordHash("hash");
        when(userRepo.findByLoginIdOrEmailIgnoreCase("john")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.authenticate("john", "bad"));

        assertEquals("Invalid credentials", ex.getMessage());
    }

    @Test
    void shouldChangePasswordWhenOldPasswordMatches() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setPasswordHash("oldHash");

        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "oldHash")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("newHash");

        authService.changePasswordById(userId, "old", "newPass");

        assertEquals("newHash", user.getPasswordHash());
        verify(userRepo).save(user);
    }

    @Test
    void shouldRejectPasswordChangeWhenOldPasswordIncorrect() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setPasswordHash("oldHash");
        when(userRepo.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "oldHash")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.changePasswordById(userId, "old", "new"));

        assertEquals("Old password is incorrect", ex.getMessage());
        verify(userRepo, never()).save(any());
    }

    @Test
    void shouldThrowWhenChangingPasswordForMissingUser() {
        UUID userId = UUID.randomUUID();
        when(userRepo.findById(userId)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.changePasswordById(userId, "old", "new"));

        assertEquals("User not found", ex.getMessage());
        verify(passwordEncoder, never()).matches(any(), any());
        verify(userRepo, never()).save(any());
    }
}
