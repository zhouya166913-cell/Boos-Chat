package com.zhiyinhui.bosschat.ai.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhiyinhui.bosschat.ai.dto.AiWorkflowRequest;
import com.zhiyinhui.bosschat.ai.dto.AiWorkflowResponse;
import com.zhiyinhui.bosschat.ai.service.AiAgentService;
import com.zhiyinhui.bosschat.ai.service.AiWorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "智能体工作流管理")
@RestController
@RequestMapping("/api/admin/agents/{agentId}/workflows")
public class AdminAiWorkflowController {

    private final AiAgentService aiAgentService;
    private final AiWorkflowService aiWorkflowService;

    public AdminAiWorkflowController(AiAgentService aiAgentService, AiWorkflowService aiWorkflowService) {
        this.aiAgentService = aiAgentService;
        this.aiWorkflowService = aiWorkflowService;
    }

    @Operation(summary = "查询智能体工作流", description = "查询某个智能体下维护的工作流，用户对话时可选择对应工作流约束执行步骤。")
    @SecurityRequirement(name = "Sa-Token")
    @GetMapping
    public List<AiWorkflowResponse> list(@PathVariable Long agentId) {
        StpUtil.checkRole("super_admin");
        aiAgentService.requireExistingAgent(agentId);
        return aiWorkflowService.listByAgent(agentId);
    }

    @Operation(summary = "新增智能体工作流", description = "为某个智能体新增一条工作流，例如咨询诊断流程、代码任务流程、资料分析流程等。")
    @SecurityRequirement(name = "Sa-Token")
    @PostMapping
    public AiWorkflowResponse create(@PathVariable Long agentId, @Valid @RequestBody AiWorkflowRequest request) {
        StpUtil.checkRole("super_admin");
        aiAgentService.requireExistingAgent(agentId);
        return aiWorkflowService.createForAgent(agentId, request);
    }

    @Operation(summary = "修改智能体工作流", description = "修改指定智能体下的某一条工作流。")
    @SecurityRequirement(name = "Sa-Token")
    @PutMapping("/{workflowId}")
    public AiWorkflowResponse update(
            @PathVariable Long agentId,
            @PathVariable Long workflowId,
            @Valid @RequestBody AiWorkflowRequest request
    ) {
        StpUtil.checkRole("super_admin");
        aiAgentService.requireExistingAgent(agentId);
        return aiWorkflowService.updateForAgent(agentId, workflowId, request);
    }

    @Operation(summary = "删除智能体工作流", description = "删除指定智能体下的某一条工作流。")
    @SecurityRequirement(name = "Sa-Token")
    @DeleteMapping("/{workflowId}")
    public void delete(@PathVariable Long agentId, @PathVariable Long workflowId) {
        StpUtil.checkRole("super_admin");
        aiAgentService.requireExistingAgent(agentId);
        aiWorkflowService.deleteForAgent(agentId, workflowId);
    }
}
