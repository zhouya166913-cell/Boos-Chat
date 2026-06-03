package com.zhiyinhui.bosschat.course.dto;

import java.time.LocalDateTime;

public record CourseGroupResponse(
        Long id,
        Long phaseId,
        String groupName,
        String leaderName,
        String remark,
        Integer sortOrder,
        Long studentCount,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
