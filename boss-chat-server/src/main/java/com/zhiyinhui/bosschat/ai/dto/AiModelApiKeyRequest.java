package com.zhiyinhui.bosschat.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AiModelApiKeyRequest(
        @NotNull(message = "供应商不能为空") Long providerId,
        @NotNull(message = "模型不能为空") Long modelId,
        @NotBlank(message = "Key 名称不能为空") String keyName,
        String keyType,
        String apiKey,
        Integer priority,
        Integer enabled,
        String remark
) {
}
