package com.zhiyinhui.bosschat.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AiModelRequest(
        @NotNull(message = "供应商不能为空") Long providerId,
        @NotBlank(message = "模型名称不能为空") String modelName,
        String displayName,
        String modelType,
        String apiPath,
        String billingType,
        String officialDocUrl,
        String compatibilityProfile,
        Integer contextWindow,
        Integer supportsStream,
        Integer supportsTools,
        Integer supportsVision,
        Integer enabled,
        String remark
) {
}
