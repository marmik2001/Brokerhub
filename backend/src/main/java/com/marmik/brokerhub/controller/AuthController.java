package com.marmik.brokerhub.controller;

import com.marmik.brokerhub.model.AccountMember;
import com.marmik.brokerhub.model.User;
import com.marmik.brokerhub.repository.AccountMemberRepository;
import com.marmik.brokerhub.security.JwtUtil;
import com.marmik.brokerhub.service.AuthService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final AccountMemberRepository memberRepo;

    public AuthController(AuthService authService, JwtUtil jwtUtil, AccountMemberRepository memberRepo) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.memberRepo = memberRepo;
    }

    public static record LoginRequest(String identifier, String password) {
    }

    public static record ChangePasswordRequest(String oldPassword, String newPassword) {
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        User user = authService.authenticate(req.identifier(), req.password());
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }

        // Fetch all memberships for this user (may be empty)
        List<AccountMember> memberships = memberRepo.findByUserId(user.getId());

        // Generate user-scoped token (no accountId inside)
        String token = jwtUtil.generateUserToken(user.getId().toString());

        return ResponseEntity.ok(Map.of(
                "token", token,
                "user", Map.of(
                        "id", user.getId(),
                        "loginId", user.getLoginId(),
                        "email", user.getEmail(),
                        "name", user.getMemberName()),
                "accounts", memberships.stream().map(m -> Map.of(
                        "accountId", m.getAccountId(),
                        "role", m.getRole())).collect(Collectors.toList())));
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest req,
            @AuthenticationPrincipal String userId) {

        UUID caller = UUID.fromString(userId);

        boolean updated = authService.changePasswordById(caller, req.oldPassword(), req.newPassword());
        if (!updated) {
            return ResponseEntity.status(400).body(Map.of("error", "Old password is incorrect"));
        }

        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }
}
