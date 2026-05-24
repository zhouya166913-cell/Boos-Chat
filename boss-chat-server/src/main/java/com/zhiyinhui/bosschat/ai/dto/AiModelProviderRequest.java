package com.zhiyinhui.bosschat.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record AiModelProviderRequest(
        @NotBlank(message = "供应商编码不能为空") String providerCode,
        @NotBlank(message = "供应商名称不能为空") String providerName,
        String baseUrl,
        String authType,
        Integer enabled,
        String remark
) {
}
