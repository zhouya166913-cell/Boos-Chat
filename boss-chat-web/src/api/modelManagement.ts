import { http } from "./http";

export interface ModelProvider {
  id: number;
  providerCode: string;
  providerName: string;
  baseUrl: string;
  authType: string;
  enabled: number;
  remark: string;
}

export interface ModelProviderPayload {
  providerCode: string;
  providerName: string;
  baseUrl: string;
  authType: string;
  enabled: number;
  remark: string;
}

export interface AiModelItem {
  id: number;
  providerId: number;
  providerName: string;
  providerCode: string;
  modelName: string;
  displayName: string;
  modelType: string;
  apiPath: string;
  billingType: string;
  officialDocUrl: string;
  compatibilityProfile: string;
  contextWindow: number;
  supportsStream: number;
  supportsTools: number;
  supportsVision: number;
  enabled: number;
  remark: string;
}

export interface AiModelPayload {
  providerId: number | null;
  modelName: string;
  displayName: string;
  modelType: string;
  apiPath: string;
  billingType: string;
  officialDocUrl: string;
  compatibilityProfile: string;
  contextWindow: number;
  supportsStream: number;
  supportsTools: number;
  supportsVision: number;
  enabled: number;
  remark: string;
}

export interface ModelApiKey {
  id: number;
  providerId: number;
  providerName: string;
  providerCode: string;
  modelId: number;
  modelName: string;
  modelDisplayName: string;
  modelType: string;
  keyName: string;
  keyType: string;
  apiKeyMask: string;
  priority: number;
  enabled: number;
  remark: string;
}

export interface ModelApiKeyPayload {
  providerId: number | null;
  modelId: number | null;
  keyName: string;
  keyType: string;
  apiKey: string;
  priority: number;
  enabled: number;
  remark: string;
}

const base = "/admin/model-management";

export function listModelProviders() {
  return http.get<ModelProvider[]>(`${base}/providers`).then((response) => response.data);
}

export function createModelProvider(payload: ModelProviderPayload) {
  return http.post<ModelProvider>(`${base}/providers`, payload).then((response) => response.data);
}

export function updateModelProvider(providerId: number, payload: ModelProviderPayload) {
  return http.put<ModelProvider>(`${base}/providers/${providerId}`, payload).then((response) => response.data);
}

export function listModels() {
  return http.get<AiModelItem[]>(`${base}/models`).then((response) => response.data);
}

export function createModel(payload: AiModelPayload) {
  return http.post<AiModelItem>(`${base}/models`, payload).then((response) => response.data);
}

export function updateModel(modelId: number, payload: AiModelPayload) {
  return http.put<AiModelItem>(`${base}/models/${modelId}`, payload).then((response) => response.data);
}

export function deleteModel(modelId: number) {
  return http.delete<void>(`${base}/models/${modelId}`).then((response) => response.data);
}

export function listModelApiKeys() {
  return http.get<ModelApiKey[]>(`${base}/api-keys`).then((response) => response.data);
}

export function createModelApiKey(payload: ModelApiKeyPayload) {
  return http.post<ModelApiKey>(`${base}/api-keys`, payload).then((response) => response.data);
}

export function updateModelApiKey(apiKeyId: number, payload: ModelApiKeyPayload) {
  return http.put<ModelApiKey>(`${base}/api-keys/${apiKeyId}`, payload).then((response) => response.data);
}

export function deleteModelApiKey(apiKeyId: number) {
  return http.delete<void>(`${base}/api-keys/${apiKeyId}`).then((response) => response.data);
}
