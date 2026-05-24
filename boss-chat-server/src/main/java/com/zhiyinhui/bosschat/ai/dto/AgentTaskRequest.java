package com.zhiyinhui.bosschat.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AgentTaskRequest(
        @NotNull(message = "AI不能为空") Long agentId,
        Long conversationId,
        @NotBlank(message = "任务内容不能为空") String prompt,
        String workflowCode,
        List<AgentTaskContextMessage> contextMessages
) {
}
