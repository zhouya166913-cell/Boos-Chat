package com.zhiyinhui.bosschat.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyinhui.bosschat.system.dto.AdminUserCreateRequest;
import com.zhiyinhui.bosschat.system.dto.AdminUserResponse;
import com.zhiyinhui.bosschat.system.dto.AdminUserUpdateRequest;
import com.zhiyinhui.bosschat.system.entity.SysRole;
import com.zhiyinhui.bosschat.system.entity.SysUser;
import com.zhiyinhui.bosschat.system.mapper.SysRoleMapper;
import com.zhiyinhui.bosschat.system.mapper.SysUserMapper;
import com.zhiyinhui.bosschat.system.mapper.SysUserRoleMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class AdminUserService {

    private static final String DEFAULT_ROLE_CODE = "user";

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(
            SysUserMapper sysUserMapper,
            SysRoleMapper sysRoleMapper,
            SysUserRoleMapper sysUserRoleMapper,
            PasswordEncoder passwordEncoder
    ) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public List<AdminUserResponse> listUsers() {
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .orderByDesc(SysUser::getId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AdminUserResponse createUser(AdminUserCreateRequest request) {
        SysUser existing = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.username())
                .last("LIMIT 1"));
        if (existing != null) {
            throw new ResponseStatusException(CONFLICT, "账号已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(request.username().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName().trim());
        user.setMobile(clean(request.mobile()));
        user.setWechatNo(clean(request.wechatNo()));
        user.setQqNo(clean(request.qqNo()));
        user.setAvatarUrl("");
        user.setStatus(1);
        sysUserMapper.insert(user);
        bindSingleRole(user.getId(), normalizeRoleCode(request.roleCode()));
        return toResponse(sysUserMapper.selectById(user.getId()));
    }

    @Transactional
    public AdminUserResponse updateUser(Long userId, AdminUserUpdateRequest request) {
        SysUser user = requireUser(userId);
        user.setDisplayName(request.displayName().trim());
        user.setMobile(clean(request.mobile()));
        user.setWechatNo(clean(request.wechatNo()));
        user.setQqNo(clean(request.qqNo()));
        if (request.status() != null) {
            user.setStatus(request.status() == 0 ? 0 : 1);
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        sysUserMapper.updateById(user);
        if (request.roleCode() != null && !request.roleCode().isBlank()) {
            bindSingleRole(userId, normalizeRoleCode(request.roleCode()));
        }
        return toResponse(sysUserMapper.selectById(userId));
    }

    private SysUser requireUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new ResponseStatusException(NOT_FOUND, "用户不存在");
        }
        return user;
    }

    private void bindSingleRole(Long userId, String roleCode) {
        SysRole role = requireRole(roleCode);
        sysUserRoleMapper.deleteByUserId(userId);
        sysUserRoleMapper.insertRelation(userId, role.getId());
    }

    private SysRole requireRole(String roleCode) {
        SysRole role = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, roleCode)
                .last("LIMIT 1"));
        if (role == null) {
            throw new ResponseStatusException(NOT_FOUND, "角色不存在");
        }
        return role;
    }

    private AdminUserResponse toResponse(SysUser user) {
        SysRole role = sysRoleMapper.selectByUserId(user.getId()).stream().findFirst().orElse(null);
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getMobile(),
                user.getWechatNo(),
                user.getQqNo(),
                role == null ? "" : role.getRoleCode(),
                role == null ? "" : role.getRoleName(),
                user.getStatus(),
                user.getLastLoginTime(),
                user.getCreateTime()
        );
    }

    private String normalizeRoleCode(String roleCode) {
        return roleCode == null || roleCode.isBlank() ? DEFAULT_ROLE_CODE : roleCode.trim();
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
