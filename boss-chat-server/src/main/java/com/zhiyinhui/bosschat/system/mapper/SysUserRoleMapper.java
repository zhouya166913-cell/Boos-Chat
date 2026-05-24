package com.zhiyinhui.bosschat.system.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;

public interface SysUserRoleMapper {

    @Select("""
            SELECT COUNT(1)
            FROM sys_user_role
            WHERE user_id = #{userId}
              AND role_id = #{roleId}
            """)
    Long countByUserIdAndRoleId(Long userId, Long roleId);

    @Insert("""
            INSERT INTO sys_user_role (user_id, role_id)
            VALUES (#{userId}, #{roleId})
            """)
    int insertRelation(Long userId, Long roleId);

    @Delete("""
            DELETE FROM sys_user_role
            WHERE user_id = #{userId}
            """)
    int deleteByUserId(Long userId);
}
