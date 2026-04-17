package com.marmik.brokerhub.controller;

import com.marmik.brokerhub.model.BrokerCredential;
import com.marmik.brokerhub.service.BrokerCredentialService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

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
            @NotBlank(message = "accountMemberId is required")
            String accountMemberId,
            @NotBlank(message = "broker is required")
            String broker,
            @NotBlank(message = "token is required")
            String token,
            @NotBlank(message = "nickname is required")
            String nickname) {
    }

    /** Store broker credential (owner membership or account admin). */
    @PostMapping
    public ResponseEntity<?> store(
            @Valid @RequestBody StoreRequest req,
            @AuthenticationPrincipal String userId) throws Exception {

        UUID caller = UUID.fromString(userId);
        UUID accountMemberId = UUID.fromString(req.accountMemberId());

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

    /** List credentials for account member (owner membership or account admin). */
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

    /** Delete credential (owner membership or account admin). */
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
