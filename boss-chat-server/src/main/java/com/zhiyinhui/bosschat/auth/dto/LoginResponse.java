package com.zhiyinhui.bosschat.auth.dto;

public record LoginResponse(
        String token,
        CurrentUserResponse user
) {
}
