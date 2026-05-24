package com.zhiyinhui.bosschat.ai.dto;

public record LlmChatResult(
        String content,
        String provider,
        String model,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens
) {
}
