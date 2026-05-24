package com.zhiyinhui.bosschat.system.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

public interface SysRolePermissionMapper {

    @Select("""
            SELECT COUNT(1)
            FROM sys_role_permission
            WHERE role_id = #{roleId}
              AND permission_id = #{permissionId}
            """)
    Long countByRoleIdAndPermissionId(Long roleId, Long permissionId);

    @Insert("""
            INSERT INTO sys_role_permission (role_id, permission_id)
            VALUES (#{roleId}, #{permissionId})
            """)
    int insertRelation(Long roleId, Long permissionId);
}
