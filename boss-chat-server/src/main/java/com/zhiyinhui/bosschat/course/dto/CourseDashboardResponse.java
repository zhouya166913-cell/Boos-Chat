package com.zhiyinhui.bosschat.course.dto;

import java.util.List;

public record CourseDashboardResponse(
        CoursePhaseResponse phase,
        long totalStudents,
        long newStudentCount,
        long existingStudentCount,
        long submittedCount,
        long completedCount,
        long failedCount,
        long missingSurveyCount,
        List<PainPointStat> painPoints,
        List<StudentPainSummary> studentPainSummaries,
        List<String> teachingIdeas
) {
    public record PainPointStat(String painPoint, long count) {
    }

    public record StudentPainSummary(
            String studentName,
            String phone,
            String status,
            List<String> painPoints,
            String finalSummary
    ) {
    }
}
