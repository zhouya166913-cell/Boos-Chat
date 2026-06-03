package com.zhiyinhui.bosschat.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CourseCheckInRequest(
        @NotBlank(message = "请填写学员姓名")
        @Size(max = 80, message = "学员姓名不能超过80个字符")
        String studentName,

        @NotBlank(message = "请填写手机号")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号需要填写11位，请检查一下")
        String phone,

        @NotBlank(message = "请填写身份证号")
        @Size(max = 32, message = "身份证号不能超过32个字符")
        String idCard
) {
}
