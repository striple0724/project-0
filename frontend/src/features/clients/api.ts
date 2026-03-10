import { httpClient } from "../../api/http-client";
import type { Client, ClientFilters, PagedClients } from "./types";

export async function fetchClients(filters: ClientFilters): Promise<PagedClients> {
  const response = await httpClient.get<PagedClients>("/api/v1/clients", {
    params: {
      name: filters.name || undefined,
      type: filters.type || undefined,
      tier: filters.tier || undefined,
      status: filters.status || undefined,
      createdAtFrom: filters.createdAtFrom || undefined,
      createdAtTo: filters.createdAtTo || undefined,
      page: filters.page ?? 0,
      size: filters.size ?? 50,
    },
  });
  return response.data;
}

export async function createClient(payload: Partial<Client>): Promise<Client> {
  const response = await httpClient.post<{ data: Client }>("/api/v1/clients", payload);
  return response.data.data;
}

export async function updateClient(id: number, payload: Partial<Client>): Promise<Client> {
  const response = await httpClient.put<{ data: Client }>(`/api/v1/clients/${id}`, payload);
  return response.data.data;
}

export async function deleteClient(id: number): Promise<void> {
  await httpClient.delete(`/api/v1/clients/${id}`);
}
