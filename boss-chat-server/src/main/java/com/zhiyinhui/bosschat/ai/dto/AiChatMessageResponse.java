package com.zhiyinhui.bosschat.ai.dto;

public record AiChatMessageResponse(
        String role,
        String content,
        Long agentId,
        String agentName,
        String modelProvider,
        String modelName
) {
    public AiChatMessageResponse(String role, String content) {
        this(role, content, null, "", "", "");
    }
}
