package com.zhiyinhui.bosschat.course.dto;

public record CourseCheckInResponse(
        Long phaseId,
        String phaseCode,
        String phaseName,
        Long studentId,
        String studentName,
        String phone,
        String idCard,
        Integer isNewStudent,
        Integer checkInCount,
        java.time.LocalDateTime lastCheckInTime
) {
}
