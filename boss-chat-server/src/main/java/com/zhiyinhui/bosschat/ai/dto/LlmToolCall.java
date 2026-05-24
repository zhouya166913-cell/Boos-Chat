package com.zhiyinhui.bosschat.ai.dto;

public record LlmToolCall(
        String id,
        String name,
        String argumentsJson
) {
}
