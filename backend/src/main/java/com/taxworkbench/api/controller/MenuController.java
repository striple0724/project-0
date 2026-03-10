package com.taxworkbench.api.controller;

import com.taxworkbench.api.dto.common.ApiResponse;
import com.taxworkbench.api.dto.menu.CreateMenuRequest;
import com.taxworkbench.api.dto.menu.MenuResponse;
import com.taxworkbench.api.dto.menu.UpdateMenuRequest;
import com.taxworkbench.api.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/menus")
@Tag(name = "Menus", description = "메뉴 관리 API")
public class MenuController {
    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping
    @Operation(summary = "메뉴 목록 조회", description = "전체 메뉴 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<MenuResponse>>> list() {
        return ResponseEntity.ok(new ApiResponse<>(menuService.findAll()));
    }

    @GetMapping("/{menuPk}")
    @Operation(summary = "메뉴 단건 조회", description = "메뉴 PK로 단건을 조회합니다.")
    public ResponseEntity<ApiResponse<MenuResponse>> get(@PathVariable long menuPk) {
        return ResponseEntity.ok(new ApiResponse<>(menuService.findByPk(menuPk)));
    }

    @PostMapping
    @Operation(summary = "메뉴 생성", description = "신규 메뉴를 생성합니다.")
    public ResponseEntity<ApiResponse<MenuResponse>> create(@Valid @RequestBody CreateMenuRequest request) {
        MenuResponse created = menuService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/menus/" + created.menuPk()))
                .body(new ApiResponse<>(created));
    }

    @PutMapping("/{menuPk}")
    @Operation(summary = "메뉴 수정", description = "메뉴 정보를 수정합니다.")
    public ResponseEntity<ApiResponse<MenuResponse>> update(@PathVariable long menuPk, @Valid @RequestBody UpdateMenuRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(menuService.update(menuPk, request)));
    }

    @DeleteMapping("/{menuPk}")
    @Operation(summary = "메뉴 삭제", description = "메뉴를 삭제합니다.")
    public ResponseEntity<Void> delete(@PathVariable long menuPk) {
        menuService.delete(menuPk);
        return ResponseEntity.noContent().build();
    }
}
