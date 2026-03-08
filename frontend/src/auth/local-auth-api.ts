import { httpClient } from "../api/http-client";
import type { SessionUser } from "./auth-types";

export interface AuthMeResponse {
  data: {
    seq: number;
    userId: string;
    name: string;
    useYn: string;
  };
}

export async function localLogin(userId: string, password: string): Promise<AuthMeResponse> {
  const response = await httpClient.post<AuthMeResponse>("/api/v1/auth/login", { userId, password });
  return response.data;
}

export async function fetchMe(): Promise<AuthMeResponse> {
  const response = await httpClient.get<AuthMeResponse>("/api/v1/auth/me");
  return response.data;
}

export async function localLogout(): Promise<void> {
  await httpClient.post("/api/v1/auth/logout");
}

export function toSessionUser(payload: AuthMeResponse["data"]): SessionUser {
  return {
    sub: String(payload.seq),
    name: payload.name,
    email: undefined,
    roles: ["USER"],
  };
}
