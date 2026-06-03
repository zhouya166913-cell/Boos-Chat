package com.zhiyinhui.bosschat.course.dto;

import java.time.LocalDateTime;

public record CourseAnalysisResponse(
        Long phaseId,
        String phaseName,
        String content,
        LocalDateTime generatedAt
) {
}
