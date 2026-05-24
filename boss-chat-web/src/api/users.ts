import { http } from "./http";

export interface AdminUser {
  id: number;
  username: string;
  displayName: string;
  mobile: string;
  wechatNo: string;
  qqNo: string;
  roleCode: string;
  roleName: string;
  status: number;
  lastLoginTime?: string;
  createTime?: string;
}

export function listUsers() {
  return http.get<AdminUser[]>("/admin/users").then((response) => response.data);
}
