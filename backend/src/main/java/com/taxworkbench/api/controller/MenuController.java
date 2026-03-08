package com.taxworkbench.api.controller;

import com.taxworkbench.api.dto.common.ApiResponse;
import com.taxworkbench.api.dto.menu.CreateMenuRequest;
import com.taxworkbench.api.dto.menu.MenuResponse;
import com.taxworkbench.api.dto.menu.UpdateMenuRequest;
import com.taxworkbench.api.service.MenuService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/menus")
public class MenuController {
    private final MenuService menuService;

    public MenuController(MenuService menuService) {
        this.menuService = menuService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MenuResponse>>> list() {
        return ResponseEntity.ok(new ApiResponse<>(menuService.findAll()));
    }

    @GetMapping("/{menuPk}")
    public ResponseEntity<ApiResponse<MenuResponse>> get(@PathVariable long menuPk) {
        return ResponseEntity.ok(new ApiResponse<>(menuService.findByPk(menuPk)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MenuResponse>> create(@Valid @RequestBody CreateMenuRequest request) {
        MenuResponse created = menuService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/menus/" + created.menuPk()))
                .body(new ApiResponse<>(created));
    }

    @PutMapping("/{menuPk}")
    public ResponseEntity<ApiResponse<MenuResponse>> update(@PathVariable long menuPk, @Valid @RequestBody UpdateMenuRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(menuService.update(menuPk, request)));
    }

    @DeleteMapping("/{menuPk}")
    public ResponseEntity<Void> delete(@PathVariable long menuPk) {
        menuService.delete(menuPk);
        return ResponseEntity.noContent().build();
    }
}
