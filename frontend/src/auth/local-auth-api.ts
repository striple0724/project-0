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
  const response = await httpClient.get<AuthMeResponse>("/api/v1/auth/me", { timeout: 5000 });
  return response.data;
}

export async function localLogout(): Promise<void> {
  await httpClient.post("/api/v1/auth/logout");
}

export function toSessionUser(payload: AuthMeResponse["data"]): SessionUser {
  const isAdmin = payload.userId?.toLowerCase() === "admin";
  return {
    sub: String(payload.seq),
    userId: payload.userId,
    name: payload.name,
    email: undefined,
    roles: isAdmin ? ["ADMIN", "USER"] : ["USER"],
  };
}
