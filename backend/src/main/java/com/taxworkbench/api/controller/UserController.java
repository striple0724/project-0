package com.taxworkbench.api.controller;

import com.taxworkbench.api.dto.common.ApiResponse;
import com.taxworkbench.api.dto.user.CreateUserRequest;
import com.taxworkbench.api.dto.user.UpdateUserRequest;
import com.taxworkbench.api.dto.user.UserResponse;
import com.taxworkbench.api.service.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserAccountService service;

    public UserController(UserAccountService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> list() {
        return ResponseEntity.ok(new ApiResponse<>(service.findAll()));
    }

    @GetMapping("/{seq}")
    public ResponseEntity<ApiResponse<UserResponse>> get(@PathVariable long seq) {
        return ResponseEntity.ok(new ApiResponse<>(service.findBySeq(seq)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + created.seq()))
                .body(new ApiResponse<>(created));
    }

    @PutMapping("/{seq}")
    public ResponseEntity<ApiResponse<UserResponse>> update(@PathVariable long seq, @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(service.update(seq, request)));
    }

    @DeleteMapping("/{seq}")
    public ResponseEntity<Void> delete(@PathVariable long seq) {
        service.delete(seq);
        return ResponseEntity.noContent().build();
    }
}
