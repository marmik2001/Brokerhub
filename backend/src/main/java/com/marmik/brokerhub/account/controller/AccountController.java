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

    public static record CreateAccountRequest(String accountName, String accountDesc) {
    }

    public static record AddMemberRequest(
            String loginId,
            String memberName,
            String email,
            String password,
            String role) {
    }

    /** Authenticated: create a new account for current user as ADMIN. */
    @PostMapping
    public ResponseEntity<?> createAccount(
            @RequestBody CreateAccountRequest req,
            @RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.toLowerCase().startsWith("bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
        }

        String token = authHeader.substring(7).trim();
        String userIdStr = jwtUtil.getUserId(token).orElse(null);
        if (userIdStr == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }

        try {
            UUID userId = UUID.fromString(userIdStr);
            AccountMember admin = accountService.createAccountForUser(
                    userId,
                    req.accountName(),
                    req.accountDesc());

            return ResponseEntity.ok(Map.of(
                    "accountId", admin.getAccountId(),
                    "name", req.accountName(),
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
