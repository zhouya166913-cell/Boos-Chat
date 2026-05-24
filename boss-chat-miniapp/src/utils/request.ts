import { API_BASE_URL } from "../config/api";

interface RequestOptions {
  url: string;
  method?: UniApp.RequestOptions["method"];
  data?: UniApp.RequestOptions["data"];
}

export function request<TResponse>(options: RequestOptions): Promise<TResponse> {
  const token = uni.getStorageSync("boss-chat-miniapp-token");

  return new Promise((resolve, reject) => {
    uni.request({
      url: `${API_BASE_URL}${options.url}`,
      method: options.method || "GET",
      data: options.data,
      header: token ? { satoken: token } : {},
      success: (response) => {
        if (response.statusCode >= 200 && response.statusCode < 300) {
          resolve(response.data as TResponse);
          return;
        }
        reject(response.data || { message: "请求失败" });
      },
      fail: reject
    });
  });
}