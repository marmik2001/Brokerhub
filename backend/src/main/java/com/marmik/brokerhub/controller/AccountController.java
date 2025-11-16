package com.marmik.brokerhub.controller;

import com.marmik.brokerhub.model.Account;
import com.marmik.brokerhub.model.AccountMember;
import com.marmik.brokerhub.repository.AccountMemberRepository;
import com.marmik.brokerhub.security.JwtUtil;
import com.marmik.brokerhub.service.AccountAccessValidator;
import com.marmik.brokerhub.service.AccountService;

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

    public record AddMemberRequest(
            String loginId,
            String email) {
    }

    public static record RoleUpdateRequest(String role) {
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

    /** Authenticated: list all members for the account (ADMIN only). */
    @GetMapping("/{accountId}/members")
    public ResponseEntity<?> listMembers(@PathVariable String accountId,
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

        List<AccountMember> members = accountService.listMembers(accId);

        var payload = members.stream().map(m -> Map.of(
                "memberId", m.getId(),
                "loginId", m.getUser().getLoginId(),
                "email", m.getUser().getEmail(),
                "memberName", m.getUser().getMemberName(),
                "role", m.getRole())).collect(Collectors.toList());

        return ResponseEntity.ok(payload);
    }

    /** Authenticated: add member to an account (ADMIN only). */
    @PostMapping("/{accountId}/members")
    public ResponseEntity<?> addMember(
            @PathVariable String accountId,
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
                    req.email());
            return ResponseEntity.ok(Map.of(
                    "memberId", member.getId(),
                    "accountId", member.getAccountId(),
                    "role", member.getRole(),
                    "loginId", member.getUser().getLoginId(),
                    "email", member.getUser().getEmail(),
                    "memberName", member.getUser().getMemberName()));
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase().contains("already")) {
                return ResponseEntity.status(409).body(Map.of("error", msg));
            }
            return ResponseEntity.badRequest().body(Map.of("error", msg));
        }
    }

    /** Authenticated: update a member's role (ADMIN only). */
    @PatchMapping("/{accountId}/members/{memberId}/role")
    public ResponseEntity<?> updateMemberRole(@PathVariable String accountId,
            @PathVariable String memberId,
            @RequestBody RoleUpdateRequest body,
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
        UUID memId = UUID.fromString(memberId);

        try {
            accessValidator.requireRole(userId, accId, "ADMIN");
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
        }

        try {
            // safety: prevent admin from demoting themselves
            Optional<AccountMember> currentMembership = memberRepo.findByUserIdAndAccountId(userId, accId);
            if (currentMembership.isPresent() && currentMembership.get().getId().equals(memId)) {
                // attempting to update own role
                if (!"ADMIN".equalsIgnoreCase(body.role())) {
                    return ResponseEntity.status(409).body(Map.of("error", "Cannot demote yourself"));
                }
                // else setting own role to ADMIN is no-op
            }

            AccountMember updated = accountService.updateMemberRole(accId, memId, body.role());
            return ResponseEntity.ok(Map.of(
                    "memberId", updated.getId(),
                    "accountId", updated.getAccountId(),
                    "role", updated.getRole()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    /** Authenticated: remove member from account (ADMIN only). */
    @DeleteMapping("/{accountId}/members/{memberId}")
    public ResponseEntity<?> removeMember(@PathVariable String accountId,
            @PathVariable String memberId,
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
        UUID memId = UUID.fromString(memberId);

        try {
            accessValidator.requireRole(userId, accId, "ADMIN");
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
        }

        try {
            // prevent admin removing themselves
            Optional<AccountMember> currentMembership = memberRepo.findByUserIdAndAccountId(userId, accId);
            if (currentMembership.isPresent() && currentMembership.get().getId().equals(memId)) {
                return ResponseEntity.status(409).body(Map.of("error", "Cannot remove yourself from the account"));
            }

            accountService.removeMember(accId, memId);
            return ResponseEntity.ok(Map.of("message", "Member removed"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
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

        var payload = memberships.stream().map(m -> {
            // fetch account details if present
            Optional<Account> accOpt = accountService.getAccountById(m.getAccountId());

            Map<String, Object> map = new HashMap<>();
            map.put("accountId", m.getAccountId());
            map.put("name", accOpt.map(Account::getName).orElse(null));
            map.put("description", accOpt.map(Account::getDescription).orElse(null));
            map.put("role", m.getRole());
            map.put("accountMemberId", m.getId());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(payload);
    }
}
