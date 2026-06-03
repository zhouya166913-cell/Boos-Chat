package com.zhiyinhui.bosschat.course.dto;

import java.time.LocalDateTime;

public record CourseAnalysisResponse(
        Long id,
        Long phaseId,
        String phaseName,
        Long agentId,
        String agentName,
        String content,
        LocalDateTime generatedAt
) {
}
