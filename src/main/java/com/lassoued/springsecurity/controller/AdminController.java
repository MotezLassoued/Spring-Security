package com.lassoued.springsecurity.controller;

import com.lassoued.springsecurity.service.AuthenticationService;
import com.lassoued.springsecurity.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final AuthenticationService authenticationService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('admin:read')")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("totalUsers", userService.getAllUsers().size());
        dashboard.put("timestamp", Instant.now());
        return ResponseEntity.ok(dashboard);
    }

    @PostMapping("/cleanup-tokens")
    @PreAuthorize("hasAuthority('admin:update')")
    public ResponseEntity<?> cleanupExpiredTokens() {
        authenticationService.cleanupExpiredTokens();
        return ResponseEntity.ok(Map.of("message", "Expired tokens cleaned up"));
    }
}