package com.marmik.brokerhub.controller;

import com.marmik.brokerhub.service.UserService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal String userId) {

        UUID caller = UUID.fromString(userId);
        Optional<Map<String, Object>> profileOpt = userService.getUserProfile(caller);
        if (profileOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "User not found"));
        }

        return ResponseEntity.ok(profileOpt.get());
    }

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> body) {
        try {
            String loginId = body.get("loginId");
            String memberName = body.get("memberName");
            String email = body.get("email");
            String password = body.get("password");

            var user = userService.registerUser(loginId, memberName, email, password);

            return ResponseEntity.ok(Map.of(
                    "id", user.getId(),
                    "loginId", user.getLoginId(),
                    "email", user.getEmail(),
                    "name", user.getMemberName()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

}
