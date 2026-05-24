package com.zhiyinhui.bosschat.ai.dto;

public record AgentToolStepResponse(
        String toolName,
        String argumentsJson,
        String resultSummary,
        String status,
        String approvalId
) {
    public AgentToolStepResponse(String toolName, String argumentsJson, String resultSummary, String status) {
        this(toolName, argumentsJson, resultSummary, status, null);
    }
}
