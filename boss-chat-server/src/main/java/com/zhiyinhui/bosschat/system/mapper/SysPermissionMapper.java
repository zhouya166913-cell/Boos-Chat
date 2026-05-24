package com.zhiyinhui.bosschat.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyinhui.bosschat.system.entity.SysPermission;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    @Select("""
            SELECT DISTINCT p.permission_code
            FROM sys_permission p
            INNER JOIN sys_role_permission rp ON rp.permission_id = p.id
            INNER JOIN sys_user_role ur ON ur.role_id = rp.role_id
            INNER JOIN sys_role r ON r.id = rp.role_id
            WHERE ur.user_id = #{userId}
              AND p.enabled = 1
              AND r.enabled = 1
            ORDER BY p.permission_code
            """)
    List<String> selectCodesByUserId(Long userId);
}
