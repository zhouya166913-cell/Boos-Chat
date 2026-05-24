package com.zhiyinhui.bosschat.ai.dto;

import java.util.List;

public record AgentTaskResponse(
        String answer,
        List<AgentToolStepResponse> toolSteps,
        String provider,
        String model
) {
}
