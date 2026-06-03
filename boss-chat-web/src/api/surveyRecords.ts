import { http } from "./http";

export interface SurveyRecordListItem {
  publicId: string;
  phaseId?: number;
  phaseName?: string;
  studentId?: number;
  isNewStudent?: number;
  customerName: string;
  phone: string;
  idCard: string;
  company: string;
  employeeCount: string;
  annualRevenue: string;
  status: string;
  createTime?: string;
}

export interface SurveyRecordDetail extends SurveyRecordListItem {
  answers: Record<string, unknown>;
  analyzerResult?: string;
  plannerPrompt?: string;
  finalReport?: string;
  errorMessage?: string;
  updateTime?: string;
}

export function listSurveyRecords(phaseId?: number) {
  return http.get<SurveyRecordListItem[]>("/survey-records", {
    params: phaseId ? { phaseId } : undefined
  }).then((response) => response.data);
}

export function getSurveyRecord(publicId: string) {
  return http.get<SurveyRecordDetail>(`/survey-records/${publicId}`).then((response) => response.data);
}
