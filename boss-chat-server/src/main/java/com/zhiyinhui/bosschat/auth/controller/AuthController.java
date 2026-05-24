package com.zhiyinhui.bosschat.auth.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhiyinhui.bosschat.auth.dto.CurrentUserResponse;
import com.zhiyinhui.bosschat.auth.dto.LoginRequest;
import com.zhiyinhui.bosschat.auth.dto.LoginResponse;
import com.zhiyinhui.bosschat.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "获取当前登录用户")
    @SecurityRequirement(name = "Sa-Token")
    @GetMapping("/me")
    public CurrentUserResponse me() {
        StpUtil.checkLogin();
        return authService.currentUser();
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public void logout() {
        if (StpUtil.isLogin()) {
            StpUtil.logout();
        }
    }
}
