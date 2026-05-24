package com.zhiyinhui.bosschat.ai.dto;

public record AiModelApiKeyResponse(
        Long id,
        Long providerId,
        String providerName,
        String providerCode,
        Long modelId,
        String modelName,
        String modelDisplayName,
        String modelType,
        String keyName,
        String keyType,
        String apiKeyMask,
        Integer priority,
        Integer enabled,
        String remark
) {
}
