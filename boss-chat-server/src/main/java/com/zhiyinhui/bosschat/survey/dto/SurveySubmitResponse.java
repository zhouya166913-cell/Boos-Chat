package com.zhiyinhui.bosschat.survey.dto;

public record SurveySubmitResponse(
        String publicId,
        String status,
        String finalReport
) {
}
