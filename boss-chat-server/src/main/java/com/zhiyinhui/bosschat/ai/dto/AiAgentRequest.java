package com.zhiyinhui.bosschat.ai.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record AiAgentRequest(
        @NotBlank(message = "智能体编码不能为空") String agentCode,
        @NotBlank(message = "智能体名称不能为空") String agentName,
        String description,
        @NotBlank(message = "系统提示词不能为空") String systemPrompt,
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
