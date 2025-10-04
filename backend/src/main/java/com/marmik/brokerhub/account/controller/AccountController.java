package com.marmik.brokerhub.account.controller;

import com.marmik.brokerhub.account.model.AccountMember;
import com.marmik.brokerhub.account.service.AccountService;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
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
            String password,
            String role) {
    }

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

    @PostMapping("/{accountId}/members")
    public ResponseEntity<?> addMember(
            @PathVariable String accountId,
            @RequestBody AddMemberRequest req) {
        try {
            AccountMember member = accountService.addMember(
                    accountId,
                    req.loginId(),
                    req.memberName(),
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
}