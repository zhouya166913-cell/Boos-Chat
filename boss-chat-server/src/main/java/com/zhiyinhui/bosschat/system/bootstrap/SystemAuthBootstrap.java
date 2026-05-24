package com.zhiyinhui.bosschat.system.bootstrap;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyinhui.bosschat.system.entity.SysPermission;
import com.zhiyinhui.bosschat.system.entity.SysRole;
import com.zhiyinhui.bosschat.system.entity.SysUser;
import com.zhiyinhui.bosschat.system.mapper.SysPermissionMapper;
import com.zhiyinhui.bosschat.system.mapper.SysRoleMapper;
import com.zhiyinhui.bosschat.system.mapper.SysRolePermissionMapper;
import com.zhiyinhui.bosschat.system.mapper.SysUserMapper;
import com.zhiyinhui.bosschat.system.mapper.SysUserRoleMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SystemAuthBootstrap implements CommandLineRunner {

    private static final String SUPER_ADMIN_ROLE_CODE = "super_admin";
    private static final String SUPER_ADMIN_ROLE_NAME = "超级管理员";
    private static final String USER_ROLE_CODE = "user";
    private static final String USER_ROLE_NAME = "普通用户";
    private static final List<PermissionSeed> DEFAULT_PERMISSIONS = List.of(
            new PermissionSeed("system:user:read", "查看用户", "api"),
            new PermissionSeed("system:user:write", "维护用户", "api"),
            new PermissionSeed("system:role:read", "查看角色", "api"),
            new PermissionSeed("system:role:write", "维护角色", "api"),
            new PermissionSeed("system:permission:read", "查看权限", "api"),
            new PermissionSeed("system:permission:write", "维护权限", "api")
    );

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;
    private final String displayName;

    public SystemAuthBootstrap(
            SysUserMapper sysUserMapper,
            SysRoleMapper sysRoleMapper,
            SysPermissionMapper sysPermissionMapper,
            SysUserRoleMapper sysUserRoleMapper,
            SysRolePermissionMapper sysRolePermissionMapper,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.username}") String username,
            @Value("${app.bootstrap.password}") String password,
            @Value("${app.bootstrap.display-name}") String displayName
    ) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysPermissionMapper = sysPermissionMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysRolePermissionMapper = sysRolePermissionMapper;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
        this.displayName = displayName;
    }

    @Override
    public void run(String... args) {
        SysRole superAdminRole = ensureSuperAdminRole();
        ensureUserRole();
        List<SysPermission> permissions = ensurePermissions();
        bindRolePermissions(superAdminRole, permissions);
        SysUser adminUser = ensureAdminUser();
        bindUserRole(adminUser, superAdminRole);
    }

    private SysRole ensureSuperAdminRole() {
        SysRole role = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, SUPER_ADMIN_ROLE_CODE)
                .last("LIMIT 1"));
        if (role != null) {
            role.setRoleName(SUPER_ADMIN_ROLE_NAME);
            role.setDescription("系统内置最高权限角色");
            role.setEnabled(1);
            sysRoleMapper.updateById(role);
            return role;
        }

        SysRole created = new SysRole();
        created.setRoleCode(SUPER_ADMIN_ROLE_CODE);
        created.setRoleName(SUPER_ADMIN_ROLE_NAME);
        created.setDescription("系统内置最高权限角色");
        created.setEnabled(1);
        sysRoleMapper.insert(created);
        return created;
    }

    private SysRole ensureUserRole() {
        SysRole role = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, USER_ROLE_CODE)
                .last("LIMIT 1"));
        if (role != null) {
            role.setRoleName(USER_ROLE_NAME);
            role.setDescription("系统普通用户角色");
            role.setEnabled(1);
            sysRoleMapper.updateById(role);
            return role;
        }

        SysRole created = new SysRole();
        created.setRoleCode(USER_ROLE_CODE);
        created.setRoleName(USER_ROLE_NAME);
        created.setDescription("系统普通用户角色");
        created.setEnabled(1);
        sysRoleMapper.insert(created);
        return created;
    }

    private List<SysPermission> ensurePermissions() {
        return DEFAULT_PERMISSIONS.stream().map(seed -> {
            SysPermission permission = sysPermissionMapper.selectOne(new LambdaQueryWrapper<SysPermission>()
                    .eq(SysPermission::getPermissionCode, seed.code())
                    .last("LIMIT 1"));
            if (permission != null) {
                permission.setPermissionName(seed.name());
                permission.setPermissionType(seed.type());
                permission.setDescription("系统内置权限");
                permission.setEnabled(1);
                sysPermissionMapper.updateById(permission);
                return permission;
            }

            SysPermission created = new SysPermission();
            created.setPermissionCode(seed.code());
            created.setPermissionName(seed.name());
            created.setPermissionType(seed.type());
            created.setDescription("系统内置权限");
            created.setEnabled(1);
            sysPermissionMapper.insert(created);
            return created;
        }).toList();
    }

    private void bindRolePermissions(SysRole role, List<SysPermission> permissions) {
        permissions.forEach(permission -> {
            Long count = sysRolePermissionMapper.countByRoleIdAndPermissionId(role.getId(), permission.getId());
            if (count != null && count > 0) {
                return;
            }

            sysRolePermissionMapper.insertRelation(role.getId(), permission.getId());
        });
    }

    private SysUser ensureAdminUser() {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .last("LIMIT 1"));
        if (user != null) {
            return user;
        }

        SysUser created = new SysUser();
        created.setUsername(username);
        created.setPasswordHash(passwordEncoder.encode(password));
        created.setDisplayName(displayName);
        created.setMobile("");
        created.setWechatNo("");
        created.setQqNo("");
        created.setAvatarUrl("");
        created.setStatus(1);
        sysUserMapper.insert(created);
        return created;
    }

    private void bindUserRole(SysUser user, SysRole role) {
        Long count = sysUserRoleMapper.countByUserIdAndRoleId(user.getId(), role.getId());
        if (count != null && count > 0) {
            return;
        }

        sysUserRoleMapper.insertRelation(user.getId(), role.getId());
    }

    private record PermissionSeed(String code, String name, String type) {
    }
}
