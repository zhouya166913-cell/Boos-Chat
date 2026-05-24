import { http } from "./http";
import type { AgentToolApproval, AgentToolStep, WorkflowItem } from "./workbench";

export interface ChatMessage {
  role: "user" | "assistant";
  content: string;
  agentId?: number | null;
  agentName?: string;
  modelProvider?: string;
  modelName?: string;
}

export interface ChatResponse {
  conversationId: number;
  userMessage: ChatMessage;
  assistantMessage: ChatMessage;
}

export interface ChatAttachment {
  attachmentId: string;
  fileName: string;
  fileType: "image" | "excel" | "video" | "document" | "file";
  mimeType: string;
  size: number;
  url: string;
  localPath: string;
  summary: string;
}

export interface StreamMeta {
  conversationId: number;
  userMessage: ChatMessage;
}

export interface Conversation {
  id: number;
  sceneId?: number | null;
  chatMode: "single" | "team";
  agentId?: number | null;
  title: string;
  createTime: string;
  updateTime: string;
}

export interface ConversationDetail {
  conversation: Conversation;
  messages: ChatMessage[];
}

export interface ChatSceneAgent {
  id: number;
  sceneId: number;
  agentId: number;
  roleName: string;
  sortOrder: number;
  enabled: number;
  agentCode: string;
  agentName: string;
  description: string;
  toolsEnabled: number;
  imageGenerationEnabled: number;
}

export interface ChatScene {
  id: number;
  sceneCode: string;
  sceneName: string;
  description: string;
  chatMode: "single" | "team";
  enabled: number;
  createTime: string;
  updateTime: string;
  agents: ChatSceneAgent[];
}

export function listChatScenes() {
  return http.get<ChatScene[]>("/chat/scenes").then((response) => response.data);
}

export function listChatWorkflows(agentId: number) {
  return http
    .get<WorkflowItem[]>("/chat/workflows", { params: { agentId } })
    .then((response) => response.data);
}

export function sendChatMessage(payload: {
  sceneId: number;
  agentId: number;
  conversationId?: number;
  content: string;
  attachments?: ChatAttachment[];
  workflowCode?: string;
}) {
  return http
    .post<ChatResponse>("/chat/messages", payload, { timeout: 120000 })
    .then((response) => response.data);
}

export async function streamChatMessage(
  payload: {
    sceneId: number;
    agentId: number;
    conversationId?: number;
    content: string;
    attachments?: ChatAttachment[];
    workflowCode?: string;
  },
  handlers: {
    onMeta: (meta: StreamMeta) => void;
    onDelta: (content: string) => void;
    onStep?: (step: AgentToolStep) => void;
    onApprovalRequired?: (approval: AgentToolApproval) => void;
    onDone: (response: ChatResponse) => void;
  },
  signal?: AbortSignal
) {
  const token = localStorage.getItem("boss-chat-token");
  const response = await fetch(`${import.meta.env.VITE_API_BASE_URL || "/api"}/chat/messages/stream`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { satoken: token } : {})
    },
    body: JSON.stringify(payload),
    signal
  });

  if (!response.ok || !response.body) {
    throw new Error("请求失败");
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
      if (event.name === "meta") handlers.onMeta(JSON.parse(event.data));
      if (event.name === "delta") handlers.onDelta(JSON.parse(event.data).content);
      if (event.name === "step") handlers.onStep?.(JSON.parse(event.data));
      if (event.name === "approval_required") handlers.onApprovalRequired?.(JSON.parse(event.data));
      if (event.name === "done") handlers.onDone(JSON.parse(event.data));
      if (event.name === "error") throw new Error(parseSseData(event.data));
    }
  }
}

export function uploadChatAttachment(file: File) {
  const formData = new FormData();
  formData.append("file", file);
  return http
    .post<ChatAttachment>("/chat/attachments", formData, {
      timeout: 120000,
      headers: { "Content-Type": "multipart/form-data" }
    })
    .then((response) => response.data);
}

export function approveChatToolExecution(approvalId: string) {
  return http.post(`/chat/approvals/${approvalId}/approve`);
}

export function rejectChatToolExecution(approvalId: string) {
  return http.post(`/chat/approvals/${approvalId}/reject`);
}

export function listConversations(params: { sceneId?: number; chatMode?: "single" | "team"; agentId?: number }) {
  return http
    .get<Conversation[]>("/chat/conversations", { params })
    .then((response) => response.data);
}

export function getConversationDetail(conversationId: number) {
  return http
    .get<ConversationDetail>(`/chat/conversations/${conversationId}`)
    .then((response) => response.data);
}

export function clearConversationContext(conversationId: number) {
  return http.post<void>(`/chat/conversations/${conversationId}/clear-context`).then((response) => response.data);
}

export function renameConversationTitle(conversationId: number, title: string) {
  return http
    .put<void>(`/chat/conversations/${conversationId}/title`, { title })
    .then((response) => response.data);
}

export function deleteConversation(conversationId: number) {
  return http.delete<void>(`/chat/conversations/${conversationId}`).then((response) => response.data);
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

function parseSseData(data: string) {
  try {
    return JSON.parse(data);
  } catch {
    return data;
  }
}
