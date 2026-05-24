package com.zhiyinhui.bosschat.system.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminUserCreateRequest(
        @NotBlank(message = "账号不能为空") String username,
        @NotBlank(message = "密码不能为空") String password,
        @NotBlank(message = "显示名不能为空") String displayName,
        String mobile,
        String wechatNo,
        String qqNo,
        String roleCode
) {
}
