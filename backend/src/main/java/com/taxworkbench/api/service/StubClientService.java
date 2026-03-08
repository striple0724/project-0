package com.taxworkbench.api.service;

import com.taxworkbench.api.dto.client.ClientFilter;
import com.taxworkbench.api.dto.client.ClientResponse;
import com.taxworkbench.api.dto.client.CreateClientRequest;
import com.taxworkbench.api.dto.common.PageInfo;
import com.taxworkbench.api.dto.common.PagedResponse;

import java.util.List;

public class StubClientService implements ClientService {
    @Override
    public PagedResponse<ClientResponse> findAll(ClientFilter filter, int page, int size) {
        ClientResponse client = new ClientResponse(11L, "ACME Tax Co.", "123-45-67890", filter.type(), filter.status(), filter.tier());
        return new PagedResponse<>(List.of(client), new PageInfo(page, size, 1, 1));
    }

    @Override
    public ClientResponse create(CreateClientRequest request) {
        return new ClientResponse(12L, request.name(), request.bizNo(), request.type(), request.status(), request.tier());
    }
}
