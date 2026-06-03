package com.zhiyinhui.bosschat.course.dto;

import java.time.LocalDateTime;

public record CoursePhaseResponse(
        Long id,
        String phaseCode,
        String phaseName,
        String courseName,
        String surveyPath,
        String qrImageUrl,
        Integer enabled,
        String remark,
        Long studentCount,
        Long surveyRecordCount,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
