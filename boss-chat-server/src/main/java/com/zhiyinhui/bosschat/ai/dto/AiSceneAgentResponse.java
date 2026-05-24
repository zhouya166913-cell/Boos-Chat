package com.zhiyinhui.bosschat.ai.dto;

public record AiSceneAgentResponse(
        Long id,
        Long sceneId,
        Long agentId,
        String roleName,
        Integer sortOrder,
        Integer enabled,
        String agentCode,
        String agentName,
        String description,
        Integer toolsEnabled,
        Integer imageGenerationEnabled
) {
}
