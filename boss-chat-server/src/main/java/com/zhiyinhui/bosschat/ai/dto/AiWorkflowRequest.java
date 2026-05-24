package com.zhiyinhui.bosschat.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record AiWorkflowRequest(
        @NotBlank(message = "工作流编码不能为空") String workflowCode,
        @NotBlank(message = "工作流名称不能为空") String workflowName,
        String description,
        @NotBlank(message = "工作流定义不能为空") String definitionJson,
        Integer enabled
) {
}
