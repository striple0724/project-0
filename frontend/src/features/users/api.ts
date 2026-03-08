import { httpClient } from "../../api/http-client";
import type { ApiEnvelope, CreateUserPayload, UpdateUserPayload, UserAccount } from "./types";

export async function fetchUsers(): Promise<UserAccount[]> {
  const response = await httpClient.get<ApiEnvelope<UserAccount[]>>("/api/v1/users");
  return response.data.data;
}

export async function createUser(payload: CreateUserPayload): Promise<UserAccount> {
  const response = await httpClient.post<ApiEnvelope<UserAccount>>("/api/v1/users", payload);
  return response.data.data;
}

export async function updateUser(seq: number, payload: UpdateUserPayload): Promise<UserAccount> {
  const response = await httpClient.put<ApiEnvelope<UserAccount>>(`/api/v1/users/${seq}`, payload);
  return response.data.data;
}

export async function deleteUser(seq: number): Promise<void> {
  await httpClient.delete(`/api/v1/users/${seq}`);
}
