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
  studentName: string;
  phone: string;
  idCard: string;
  isNewStudent: number;
  remark: string;
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
  phaseId: number;
  phaseName: string;
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

export function analyzeCoursePhase(phaseId: number) {
  return http.post<CourseAnalysis>(`/admin/course-phases/${phaseId}/course-analysis`).then((response) => response.data);
}
