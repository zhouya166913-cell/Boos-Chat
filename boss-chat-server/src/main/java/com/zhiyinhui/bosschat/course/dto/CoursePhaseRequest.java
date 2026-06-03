package com.zhiyinhui.bosschat.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CoursePhaseRequest(
        @NotBlank(message = "请填写期数名称")
        @Size(max = 120, message = "期数名称不能超过120个字符")
        String phaseName,

        @Size(max = 120, message = "课程名称不能超过120个字符")
        String courseName,

        @Size(max = 500, message = "二维码图片地址不能超过500个字符")
        String qrImageUrl,

        Integer enabled,

        @Size(max = 500, message = "备注不能超过500个字符")
        String remark
) {
}
