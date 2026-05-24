package com.zhiyinhui.bosschat.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record AiKnowledgeDocumentRequest(
        @NotBlank(message = "知识标题不能为空") String title,
        @NotBlank(message = "知识内容不能为空") String content,
        String tags,
        Integer enabled
) {
}
