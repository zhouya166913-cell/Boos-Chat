package com.zhiyinhui.bosschat.survey.dto;

import java.time.LocalDateTime;

public record SurveyListItemResponse(
        String publicId,
        Long phaseId,
        String phaseName,
        Long studentId,
        Integer isNewStudent,
        String customerName,
        String phone,
        String idCard,
        String company,
        String employeeCount,
        String annualRevenue,
        String status,
        LocalDateTime createTime
) {
}
