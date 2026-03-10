package com.taxworkbench.api.service;

import com.taxworkbench.api.dto.client.ClientFilter;
import com.taxworkbench.api.dto.client.ClientResponse;
import com.taxworkbench.api.dto.client.CreateClientRequest;
import com.taxworkbench.api.dto.client.UpdateClientRequest;
import com.taxworkbench.api.dto.common.PagedResponse;

public interface ClientService {
    PagedResponse<ClientResponse> findAll(ClientFilter filter, int page, int size);

    ClientResponse findClientById(long id);

    ClientResponse create(CreateClientRequest request);

    ClientResponse updateClient(long id, UpdateClientRequest request);

    void deleteClient(long id);
}
