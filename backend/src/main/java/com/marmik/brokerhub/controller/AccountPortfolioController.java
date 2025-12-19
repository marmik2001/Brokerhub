package com.marmik.brokerhub.controller;

import com.marmik.brokerhub.service.AccountPortfolioService;
import com.marmik.brokerhub.service.AccountAccessValidator;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/accounts")
public class AccountPortfolioController {

    private final AccountPortfolioService portfolioService;
    private final AccountAccessValidator accessValidator;

    public AccountPortfolioController(
            AccountPortfolioService portfolioService,
            AccountAccessValidator accessValidator) {
        this.portfolioService = portfolioService;
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
            @AuthenticationPrincipal String userId) {

        UUID caller;
        UUID accId;
        try {
            caller = UUID.fromString(userId);
            accId = UUID.fromString(accountId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid UUID"));
        }

        // ensure caller is a member of the account (throws AccessDeniedException if
        // not)
        try {
            accessValidator.requireMembership(caller, accId);
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
        }

        // returns Map { full: [...], partial: [...] }
        Map<String, Object> result = portfolioService.aggregateHoldingsForAccount(accId, caller);

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/accounts/{accountId}/aggregate-positions
     *
     * Returns aggregated positions for the account across all members' connected
     * brokers. Uses identical auth & membership checks as holdings.
     */
    @GetMapping("/{accountId}/aggregate-positions")
    public ResponseEntity<?> getAggregatePositions(
            @PathVariable String accountId,
            @AuthenticationPrincipal String userId) {

        UUID caller;
        UUID accId;
        try {
            caller = UUID.fromString(userId);
            accId = UUID.fromString(accountId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid UUID"));
        }

        // ensure caller is a member of the account
        try {
            accessValidator.requireMembership(caller, accId);
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            return ResponseEntity.status(403).body(Map.of("error", ex.getMessage()));
        }

        // returns Map { full: [...], partial: [...] }
        Map<String, Object> result = portfolioService.aggregatePositionsForAccount(accId, caller);

        return ResponseEntity.ok(result);
    }
}
