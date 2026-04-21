package com.marmik.brokerhub.controller;

import com.marmik.brokerhub.model.AccountMember;
import com.marmik.brokerhub.service.AccountAccessValidator;
import com.marmik.brokerhub.service.AccountService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for managing accounts and account memberships.
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final AccountAccessValidator accessValidator;

    public AccountController(
            AccountService accountService,
            AccountAccessValidator accessValidator) {
        this.accountService = accountService;
        this.accessValidator = accessValidator;
    }

    public static record CreateAccountRequest(String accountName, String accountDesc) {
    }

    public record AddMemberRequest(String loginId, String email) {
    }

    public static record RoleUpdateRequest(String role) {
    }

    public static record PrivacyUpdateRequest(String privacy) {
    }

    /** 
     * Create a new account. The caller becomes the ADMIN of the new account.
     */
    @PostMapping
    public ResponseEntity<?> createAccount(
            @RequestBody CreateAccountRequest req,
            @AuthenticationPrincipal String userId) {

        UUID caller = UUID.fromString(userId);
        AccountMember admin = accountService.createAccountForUser(
                caller, req.accountName(), req.accountDesc());

        return ResponseEntity.ok(Map.of(
                "accountId", admin.getAccountId(),
                "name", req.accountName(),
                "role", admin.getRole()));
    }

    /** 
     * List all members for a specific account. 
     * Restricted to ADMIN users only.
     */
    @GetMapping("/{accountId}/members")
    public ResponseEntity<?> listMembers(
            @PathVariable String accountId,
            @AuthenticationPrincipal String userId) {

        UUID caller = UUID.fromString(userId);
        UUID accId = UUID.fromString(accountId);

        accessValidator.requireRole(caller, accId, "ADMIN");

        return ResponseEntity.ok(
                accountService.listMembers(accId).stream().map(m -> Map.of(
                        "memberId", m.getId(),
                        "loginId", m.getUser().getLoginId(),
                        "email", m.getUser().getEmail(),
                        "memberName", m.getUser().getMemberName(),
                        "role", m.getRole())).toList());
    }

    /** 
     * Add a new member to an account. 
     * Restricted to ADMIN users only.
     */
    @PostMapping("/{accountId}/members")
    public ResponseEntity<?> addMember(
            @PathVariable String accountId,
            @RequestBody AddMemberRequest req,
            @AuthenticationPrincipal String userId) {

        UUID caller = UUID.fromString(userId);
        UUID accId = UUID.fromString(accountId);

        accessValidator.requireRole(caller, accId, "ADMIN");

        AccountMember member = accountService.addMember(
                accountId, req.loginId(), req.email());

        return ResponseEntity.ok(Map.of(
                "memberId", member.getId(),
                "accountId", member.getAccountId(),
                "role", member.getRole(),
                "loginId", member.getUser().getLoginId(),
                "email", member.getUser().getEmail(),
                "memberName", member.getUser().getMemberName()));
    }

    /** 
     * Update the role of an existing account member. 
     * Restricted to ADMIN users only.
     */
    @PatchMapping("/{accountId}/members/{memberId}/role")
    public ResponseEntity<?> updateMemberRole(
            @PathVariable String accountId,
            @PathVariable String memberId,
            @RequestBody RoleUpdateRequest body,
            @AuthenticationPrincipal String userId) {

        UUID caller = UUID.fromString(userId);
        UUID accId = UUID.fromString(accountId);
        UUID memId = UUID.fromString(memberId);

        accessValidator.requireRole(caller, accId, "ADMIN");

        AccountMember updated = accountService.updateMemberRole(
                caller, accId, memId, body.role());

        return ResponseEntity.ok(Map.of(
                "memberId", updated.getId(),
                "accountId", updated.getAccountId(),
                "role", updated.getRole()));
    }

    /** 
     * Remove a member from the account. 
     * Restricted to ADMIN users only.
     */
    @DeleteMapping("/{accountId}/members/{memberId}")
    public ResponseEntity<?> removeMember(
            @PathVariable String accountId,
            @PathVariable String memberId,
            @AuthenticationPrincipal String userId) {

        UUID caller = UUID.fromString(userId);
        UUID accId = UUID.fromString(accountId);
        UUID memId = UUID.fromString(memberId);

        accessValidator.requireRole(caller, accId, "ADMIN");

        accountService.removeMember(caller, accId, memId);
        return ResponseEntity.ok(Map.of("message", "Member removed"));
    }

    /** 
     * List all accounts the currently authenticated user belongs to.
     */
    @GetMapping
    public ResponseEntity<?> listUserAccounts(
            @AuthenticationPrincipal String userId) {

        UUID userUUID = UUID.fromString(userId);
        return ResponseEntity.ok(
                accountService.listUserAccountViews(userUUID));
    }

    /** 
     * Update privacy rules for the caller's own membership in an account.
     */
    @PatchMapping("/{accountId}/members/{memberId}/rule")
    public ResponseEntity<?> updateMemberRule(
            @PathVariable String accountId,
            @PathVariable String memberId,
            @RequestBody PrivacyUpdateRequest body,
            @AuthenticationPrincipal String userId) {

        UUID caller = UUID.fromString(userId);
        UUID accId = UUID.fromString(accountId);
        UUID memId = UUID.fromString(memberId);

        AccountMember updated = accountService.updateOwnMemberPrivacy(
                caller, accId, memId, body.privacy());

        return ResponseEntity.ok(Map.of(
                "memberId", updated.getId(),
                "accountId", updated.getAccountId(),
                "rules", updated.getRules()));
    }
}
