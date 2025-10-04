package com.marmik.brokerhub.account.controller;

import com.marmik.brokerhub.account.model.AccountMember;
import com.marmik.brokerhub.account.model.User;
import com.marmik.brokerhub.account.repository.AccountMemberRepository;
import com.marmik.brokerhub.account.service.AuthService;
import com.marmik.brokerhub.config.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
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

        // Fetch all memberships for this user
        List<AccountMember> memberships = memberRepo.findByUserId(user.getId());
        if (memberships.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("error", "No account memberships found"));
        }

        // Choose first account by default for now
        AccountMember active = memberships.get(0);

        String token = jwtUtil.generateToken(
                user.getId().toString(),
                active.getAccountId().toString(),
                active.getRole());

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
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String userId = jwtUtil.getMemberId(token).orElse(null);

        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }

        boolean updated = authService.changePasswordById(userId, req.oldPassword(), req.newPassword());
        if (!updated) {
            return ResponseEntity.status(400).body(Map.of("error", "Old password is incorrect"));
        }

        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }
}
