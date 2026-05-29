import { http } from "./http";

export interface ImageStorageConfig {
  id: number;
  storageCode: string;
  storageName: string;
  storageType: string;
  endpoint: string;
  region: string;
  bucketName: string;
  baseUrl: string;
  rootPath: string;
  extraConfigJson: string;
  accessKeyId: string;
  accessKeySecret: string;
  accessKeyIdMask: string;
  accessKeySecretMask: string;
  enabled: number;
  isDefault: number;
  remark: string;
}

export interface ImageStoragePayload {
  storageCode: string;
  storageName: string;
  storageType: string;
  endpoint: string;
  region: string;
  bucketName: string;
  baseUrl: string;
  rootPath: string;
  extraConfigJson: string;
  accessKeyId: string;
  accessKeySecret: string;
  enabled: number;
  isDefault: number;
  remark: string;
}

export interface ImageStorageValidationResult {
  success: boolean;
  message: string;
  objectUrl: string;
}

const base = "/admin/image-storage";

export function listImageStorageConfigs() {
  return http.get<ImageStorageConfig[]>(base).then((response) => response.data);
}

export function createImageStorageConfig(payload: ImageStoragePayload) {
  return http.post<ImageStorageConfig>(base, payload).then((response) => response.data);
}

export function updateImageStorageConfig(storageId: number, payload: ImageStoragePayload) {
  return http.put<ImageStorageConfig>(`${base}/${storageId}`, payload).then((response) => response.data);
}

export function validateImageStorageConfig(payload: ImageStoragePayload, storageId?: number) {
  const url = storageId ? `${base}/${storageId}/validate` : `${base}/validate`;
  return http.post<ImageStorageValidationResult>(url, payload).then((response) => response.data);
}
