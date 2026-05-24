package com.zhiyinhui.bosschat.survey.dto;

import java.time.LocalDateTime;

public record SurveyListItemResponse(
        String publicId,
        String customerName,
        String phone,
        String company,
        String employeeCount,
        String annualRevenue,
        String status,
        LocalDateTime createTime
) {
}
