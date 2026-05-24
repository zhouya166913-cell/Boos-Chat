import type { CurrentUser, LoginResponse } from "../types/auth";
import { request } from "../utils/request";

export function login(username: string, password: string) {
  return request<LoginResponse>({
    url: "/auth/login",
    method: "POST",
    data: { username, password }
  });
}

export function me() {
  return request<CurrentUser>({
    url: "/auth/me"
  });
}

export function logout() {
  return request<void>({
    url: "/auth/logout",
    method: "POST"
  });
}