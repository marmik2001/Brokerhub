package com.marmik.brokerhub.account.controller;

import com.marmik.brokerhub.account.model.AccountMember;
import com.marmik.brokerhub.config.JwtUtil;
import com.marmik.brokerhub.account.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtUtil jwtUtil;

    public AuthController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    public static record LoginRequest(String loginId, String password) {
    }

    public static record ChangePasswordRequest(String oldPassword, String newPassword) {
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        AccountMember member = authService.authenticate(req.loginId(), req.password());
        if (member == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }
        String token = jwtUtil.generateToken(
                member.getId().toString(),
                member.getAccountId().toString(),
                member.getRole());

        return ResponseEntity.ok(Map.of(
                "token", token,
                "memberId", member.getId(),
                "accountId", member.getAccountId(),
                "role", member.getRole()));
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest req,
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String memberId = jwtUtil.getMemberId(token).orElse(null);

        if (memberId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }

        boolean updated = authService.changePasswordById(memberId, req.oldPassword(), req.newPassword());
        if (!updated) {
            return ResponseEntity.status(400).body(Map.of("error", "Old password is incorrect"));
        }
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

}
