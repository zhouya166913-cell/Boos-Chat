package com.zhiyinhui.bosschat.survey.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record SurveyRecordResponse(
        String publicId,
        String customerName,
        String phone,
        String company,
        String employeeCount,
        String annualRevenue,
        Map<String, Object> answers,
        String analyzerResult,
        String plannerPrompt,
        String finalReport,
        String status,
        String errorMessage,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
