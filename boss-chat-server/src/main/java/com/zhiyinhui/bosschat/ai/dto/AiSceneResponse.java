package com.zhiyinhui.bosschat.ai.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AiSceneResponse(
        Long id,
        String sceneCode,
        String sceneName,
        String description,
        String chatMode,
        Integer enabled,
        LocalDateTime createTime,
        LocalDateTime updateTime,
        List<AiSceneAgentResponse> agents
) {
}
