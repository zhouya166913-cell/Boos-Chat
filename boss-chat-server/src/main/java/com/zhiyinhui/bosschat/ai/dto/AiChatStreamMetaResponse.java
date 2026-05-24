package com.zhiyinhui.bosschat.ai.dto;

public record AiChatStreamMetaResponse(
        Long conversationId,
        AiChatMessageResponse userMessage
) {
}
