package com.zhiyinhui.bosschat.ai.dto;

public record AiWorkflowResponse(
        Long id,
        Long agentId,
        String workflowCode,
        String workflowName,
        String description,
        String definitionJson,
        Integer enabled
) {
}
