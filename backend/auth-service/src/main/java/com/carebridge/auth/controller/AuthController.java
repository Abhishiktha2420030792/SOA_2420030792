package com.carebridge.auth.controller;

import com.carebridge.auth.dto.AuthResponse;
import com.carebridge.auth.dto.LoginRequest;
import com.carebridge.auth.dto.RegisterRequest;
import com.carebridge.auth.model.User;
import com.carebridge.auth.repository.UserRepository;
import com.carebridge.auth.service.AuthService;
import com.carebridge.common.dto.ApiResponse;
import com.carebridge.common.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestBody Map<String, String> body) {
        AuthResponse response = authService.refresh(body.get("refreshToken"));
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", response));
    }

    /**
     * Internal, service-to-service lookup (e.g. patient-service / provider-service
     * confirming a userId is legitimate and fetching its role/name at record-creation time).
     * In production this would be locked down to internal traffic only (network policy /
     * a shared internal API key) - called out here for the pitch/demo narrative.
     */
    @GetMapping("/internal/users/{id}")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        user.setPasswordHash(null); // never leak the hash, even internally
        return ResponseEntity.ok(ApiResponse.success("User found", user));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> getCurrentUser(@RequestAttribute("userId") String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setPasswordHash(null);
        return ResponseEntity.ok(ApiResponse.success("Current user", user));
    }
}
