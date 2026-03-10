package com.taxworkbench.api.controller;

import com.taxworkbench.api.dto.common.ApiResponse;
import com.taxworkbench.api.service.LargeSeedLoader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/seed")
@Tag(name = "Admin Seed", description = "대량 데이터 적재 관리 API")
public class SeedAdminController {
    private final LargeSeedLoader largeSeedLoader;

    public SeedAdminController(LargeSeedLoader largeSeedLoader) {
        this.largeSeedLoader = largeSeedLoader;
    }

    @GetMapping("/status")
    @Operation(summary = "대량 적재 상태 조회", description = "대량 시드 데이터 적재 작업의 현재 상태를 조회합니다.")
    public ResponseEntity<ApiResponse<LargeSeedLoader.SeedStatus>> status() {
        return ResponseEntity.ok(new ApiResponse<>(largeSeedLoader.getStatus()));
    }

    @PostMapping("/start")
    @Operation(summary = "대량 적재 시작", description = "대량 시드 데이터 적재 작업을 수동 시작합니다.")
    public ResponseEntity<ApiResponse<LargeSeedLoader.SeedStatus>> start() {
        largeSeedLoader.triggerManual();
        return ResponseEntity.accepted().body(new ApiResponse<>(largeSeedLoader.getStatus()));
    }
}
