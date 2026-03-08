import { create } from "zustand";
import type { SessionActions, SessionState } from "./auth-types";

type SessionStore = SessionState & SessionActions;

const initialState: SessionState = {
  status: "refreshing",
  user: null,
  tokens: null,
};

export const useSessionStore = create<SessionStore>((set) => ({
  ...initialState,
  setAuthenticated: ({ user, tokens }) =>
    set({
      status: "authenticated",
      user,
      tokens,
    }),
  setRefreshing: () => set({ status: "refreshing" }),
  clearSession: () => set({ status: "anonymous", user: null, tokens: null }),
}));
