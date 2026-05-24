import { http } from "./http";

export interface Agent {
  id: number;
  agentCode: string;
  agentName: string;
  description: string;
  systemPrompt: string;
  modelProvider: string;
  modelName: string;
  modelId?: number | null;
  apiKeyId?: number | null;
  temperature: number;
  maxCompletionTokens: number;
  memoryEnabled: number;
  knowledgeEnabled: number;
  workflowEnabled: number;
  toolsEnabled: number;
  imageGenerationEnabled: number;
  imageModelId?: number | null;
  imageApiKeyId?: number | null;
  imageStorageStrategy: string;
  imageStorageConfigId?: number | null;
  enabled: number;
}

export interface AgentPayload {
  agentCode: string;
  agentName: string;
  description: string;
  systemPrompt: string;
  modelProvider: string;
  modelName: string;
  modelId?: number | null;
  apiKeyId?: number | null;
  temperature: number;
  maxCompletionTokens: number;
  memoryEnabled: number;
  knowledgeEnabled: number;
  workflowEnabled: number;
  toolsEnabled: number;
  imageGenerationEnabled: number;
  imageModelId?: number | null;
  imageApiKeyId?: number | null;
  imageStorageStrategy: string;
  imageStorageConfigId?: number | null;
  enabled: number;
}

export function listAdminAgents() {
  return http.get<Agent[]>("/admin/agents").then((response) => response.data);
}

export function createAgent(payload: AgentPayload) {
  return http.post<Agent>("/admin/agents", payload).then((response) => response.data);
}

export function updateAgent(agentId: number, payload: AgentPayload) {
  return http.put<Agent>(`/admin/agents/${agentId}`, payload).then((response) => response.data);
}
