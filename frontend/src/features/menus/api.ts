import { httpClient } from "../../api/http-client";
import type { ApiEnvelope, CreateMenuPayload, MenuItem, UpdateMenuPayload } from "./types";

export async function fetchMenus(): Promise<MenuItem[]> {
  const response = await httpClient.get<ApiEnvelope<MenuItem[]>>("/api/v1/menus");
  return response.data.data;
}

export async function createMenu(payload: CreateMenuPayload): Promise<MenuItem> {
  const response = await httpClient.post<ApiEnvelope<MenuItem>>("/api/v1/menus", payload);
  return response.data.data;
}

export async function updateMenu(menuPk: number, payload: UpdateMenuPayload): Promise<MenuItem> {
  const response = await httpClient.put<ApiEnvelope<MenuItem>>(`/api/v1/menus/${menuPk}`, payload);
  return response.data.data;
}

export async function deleteMenu(menuPk: number): Promise<void> {
  await httpClient.delete(`/api/v1/menus/${menuPk}`);
}
