package com.taxworkbench.api.controller;

import com.taxworkbench.api.dto.client.ClientFilter;
import com.taxworkbench.api.dto.client.ClientResponse;
import com.taxworkbench.api.dto.client.CreateClientRequest;
import com.taxworkbench.api.dto.common.ApiResponse;
import com.taxworkbench.api.dto.common.PagedResponse;
import com.taxworkbench.api.service.ClientService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/clients")
@Validated
public class ClientController {
    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<ClientResponse>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) com.taxworkbench.api.model.ClientType type,
            @RequestParam(required = false) com.taxworkbench.api.model.ClientTier tier,
            @RequestParam(required = false) com.taxworkbench.api.model.ClientStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(clientService.findAll(new ClientFilter(name, type, tier, status), page, size));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ClientResponse>> create(@Valid @RequestBody CreateClientRequest request) {
        ClientResponse created = clientService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/clients/" + created.id()))
                .body(new ApiResponse<>(created));
    }
}
