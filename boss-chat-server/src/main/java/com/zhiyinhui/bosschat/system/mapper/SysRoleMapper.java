package com.zhiyinhui.bosschat.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyinhui.bosschat.system.entity.SysRole;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysRoleMapper extends BaseMapper<SysRole> {

    @Select("""
            SELECT r.*
            FROM sys_role r
            INNER JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
              AND r.enabled = 1
            ORDER BY r.id
            """)
    List<SysRole> selectByUserId(Long userId);

    @Select("""
            SELECT r.role_code
            FROM sys_role r
            INNER JOIN sys_user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
              AND r.enabled = 1
            ORDER BY r.id
            """)
    List<String> selectCodesByUserId(Long userId);
}
