package com.marmik.brokerhub.controller;

import com.marmik.brokerhub.model.BrokerCredential;
import com.marmik.brokerhub.service.BrokerCredentialService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/brokers")
public class BrokerCredentialController {

    private final BrokerCredentialService service;

    public BrokerCredentialController(BrokerCredentialService service) {
        this.service = service;
    }

    public static record StoreRequest(
            String accountMemberId,
            String broker,
            String token,
            String nickname) {
    }

    /** Store broker credential (member of account only). */
    @PostMapping
    public ResponseEntity<?> store(
            @RequestBody StoreRequest req,
            @AuthenticationPrincipal String userId) throws Exception {

        UUID caller = UUID.fromString(userId);
        UUID accountMemberId = UUID.fromString(req.accountMemberId());

        if (req.nickname() == null || req.nickname().isBlank()) {
            throw new IllegalArgumentException("nickname is required");
        }

        byte[] tokenBytes = req.token().getBytes(StandardCharsets.UTF_8);
        try {
            BrokerCredential bc = service.storeCredential(
                    caller,
                    accountMemberId,
                    req.broker(),
                    req.nickname(),
                    tokenBytes);

            return ResponseEntity.ok(Map.of(
                    "credentialId", bc.getCredentialId(),
                    "accountMemberId", bc.getAccountMemberId(),
                    "broker", bc.getBroker(),
                    "nickname", bc.getNickname()));
        } finally {
            // always zero token buffer
            Arrays.fill(tokenBytes, (byte) 0);
        }
    }

    /** List credentials for account member (member of account only). */
    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam("accountMemberId") String accountMemberId,
            @AuthenticationPrincipal String userId) {

        UUID caller = UUID.fromString(userId);
        UUID amId = UUID.fromString(accountMemberId);

        return ResponseEntity.ok(
                service.listByAccountMember(caller, amId).stream().map(bc -> Map.of(
                        "credentialId", bc.getCredentialId(),
                        "accountMemberId", bc.getAccountMemberId(),
                        "broker", bc.getBroker(),
                        "nickname", bc.getNickname())).toList());
    }

    /** Delete credential (member of account only). */
    @DeleteMapping("/{credentialId}")
    public ResponseEntity<?> delete(
            @PathVariable String credentialId,
            @AuthenticationPrincipal String userId) {

        UUID caller = UUID.fromString(userId);
        UUID credId = UUID.fromString(credentialId);

        service.deleteCredential(caller, credId);
        return ResponseEntity.ok(Map.of("message", "deleted"));
    }
}
