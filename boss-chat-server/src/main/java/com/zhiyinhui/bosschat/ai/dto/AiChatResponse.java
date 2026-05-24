package com.zhiyinhui.bosschat.ai.dto;

public record AiChatResponse(
        Long conversationId,
        AiChatMessageResponse userMessage,
        AiChatMessageResponse assistantMessage
) {
}
