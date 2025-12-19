package com.marmik.brokerhub.controller;

import com.marmik.brokerhub.model.BrokerCredential;
import com.marmik.brokerhub.repository.AccountMemberRepository;
import com.marmik.brokerhub.service.BrokerCredentialService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Endpoints:
 * POST /api/brokers -> store credential
 * GET /api/brokers?accountMemberId=... -> list credentials for account_member
 * (members only)
 * DELETE /api/brokers/{credentialId} -> delete credential (members only)
 */
@RestController
@RequestMapping("/api/brokers")
public class BrokerCredentialController {

    private final BrokerCredentialService service;
    private final AccountMemberRepository accountMemberRepository;

    public BrokerCredentialController(BrokerCredentialService service,
            AccountMemberRepository accountMemberRepository) {
        this.service = service;
        this.accountMemberRepository = accountMemberRepository;
    }

    public static record StoreRequest(String accountMemberId, String broker, String token, String nickname) {
    }

    @PostMapping
    public ResponseEntity<?> store(@RequestBody StoreRequest req,
            @AuthenticationPrincipal String userId) {
        UUID caller = UUID.fromString(userId);
        UUID amId;
        try {
            amId = UUID.fromString(req.accountMemberId());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid accountMemberId"));
        }

        // validate accountMember exists
        if (accountMemberRepository.findById(amId).isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "account member not found"));
        }

        try {
            byte[] tokenBytes = req.token().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            String nickname = req.nickname();
            if (nickname == null || nickname.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "nickname is required"));
            }
            BrokerCredential bc = service.storeCredential(caller, amId, req.broker(), nickname, tokenBytes);

            // zero token buffer
            Arrays.fill(tokenBytes, (byte) 0);

            return ResponseEntity.ok(Map.of(
                    "credentialId", bc.getCredentialId(),
                    "accountMemberId", bc.getAccountMemberId(),
                    "broker", bc.getBroker(),
                    "nickname", bc.getNickname()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "encryption error"));
        }
    }

    @GetMapping
    public ResponseEntity<?> list(@RequestParam("accountMemberId") String accountMemberId,
            @AuthenticationPrincipal String userId) {
        UUID caller = UUID.fromString(userId);
        UUID amId;
        try {
            amId = UUID.fromString(accountMemberId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid accountMemberId"));
        }

        if (accountMemberRepository.findById(amId).isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "account member not found"));
        }

        try {
            List<BrokerCredential> list = service.listByAccountMember(caller, amId);
            var payload = list.stream().map(bc -> Map.of(
                    "credentialId", bc.getCredentialId(),
                    "accountMemberId", bc.getAccountMemberId(),
                    "broker", bc.getBroker(),
                    "nickname", bc.getNickname())).collect(Collectors.toList());
            return ResponseEntity.ok(payload);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{credentialId}")
    public ResponseEntity<?> delete(@PathVariable String credentialId,
            @AuthenticationPrincipal String userId) {
        UUID caller = UUID.fromString(userId);
        UUID credId;
        try {
            credId = UUID.fromString(credentialId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid credentialId"));
        }
        try {
            service.deleteCredential(caller, credId);
            return ResponseEntity.ok(Map.of("message", "deleted"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }
}
