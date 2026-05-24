package com.zhiyinhui.bosschat.ai.dto;

import java.util.List;

public record AiConversationDetailResponse(
        AiConversationResponse conversation,
        List<AiChatMessageResponse> messages
) {
}
