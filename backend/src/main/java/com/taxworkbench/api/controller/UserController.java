package com.taxworkbench.api.controller;

import com.taxworkbench.api.dto.common.ApiResponse;
import com.taxworkbench.api.dto.user.CreateUserRequest;
import com.taxworkbench.api.dto.user.UpdateUserRequest;
import com.taxworkbench.api.dto.user.UserResponse;
import com.taxworkbench.api.service.UserAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "사용자 계정 관리 API")
public class UserController {
    private final UserAccountService service;

    public UserController(UserAccountService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "사용자 목록 조회", description = "전체 사용자 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<UserResponse>>> list() {
        return ResponseEntity.ok(new ApiResponse<>(service.findAll()));
    }

    @GetMapping("/{seq}")
    @Operation(summary = "사용자 단건 조회", description = "사용자 SEQ로 단건을 조회합니다.")
    public ResponseEntity<ApiResponse<UserResponse>> get(@PathVariable long seq) {
        return ResponseEntity.ok(new ApiResponse<>(service.findBySeq(seq)));
    }

    @PostMapping
    @Operation(summary = "사용자 생성", description = "신규 사용자 계정을 생성합니다.")
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse created = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + created.seq()))
                .body(new ApiResponse<>(created));
    }

    @PutMapping("/{seq}")
    @Operation(summary = "사용자 수정", description = "사용자 계정 정보를 수정합니다.")
    public ResponseEntity<ApiResponse<UserResponse>> update(@PathVariable long seq, @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(service.update(seq, request)));
    }

    @DeleteMapping("/{seq}")
    @Operation(summary = "사용자 삭제", description = "사용자 계정을 삭제합니다.")
    public ResponseEntity<Void> delete(@PathVariable long seq) {
        service.delete(seq);
        return ResponseEntity.noContent().build();
    }
}
