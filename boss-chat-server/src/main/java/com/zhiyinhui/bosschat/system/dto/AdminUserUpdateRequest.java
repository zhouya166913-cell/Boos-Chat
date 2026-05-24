package com.zhiyinhui.bosschat.system.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminUserUpdateRequest(
        @NotBlank(message = "显示名不能为空") String displayName,
        String password,
        String mobile,
        String wechatNo,
        String qqNo,
        Integer status,
        String roleCode
) {
}
