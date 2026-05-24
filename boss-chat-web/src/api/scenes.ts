import { http } from "./http";
import type { ChatScene } from "./chat";

export interface ScenePayload {
  sceneCode?: string;
  sceneName: string;
  description: string;
  chatMode: "single" | "team";
  agentIds: number[];
  enabled: number;
}

export function listAdminScenes() {
  return http.get<ChatScene[]>("/admin/scenes").then((response) => response.data);
}

export function createScene(payload: ScenePayload) {
  return http.post<ChatScene>("/admin/scenes", payload).then((response) => response.data);
}

export function updateScene(sceneId: number, payload: ScenePayload) {
  return http.put<ChatScene>(`/admin/scenes/${sceneId}`, payload).then((response) => response.data);
}
