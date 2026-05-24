package com.zhiyinhui.bosschat.ai.dto;

import java.math.BigDecimal;

public record AiAgentResponse(
        Long id,
        String agentCode,
        String agentName,
        String description,
        String systemPrompt,
        String modelProvider,
        String modelName,
        Long modelId,
        Long apiKeyId,
        BigDecimal temperature,
        Integer maxCompletionTokens,
        Integer memoryEnabled,
        Integer knowledgeEnabled,
        Integer workflowEnabled,
        Integer toolsEnabled,
        Integer imageGenerationEnabled,
        Long imageModelId,
        Long imageApiKeyId,
        String imageStorageStrategy,
        Long imageStorageConfigId,
        Integer enabled
) {
}
