package com.zhiyinhui.bosschat.ai.dto;

import java.time.LocalDateTime;

public record AiConversationResponse(
        Long id,
        Long sceneId,
        String chatMode,
        Long agentId,
        String title,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
