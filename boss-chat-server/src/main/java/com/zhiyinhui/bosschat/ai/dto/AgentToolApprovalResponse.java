package com.zhiyinhui.bosschat.ai.dto;

public record AgentToolApprovalResponse(
        String approvalId,
        String toolName,
        String argumentsJson,
        String message
) {
}
