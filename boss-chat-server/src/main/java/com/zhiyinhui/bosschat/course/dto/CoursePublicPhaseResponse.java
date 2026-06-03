package com.zhiyinhui.bosschat.course.dto;

public record CoursePublicPhaseResponse(
        String phaseCode,
        String phaseName,
        String courseName,
        String surveyPath,
        String remark
) {
}
