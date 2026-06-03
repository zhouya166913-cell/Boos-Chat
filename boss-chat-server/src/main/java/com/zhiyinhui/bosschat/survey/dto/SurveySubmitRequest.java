package com.zhiyinhui.bosschat.survey.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SurveySubmitRequest(
        @Size(max = 64, message = "期数编号不能超过64个字符")
        String phaseCode,

        @NotBlank(message = "请填写姓名")
        @Size(max = 80, message = "姓名不能超过80个字符")
        String customerName,

        @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号需要填写11位，请检查一下")
        String phone,

        @Size(max = 32, message = "身份证号不能超过32个字符")
        String idCard,

        @Size(max = 160, message = "公司不能超过160个字符")
        String company,

        @Size(max = 80, message = "公司人数不能超过80个字符")
        String employeeCount,

        @Size(max = 120, message = "公司业绩不能超过120个字符")
        String annualRevenue,

        @NotBlank(message = "请填写公司主营产品/服务")
        @Size(max = 1000, message = "公司主营产品/服务不能超过1000个字符")
        String productService,

        @Size(max = 1000, message = "主要客户群体不能超过1000个字符")
        String targetCustomers,

        @Size(max = 1000, message = "获客渠道不能超过1000个字符")
        String acquisitionChannels,

        @Size(max = 1000, message = "业务流程或系统使用情况不能超过1000个字符")
        String workflowStatus,

        List<String> painPoints,
        List<String> currentImpact,
        String aiStatus,
        String priorityGoal,
        String biggestObstacle,
        String willingness,
        List<String> cooperationPreference,
        String urgentProblem,
        String followUpIntention
) {
}
