package com.zhiyinhui.bosschat.course.dto;

import java.time.LocalDateTime;

public record CourseStudentResponse(
        Long id,
        Long phaseId,
        String studentName,
        String phone,
        String idCard,
        Integer isNewStudent,
        String remark,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
