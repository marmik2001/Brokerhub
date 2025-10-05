package com.marmik.brokerhub.account.controller;

import com.marmik.brokerhub.account.service.UserService;
import com.marmik.brokerhub.config.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public UserController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Get the currently authenticated user's profile.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.toLowerCase().startsWith("bearer ")) {
            return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid Authorization header"));
        }

        String token = authHeader.substring(7).trim();
        String userIdStr = jwtUtil.getUserId(token).orElse(null);
        if (userIdStr == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid token"));
        }

        UUID userId = UUID.fromString(userIdStr);
        Optional<Map<String, Object>> profileOpt = userService.getUserProfile(userId);
        if (profileOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        return ResponseEntity.ok(profileOpt.get());
    }
}
