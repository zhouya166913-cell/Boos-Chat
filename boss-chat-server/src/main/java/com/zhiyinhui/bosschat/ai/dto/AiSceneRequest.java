package com.zhiyinhui.bosschat.ai.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record AiSceneRequest(
        String sceneCode,
        @NotBlank(message = "场景名称不能为空") String sceneName,
        String description,
        String chatMode,
        List<Long> agentIds,
        Integer enabled
) {
}
