package com.zhiyinhui.bosschat.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record AiMemoryRequest(
        @NotBlank(message = "记忆键不能为空") String memoryKey,
        @NotBlank(message = "记忆内容不能为空") String memoryValue,
        String memoryType,
        Integer enabled
) {
}
