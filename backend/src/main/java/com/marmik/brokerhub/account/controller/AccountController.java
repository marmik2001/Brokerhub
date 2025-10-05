package com.marmik.brokerhub.account.controller;

import com.marmik.brokerhub.account.model.AccountMember;
import com.marmik.brokerhub.account.repository.AccountMemberRepository;
import com.marmik.brokerhub.account.service.AccountService;
import com.marmik.brokerhub.account.service.AccountAccessValidator;
import com.marmik.brokerhub.config.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final AccountMemberRepository memberRepo;
    private final JwtUtil jwtUtil;
    private final AccountAccessValidator accessValidator;

    public AccountController(AccountService accountService,
            AccountMemberRepository memberRepo,
            JwtUtil jwtUtil,
            AccountAccessValidator accessValidator) {
        this.accountService = accountService;
        this.memberRepo = memberRepo;
        this.jwtUtil = jwtUtil;
        this.accessValidator = accessValidator;
    }

    public static record SignupRequest(
            String accountName,
            String accountDesc,
            String loginId,
            String memberName,
            String email,
            String password) {
    }

    public static record AddMemberRequest(
            String loginId,
            String memberName,
            String email,
            String password,
            String role) {
    }

    /** Public: create new account + admin user. */
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest req) {
        try {
            AccountMember admin = accountService.createAccountWithAdmin(
                    req.accountName(),
                    req.accountDesc(),
                    req.loginId(),
                    req.memberName(),
                    req.email(),
                    req.password());

            return ResponseEntity.ok(Map.of(
                    "memberId", admin.getId(),
                    "accountId", admin.getAccountId(),
                    "role", admin.getRole()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Authenticated: add member to an account (ADMIN only). */
    @PostMapping("/{accountId}/members")
    public ResponseEntity<?> addMember(@PathVariable String accountId,
            @RequestBody AddMemberRequest req,
            @RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.toLowerCase().startsWith("bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
        }

        String token = authHeader.substring(7).trim();
        String userIdStr = jwtUtil.getUserId(token).orElse(null);
        if (userIdStr == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }

        UUID userId = UUID.fromString(userIdStr);
        UUID accId = UUID.fromString(accountId);

        // ✅ Use centralized validator instead of inline membership checks
        try {
            accessValidator.requireRole(userId, accId, "ADMIN");
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
        }

        try {
            AccountMember member = accountService.addMember(
                    accountId,
                    req.loginId(),
                    req.memberName(),
                    req.email(),
                    req.password(),
                    req.role());
            return ResponseEntity.ok(Map.of(
                    "memberId", member.getId(),
                    "accountId", member.getAccountId(),
                    "role", member.getRole()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /** Authenticated: list all accounts current user belongs to. */
    @GetMapping
    public ResponseEntity<?> listUserAccounts(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.toLowerCase().startsWith("bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
        }

        String token = authHeader.substring(7).trim();
        String userIdStr = jwtUtil.getUserId(token).orElse(null);
        if (userIdStr == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }

        UUID userUUID = UUID.fromString(userIdStr);
        List<AccountMember> memberships = memberRepo.findByUserId(userUUID);

        return ResponseEntity.ok(memberships.stream().map(m -> Map.of(
                "accountId", m.getAccountId(),
                "role", m.getRole())).collect(Collectors.toList()));
    }
}
