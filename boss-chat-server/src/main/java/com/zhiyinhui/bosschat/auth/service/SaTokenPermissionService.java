package com.zhiyinhui.bosschat.auth.service;

import cn.dev33.satoken.stp.StpInterface;
import com.zhiyinhui.bosschat.system.mapper.SysPermissionMapper;
import com.zhiyinhui.bosschat.system.mapper.SysRoleMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SaTokenPermissionService implements StpInterface {

    private final SysRoleMapper sysRoleMapper;
    private final SysPermissionMapper sysPermissionMapper;

    public SaTokenPermissionService(SysRoleMapper sysRoleMapper, SysPermissionMapper sysPermissionMapper) {
        this.sysRoleMapper = sysRoleMapper;
        this.sysPermissionMapper = sysPermissionMapper;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return sysPermissionMapper.selectCodesByUserId(Long.parseLong(loginId.toString()));
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return sysRoleMapper.selectCodesByUserId(Long.parseLong(loginId.toString()));
    }
}
