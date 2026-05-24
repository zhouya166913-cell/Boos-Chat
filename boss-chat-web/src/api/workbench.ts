import { http } from "./http";

export interface AgentToolStep {
  toolName: string;
  argumentsJson: string;
  resultSummary: string;
  status: string;
  approvalId?: string;
}

export interface AgentTaskResponse {
  answer: string;
  toolSteps: AgentToolStep[];
}

export interface AgentContextMessage {
  role: "user" | "assistant";
  content: string;
}

export interface AgentToolApproval {
  approvalId: string;
  toolName: string;
  argumentsJson: string;
  message: string;
}

export interface MemoryItem {
  id: number;
  agentId: number;
  memoryType: string;
  memoryKey: string;
  memoryValue: string;
  enabled: number;
}

export interface KnowledgeDocument {
  id: number;
  agentId: number;
  title: string;
  content: string;
  tags: string;
  enabled: number;
}

export interface WorkflowItem {
  id: number;
  agentId: number;
  workflowCode: string;
  workflowName: string;
  description: string;
  definitionJson: string;
  enabled: number;
}

export function runAgentTask(payload: {
  agentId: number;
  prompt: string;
  workflowCode?: string;
  contextMessages?: AgentContextMessage[];
}) {
  return http
    .post<AgentTaskResponse>("/agent-workbench/tasks", payload, { timeout: 180000 })
    .then((response) => response.data);
}

export async function streamAgentTask(
  payload: {
    agentId: number;
    prompt: string;
    workflowCode?: string;
    contextMessages?: AgentContextMessage[];
  },
  handlers: {
    onStart?: (message: string) => void;
    onStep: (step: AgentToolStep) => void;
    onApprovalRequired?: (approval: AgentToolApproval) => void;
    onDone: (response: AgentTaskResponse) => void;
    onError?: (message: string) => void;
  },
  signal?: AbortSignal
) {
  const token = localStorage.getItem("boss-chat-token");
  const response = await fetch(`${import.meta.env.VITE_API_BASE_URL || "/api"}/agent-workbench/tasks/stream`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { satoken: token } : {})
    },
    body: JSON.stringify(payload),
    signal
  });

  if (!response.ok || !response.body) {
    throw new Error("智能体任务请求失败");
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });

    const chunks = buffer.split("\n\n");
    buffer = chunks.pop() || "";

    for (const chunk of chunks) {
      const event = parseSseChunk(chunk);
      if (!event) continue;
      if (event.name === "start") handlers.onStart?.(event.data);
      if (event.name === "step") handlers.onStep(JSON.parse(event.data));
      if (event.name === "approval_required") handlers.onApprovalRequired?.(JSON.parse(event.data));
      if (event.name === "done") handlers.onDone(JSON.parse(event.data));
      if (event.name === "error") {
        handlers.onError?.(event.data);
        throw new Error(event.data);
      }
    }
  }
}

export function approveToolExecution(approvalId: string) {
  return http.post(`/agent-workbench/approvals/${approvalId}/approve`);
}

export function rejectToolExecution(approvalId: string) {
  return http.post(`/agent-workbench/approvals/${approvalId}/reject`);
}

function parseSseChunk(chunk: string) {
  const lines = chunk.split("\n");
  const eventName = lines.find((line) => line.startsWith("event:"))?.slice("event:".length).trim();
  const data = lines
    .filter((line) => line.startsWith("data:"))
    .map((line) => line.slice("data:".length).trim())
    .join("\n");
  if (!eventName || !data) return undefined;
  return { name: eventName, data };
}

export function listMemories(agentId: number) {
  return http.get<MemoryItem[]>(`/admin/agents/${agentId}/memories`).then((response) => response.data);
}

export function saveMemory(
  agentId: number,
  payload: {
    memoryKey: string;
    memoryValue: string;
    memoryType?: string;
    enabled?: number;
  }
) {
  return http.post<MemoryItem>(`/admin/agents/${agentId}/memories`, payload).then((response) => response.data);
}

export function updateMemory(
  agentId: number,
  memoryId: number,
  payload: {
    memoryKey: string;
    memoryValue: string;
    memoryType?: string;
    enabled?: number;
  }
) {
  return http.put<MemoryItem>(`/admin/agents/${agentId}/memories/${memoryId}`, payload).then((response) => response.data);
}

export function deleteMemory(agentId: number, memoryId: number) {
  return http.delete(`/admin/agents/${agentId}/memories/${memoryId}`);
}

export function listKnowledgeDocuments(agentId: number) {
  return http
    .get<KnowledgeDocument[]>(`/admin/agents/${agentId}/knowledge-documents`)
    .then((response) => response.data);
}

export function createKnowledgeDocument(
  agentId: number,
  payload: {
    title: string;
    content: string;
    tags?: string;
    enabled?: number;
  }
) {
  return http
    .post<KnowledgeDocument>(`/admin/agents/${agentId}/knowledge-documents`, payload)
    .then((response) => response.data);
}

export function updateKnowledgeDocument(
  agentId: number,
  documentId: number,
  payload: {
    title: string;
    content: string;
    tags?: string;
    enabled?: number;
  }
) {
  return http
    .put<KnowledgeDocument>(`/admin/agents/${agentId}/knowledge-documents/${documentId}`, payload)
    .then((response) => response.data);
}

export function deleteKnowledgeDocument(agentId: number, documentId: number) {
  return http.delete(`/admin/agents/${agentId}/knowledge-documents/${documentId}`);
}

export function listWorkflows(agentId: number) {
  return http.get<WorkflowItem[]>(`/admin/agents/${agentId}/workflows`).then((response) => response.data);
}

export function createWorkflow(
  agentId: number,
  payload: {
    workflowCode: string;
    workflowName: string;
    description?: string;
    definitionJson: string;
    enabled?: number;
  }
) {
  return http.post<WorkflowItem>(`/admin/agents/${agentId}/workflows`, payload).then((response) => response.data);
}

export function updateWorkflow(
  agentId: number,
  workflowId: number,
  payload: {
    workflowCode: string;
    workflowName: string;
    description?: string;
    definitionJson: string;
    enabled?: number;
  }
) {
  return http
    .put<WorkflowItem>(`/admin/agents/${agentId}/workflows/${workflowId}`, payload)
    .then((response) => response.data);
}

export function deleteWorkflow(agentId: number, workflowId: number) {
  return http.delete(`/admin/agents/${agentId}/workflows/${workflowId}`);
}
