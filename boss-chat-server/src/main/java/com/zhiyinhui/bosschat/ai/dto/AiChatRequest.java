package com.zhiyinhui.bosschat.ai.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AiChatRequest(
        @NotNull(message = "请选择场景") Long sceneId,
        @NotNull(message = "请选择 AI") Long agentId,
        Long conversationId,
        String content,
        List<AiChatAttachmentRequest> attachments,
        String workflowCode
) {
}
