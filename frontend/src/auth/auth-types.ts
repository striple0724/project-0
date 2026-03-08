export type SessionStatus = "anonymous" | "authenticated" | "refreshing";

export interface SessionUser {
  sub: string;
  name?: string;
  email?: string;
  roles: string[];
}

export interface SessionTokens {
  accessToken: string;
  expiresAt: number;
}

export interface SessionState {
  status: SessionStatus;
  user: SessionUser | null;
  tokens: SessionTokens | null;
}

export interface SessionActions {
  setAuthenticated: (payload: { user: SessionUser; tokens: SessionTokens }) => void;
  setRefreshing: () => void;
  clearSession: () => void;
}
