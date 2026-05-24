package com.zhiyinhui.bosschat.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhiyinhui.bosschat.system.dto.AdminUserCreateRequest;
import com.zhiyinhui.bosschat.system.dto.AdminUserResponse;
import com.zhiyinhui.bosschat.system.dto.AdminUserUpdateRequest;
import com.zhiyinhui.bosschat.system.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @Operation(summary = "查询用户列表", description = "供超级管理员查看系统中的全部用户信息。")
    @SecurityRequirement(name = "Sa-Token")
    @GetMapping
    public List<AdminUserResponse> listUsers() {
        StpUtil.checkRole("super_admin");
        return adminUserService.listUsers();
    }

    @Operation(summary = "新增用户", description = "供超级管理员创建新用户，并设置账号、密码、联系方式和角色。")
    @SecurityRequirement(name = "Sa-Token")
    @PostMapping
    public AdminUserResponse createUser(@Valid @RequestBody AdminUserCreateRequest request) {
        StpUtil.checkRole("super_admin");
        return adminUserService.createUser(request);
    }

    @Operation(summary = "修改用户", description = "供超级管理员修改指定用户的基础信息、状态和角色。")
    @SecurityRequirement(name = "Sa-Token")
    @PutMapping("/{userId}")
    public AdminUserResponse updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserUpdateRequest request
    ) {
        StpUtil.checkRole("super_admin");
        return adminUserService.updateUser(userId, request);
    }
}
