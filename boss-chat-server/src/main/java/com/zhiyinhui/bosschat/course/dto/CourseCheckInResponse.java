package com.zhiyinhui.bosschat.course.dto;

public record CourseCheckInResponse(
        Long phaseId,
        String phaseCode,
        String phaseName,
        Long studentId,
        String studentName,
        Long groupId,
        String groupName,
        String groupLeaderName,
        String seatNo,
        String phone,
        String idCard,
        Integer isNewStudent,
        Integer checkInCount,
        java.time.LocalDateTime lastCheckInTime
) {
}
