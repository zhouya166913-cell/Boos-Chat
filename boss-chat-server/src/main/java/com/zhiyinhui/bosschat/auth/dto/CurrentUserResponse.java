package com.zhiyinhui.bosschat.auth.dto;

import java.util.List;

public record CurrentUserResponse(
        Long id,
        String username,
        String displayName,
        String role,
        List<String> roles,
        List<String> permissions
) {
}
