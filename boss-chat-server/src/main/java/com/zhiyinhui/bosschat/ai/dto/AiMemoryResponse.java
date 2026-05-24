package com.zhiyinhui.bosschat.ai.dto;

public record AiMemoryResponse(
        Long id,
        Long agentId,
        String memoryType,
        String memoryKey,
        String memoryValue,
        Integer enabled
) {
}
