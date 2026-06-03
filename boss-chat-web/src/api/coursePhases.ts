import { http } from "./http";

export interface CoursePhase {
  id: number;
  phaseCode: string;
  phaseName: string;
  courseName: string;
  surveyPath: string;
  qrImageUrl: string;
  enabled: number;
  remark: string;
  studentCount: number;
  surveyRecordCount: number;
  createTime?: string;
  updateTime?: string;
}

export interface CourseStudent {
  id: number;
  phaseId: number;
  groupId?: number | null;
  groupName: string;
  leaderName: string;
  studentNo: string;
  studentName: string;
  phone: string;
  idCard: string;
  isNewStudent: number;
  checkInCount: number;
  lastCheckInTime?: string;
  remark: string;
  createTime?: string;
  updateTime?: string;
}

export interface CourseGroup {
  id: number;
  phaseId: number;
  groupName: string;
  leaderName: string;
  remark: string;
  sortOrder: number;
  studentCount: number;
  createTime?: string;
  updateTime?: string;
}

export interface CourseDashboard {
  phase: CoursePhase;
  totalStudents: number;
  newStudentCount: number;
  existingStudentCount: number;
  submittedCount: number;
  completedCount: number;
  failedCount: number;
  missingSurveyCount: number;
  painPoints: Array<{ painPoint: string; count: number }>;
  studentPainSummaries: Array<{
    studentName: string;
    phone: string;
    status: string;
    painPoints: string[];
    finalSummary: string;
  }>;
  teachingIdeas: string[];
}

export interface CourseAnalysis {
  id: number;
  phaseId: number;
  phaseName: string;
  agentId?: number | null;
  agentName: string;
  content: string;
  generatedAt?: string;
}

export interface CoursePhasePayload {
  phaseName: string;
  courseName: string;
  qrImageUrl?: string;
  enabled: number;
  remark?: string;
}

export interface CourseStudentPayload {
  groupId?: number | null;
  studentNo: string;
  studentName: string;
  phone: string;
  idCard: string;
  isNewStudent: number;
  remark?: string;
}

export function listCoursePhases() {
  return http.get<CoursePhase[]>("/admin/course-phases").then((response) => response.data);
}

export function createCoursePhase(payload: CoursePhasePayload) {
  return http.post<CoursePhase>("/admin/course-phases", payload).then((response) => response.data);
}

export function updateCoursePhase(phaseId: number, payload: CoursePhasePayload) {
  return http.put<CoursePhase>(`/admin/course-phases/${phaseId}`, payload).then((response) => response.data);
}

export interface CourseGroupPayload {
  groupName: string;
  leaderName: string;
  remark?: string;
  sortOrder?: number;
}

export function listCourseGroups(phaseId: number) {
  return http.get<CourseGroup[]>(`/admin/course-phases/${phaseId}/groups`).then((response) => response.data);
}

export function createCourseGroup(phaseId: number, payload: CourseGroupPayload) {
  return http.post<CourseGroup>(`/admin/course-phases/${phaseId}/groups`, payload).then((response) => response.data);
}

export function updateCourseGroup(phaseId: number, groupId: number, payload: CourseGroupPayload) {
  return http.put<CourseGroup>(`/admin/course-phases/${phaseId}/groups/${groupId}`, payload).then((response) => response.data);
}

export function deleteCourseGroup(phaseId: number, groupId: number) {
  return http.delete<void>(`/admin/course-phases/${phaseId}/groups/${groupId}`).then((response) => response.data);
}

export function listCourseStudents(phaseId: number) {
  return http.get<CourseStudent[]>(`/admin/course-phases/${phaseId}/students`).then((response) => response.data);
}

export function createCourseStudent(phaseId: number, payload: CourseStudentPayload) {
  return http.post<CourseStudent>(`/admin/course-phases/${phaseId}/students`, payload).then((response) => response.data);
}

export function updateCourseStudent(phaseId: number, studentId: number, payload: CourseStudentPayload) {
  return http.put<CourseStudent>(`/admin/course-phases/${phaseId}/students/${studentId}`, payload).then((response) => response.data);
}

export function deleteCourseStudent(phaseId: number, studentId: number) {
  return http.delete<void>(`/admin/course-phases/${phaseId}/students/${studentId}`).then((response) => response.data);
}

export function getCourseDashboard(phaseId: number) {
  return http.get<CourseDashboard>(`/admin/course-phases/${phaseId}/dashboard`).then((response) => response.data);
}

export function analyzeCoursePhase(phaseId: number, agentId?: number | null) {
  return http.post<CourseAnalysis>(`/admin/course-phases/${phaseId}/course-analysis`, {
    agentId: agentId ?? null
  }).then((response) => response.data);
}

export async function streamCourseAnalysis(
  phaseId: number,
  agentId: number | null | undefined,
  handlers: {
    onDelta: (content: string) => void;
    onDone: (response: CourseAnalysis) => void;
  },
  signal?: AbortSignal
) {
  const token = localStorage.getItem("boss-chat-token");
  const response = await fetch(`${import.meta.env.VITE_API_BASE_URL || "/api"}/admin/course-phases/${phaseId}/course-analysis/stream`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { satoken: token } : {})
    },
    body: JSON.stringify({ agentId: agentId ?? null }),
    signal
  });

  if (!response.ok || !response.body) {
    throw new Error("课程分析请求失败");
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
      if (event.name === "delta") handlers.onDelta(JSON.parse(event.data).content || "");
      if (event.name === "done") handlers.onDone(JSON.parse(event.data));
      if (event.name === "error") throw new Error(parseSseData(event.data));
    }
  }
}

export function listCourseAnalyses(phaseId: number) {
  return http.get<CourseAnalysis[]>(`/admin/course-phases/${phaseId}/course-analyses`).then((response) => response.data);
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
