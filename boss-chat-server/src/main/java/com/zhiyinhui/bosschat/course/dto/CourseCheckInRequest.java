package com.zhiyinhui.bosschat.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourseCheckInRequest(
        @NotBlank(message = "请填写学员姓名")
        @Size(max = 80, message = "学员姓名不能超过80个字符")
        String studentName
) {
}
