package com.zhiyinhui.bosschat.auth.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyinhui.bosschat.auth.dto.CurrentUserResponse;
import com.zhiyinhui.bosschat.auth.dto.LoginRequest;
import com.zhiyinhui.bosschat.auth.dto.LoginResponse;
import com.zhiyinhui.bosschat.system.entity.SysLoginRecord;
import com.zhiyinhui.bosschat.system.entity.SysRole;
import com.zhiyinhui.bosschat.system.entity.SysUser;
import com.zhiyinhui.bosschat.system.mapper.SysLoginRecordMapper;
import com.zhiyinhui.bosschat.system.mapper.SysPermissionMapper;
import com.zhiyinhui.bosschat.system.mapper.SysRoleMapper;
import com.zhiyinhui.bosschat.system.mapper.SysUserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.time.LocalDateTime;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysLoginRecordMapper sysLoginRecordMapper;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            SysUserMapper sysUserMapper,
            SysRoleMapper sysRoleMapper,
            SysPermissionMapper sysPermissionMapper,
            SysLoginRecordMapper sysLoginRecordMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysPermissionMapper = sysPermissionMapper;
        this.sysLoginRecordMapper = sysLoginRecordMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.username())
                .last("LIMIT 1"));

        if (user == null || user.getStatus() == null || user.getStatus() != 1
                || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            recordLogin(null, request.username(), false, "invalid_credentials");
            throw new ResponseStatusException(UNAUTHORIZED, "账号或密码错误");
        }

        StpUtil.login(user.getId());
        user.setLastLoginTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
        recordLogin(user.getId(), user.getUsername(), true, "success");
        return new LoginResponse(StpUtil.getTokenValue(), toCurrentUser(user));
    }

    public CurrentUserResponse currentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            StpUtil.logout();
            throw new ResponseStatusException(UNAUTHORIZED, "登录已失效");
        }
        return toCurrentUser(user);
    }

    private void recordLogin(Long userId, String username, boolean success, String reason) {
        SysLoginRecord record = new SysLoginRecord();
        record.setUserId(userId);
        record.setUsername(username);
        record.setLoginType("password");
        record.setSuccess(success ? 1 : 0);
        record.setReason(reason);
        sysLoginRecordMapper.insert(record);
    }

    private CurrentUserResponse toCurrentUser(SysUser user) {
        List<SysRole> roles = sysRoleMapper.selectByUserId(user.getId());
        List<String> roleCodes = roles.stream().map(SysRole::getRoleCode).toList();
        List<String> permissions = sysPermissionMapper.selectCodesByUserId(user.getId());
        String primaryRoleName = roles.stream()
                .findFirst()
                .map(SysRole::getRoleName)
                .orElse("");
        return new CurrentUserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                primaryRoleName,
                roleCodes,
                permissions
        );
    }
}
