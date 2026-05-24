package com.zhiyinhui.bosschat.ai.dto;

public record AiKnowledgeDocumentResponse(
        Long id,
        Long agentId,
        String title,
        String content,
        String tags,
        Integer enabled
) {
}
