package com.zhiyinhui.bosschat.ai.dto;

public record AiModelResponse(
        Long id,
        Long providerId,
        String providerName,
        String providerCode,
        String modelName,
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
