import { http } from "./http";

export interface LoginPayload {
  username: string;
  password: string;
}

export interface CurrentUser {
  id: number;
  username: string;
  displayName: string;
  role: string;
}

export interface LoginResponse {
  token: string;
  user: CurrentUser;
}

export async function login(payload: LoginPayload) {
  const { data } = await http.post<LoginResponse>("/auth/login", payload);
  return data;
}

export async function me() {
  const { data } = await http.get<CurrentUser>("/auth/me");
  return data;
}

export async function logout() {
  await http.post("/auth/logout");
}
