package com.zhiyinhui.bosschat.ai.dto;

public record AiModelProviderResponse(
        Long id,
        String providerCode,
        String providerName,
        String baseUrl,
        String authType,
        Integer enabled,
        String remark
) {
}
