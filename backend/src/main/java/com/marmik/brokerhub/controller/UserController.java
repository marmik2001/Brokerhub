package com.marmik.brokerhub.controller;

import com.marmik.brokerhub.model.User;
import com.marmik.brokerhub.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST controller for managing user profiles and registration.
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Get the currently authenticated user's profile.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(
            @AuthenticationPrincipal String userId) {

        UUID caller = UUID.fromString(userId);
        return ResponseEntity.ok(userService.getUserProfile(caller));
    }

    /**
     * Register a new user.
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(
            @RequestBody Map<String, String> body) {

        User user = userService.registerUser(
                body.get("loginId"),
                body.get("memberName"),
                body.get("email"),
                body.get("password"));

        return ResponseEntity.ok(Map.of(
                "id", user.getId(),
                "loginId", user.getLoginId(),
                "email", user.getEmail(),
                "name", user.getMemberName()));
    }
}
