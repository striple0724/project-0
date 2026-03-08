import axios from "axios";
import { useSessionStore } from "../auth/session-store";

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL as string) ?? "http://localhost:19100";

export const httpClient = axios.create({
  baseURL: apiBaseUrl,
  timeout: 15000,
  withCredentials: true,
});

httpClient.interceptors.request.use((config) => {
  const token = useSessionStore.getState().tokens?.accessToken;
  if (token && token !== "SESSION") {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

httpClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      useSessionStore.getState().clearSession();
    }
    return Promise.reject(error);
  }
);

