package com.marmik.brokerhub.controller;

import com.marmik.brokerhub.service.AccountHoldingsService;
import com.marmik.brokerhub.service.AccountAccessValidator;
import com.marmik.brokerhub.security.JwtUtil;
import com.marmik.brokerhub.dto.AggregatedHolding;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
public class AccountHoldingsController {

    private final AccountHoldingsService holdingsService;
    private final JwtUtil jwtUtil;
    private final AccountAccessValidator accessValidator;

    public AccountHoldingsController(AccountHoldingsService holdingsService,
            JwtUtil jwtUtil,
            AccountAccessValidator accessValidator) {
        this.holdingsService = holdingsService;
        this.jwtUtil = jwtUtil;
        this.accessValidator = accessValidator;
    }

    /**
     * GET /api/accounts/{accountId}/aggregate-holdings
     *
     * Returns aggregated holdings for the account across all members' connected
     * brokers.
     * Accessible to any member of the account.
     */
    @GetMapping("/{accountId}/aggregate-holdings")
    public ResponseEntity<?> getAggregateHoldings(
            @PathVariable String accountId,
            @RequestHeader("Authorization") String authHeader) {

        if (authHeader == null || !authHeader.toLowerCase().startsWith("bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
        }

        String token = authHeader.substring(7).trim();
        String userIdStr = jwtUtil.getUserId(token).orElse(null);
        if (userIdStr == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }

        UUID userId;
        UUID accId;
        try {
            userId = UUID.fromString(userIdStr);
            accId = UUID.fromString(accountId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid UUID"));
        }

        // ensure caller is a member of the account (throws AccessDeniedException if
        // not)
        try {
            accessValidator.requireMembership(userId, accId);
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
        }

        // Delegate to service; service will handle concurrent fetching and aggregation.
        List<AggregatedHolding> aggregated = holdingsService.aggregateHoldingsForAccount(accId, userId);

        return ResponseEntity.ok(aggregated);
    }
}
