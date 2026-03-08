package com.taxworkbench.api.controller;

import com.taxworkbench.api.dto.auth.LoginRequest;
import com.taxworkbench.api.dto.auth.LoginResponse;
import com.taxworkbench.api.dto.common.ApiResponse;
import com.taxworkbench.api.service.UserAccountService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserAccountService userAccountService;

    public AuthController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request, HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(userAccountService.login(request, session)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<LoginResponse>> me(HttpSession session) {
        return ResponseEntity.ok(new ApiResponse<>(userAccountService.currentUser(session)));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        userAccountService.logout(session);
        return ResponseEntity.noContent().build();
    }
}
