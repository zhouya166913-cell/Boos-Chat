package com.zhiyinhui.bosschat.ai.dto;

import java.util.List;
import java.util.Map;

public record LlmToolResponse(
        String content,
        List<LlmToolCall> toolCalls,
        Map<String, Object> assistantMessage,
        String provider,
        String model
) {
}
