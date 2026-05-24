package com.zhiyinhui.bosschat.ai.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhiyinhui.bosschat.ai.dto.AiAgentRequest;
import com.zhiyinhui.bosschat.ai.dto.AiAgentResponse;
import com.zhiyinhui.bosschat.ai.service.AiAgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "智能体管理")
@RestController
@RequestMapping("/api/admin/agents")
public class AdminAiAgentController {

    private final AiAgentService aiAgentService;

    public AdminAiAgentController(AiAgentService aiAgentService) {
        this.aiAgentService = aiAgentService;
    }

    @Operation(summary = "查询全部智能体", description = "供超级管理员查看后台中已经创建的全部智能体配置。")
    @SecurityRequirement(name = "Sa-Token")
    @GetMapping
    public List<AiAgentResponse> listAll() {
        StpUtil.checkRole("super_admin");
        return aiAgentService.listAll();
    }

    @Operation(summary = "新增智能体", description = "供超级管理员创建新的智能体，并配置名称、提示词、模型等信息。")
    @SecurityRequirement(name = "Sa-Token")
    @PostMapping
    public AiAgentResponse create(@Valid @RequestBody AiAgentRequest request) {
        StpUtil.checkRole("super_admin");
        return aiAgentService.create(request);
    }

    @Operation(summary = "修改智能体", description = "供超级管理员修改指定智能体的名称、提示词、模型和启用状态。")
    @SecurityRequirement(name = "Sa-Token")
    @PutMapping("/{agentId}")
    public AiAgentResponse update(@PathVariable Long agentId, @Valid @RequestBody AiAgentRequest request) {
        StpUtil.checkRole("super_admin");
        return aiAgentService.update(agentId, request);
    }
}
