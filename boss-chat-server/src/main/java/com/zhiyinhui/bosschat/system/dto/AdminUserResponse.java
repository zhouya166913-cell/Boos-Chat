package com.zhiyinhui.bosschat.system.dto;

import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String username,
        String displayName,
        String mobile,
        String wechatNo,
        String qqNo,
        String roleCode,
        String roleName,
        Integer status,
        LocalDateTime lastLoginTime,
        LocalDateTime createTime
) {
}
