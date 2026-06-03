package com.zhiyinhui.bosschat.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourseGroupRequest(
        @NotBlank(message = "请填写分组名称")
        @Size(max = 120, message = "分组名称不能超过120个字符")
        String groupName,

        @NotBlank(message = "请填写组长")
        @Size(max = 80, message = "组长名称不能超过80个字符")
        String leaderName,

        @Size(max = 500, message = "备注不能超过500个字符")
        String remark,

        Integer sortOrder
) {
}
