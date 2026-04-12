package com.marmik.brokerhub.controller;

import com.marmik.brokerhub.model.User;
import com.marmik.brokerhub.security.JwtUtil;
import com.marmik.brokerhub.service.AuthService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    public static record LoginRequest(String identifier, String password) {
    }

    public static record ChangePasswordRequest(String oldPassword, String newPassword) {
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {

        User user = authService.authenticate(req.identifier(), req.password());

        String token = jwtUtil.generateUserToken(user.getId().toString());

        return ResponseEntity.ok(Map.of(
                "token", token,
                "user", Map.of(
                        "id", user.getId(),
                        "loginId", user.getLoginId(),
                        "email", user.getEmail(),
                        "name", user.getMemberName()),
                "accounts", authService.getUserAccountSummaries(user.getId())));
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody ChangePasswordRequest req,
            @AuthenticationPrincipal String userId) {
        if (userId == null || userId.isBlank()) {
            throw new AccessDeniedException("Authentication required");
        }

        UUID caller = UUID.fromString(userId);

        authService.changePasswordById(
                caller,
                req.oldPassword(),
                req.newPassword());

        return ResponseEntity.ok(
                Map.of("message", "Password updated successfully"));
    }
}
