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
