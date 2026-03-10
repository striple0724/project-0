package com.taxworkbench.api.controller;

import com.taxworkbench.api.dto.client.ClientFilter;
import com.taxworkbench.api.dto.client.ClientResponse;
import com.taxworkbench.api.dto.client.CreateClientRequest;
import com.taxworkbench.api.dto.client.UpdateClientRequest;
import com.taxworkbench.api.dto.common.ApiResponse;
import com.taxworkbench.api.dto.common.PagedResponse;
import com.taxworkbench.api.service.ClientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/clients")
@Validated
@Tag(name = "Clients", description = "고객사(Client) 관리 API")
public class ClientController {
    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    @Operation(summary = "고객사 목록 조회", description = "필터와 페이징으로 고객사 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = PagedResponse.class)))
    })
    public ResponseEntity<PagedResponse<ClientResponse>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) com.taxworkbench.api.model.ClientType type,
            @RequestParam(required = false) com.taxworkbench.api.model.ClientTier tier,
            @RequestParam(required = false) com.taxworkbench.api.model.ClientStatus status,
            @RequestParam(required = false) String createdAtFrom,
            @RequestParam(required = false) String createdAtTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(clientService.findAll(new ClientFilter(name, type, tier, status, createdAtFrom, createdAtTo), page, size));
    }

    @PostMapping
    @Operation(
            summary = "고객사 생성",
            description = "신규 고객사를 등록합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CreateClientRequest.class),
                            examples = @ExampleObject(
                                    name = "CreateClient",
                                    value = "{\"name\":\"ABC 세무법인\",\"bizNo\":\"123-45-67890\",\"type\":\"CORPORATE\",\"status\":\"ACTIVE\",\"tier\":\"PREMIUM\"}"
                            )
                    )
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "중복/도메인 규칙 충돌",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ApiResponse<ClientResponse>> create(@Valid @RequestBody CreateClientRequest request) {
        ClientResponse created = clientService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/clients/" + created.id()))
                .body(new ApiResponse<>(created));
    }

    @GetMapping("/{id}")
    @Operation(summary = "고객사 단건 조회", description = "고객사 ID로 단건을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대상 없음",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ApiResponse<ClientResponse>> get(@PathVariable long id) {
        return ResponseEntity.ok(new ApiResponse<>(clientService.findClientById(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "고객사 수정", description = "고객사 정보를 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대상 없음",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "도메인 규칙 충돌",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ApiResponse<ClientResponse>> update(@PathVariable long id, @Valid @RequestBody UpdateClientRequest request) {
        return ResponseEntity.ok(new ApiResponse<>(clientService.updateClient(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "고객사 삭제", description = "고객사를 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대상 없음",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "연관 데이터 충돌",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> delete(@PathVariable long id) {
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }
}
