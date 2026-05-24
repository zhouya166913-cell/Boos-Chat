package com.zhiyinhui.bosschat.ai.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhiyinhui.bosschat.ai.dto.AiMemoryRequest;
import com.zhiyinhui.bosschat.ai.dto.AiMemoryResponse;
import com.zhiyinhui.bosschat.ai.service.AiAgentService;
import com.zhiyinhui.bosschat.ai.service.AiMemoryService;
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

@Tag(name = "智能体长期记忆管理")
@RestController
@RequestMapping("/api/admin/agents/{agentId}/memories")
public class AiMemoryController {

    private final AiAgentService aiAgentService;
    private final AiMemoryService aiMemoryService;

    public AiMemoryController(AiAgentService aiAgentService, AiMemoryService aiMemoryService) {
        this.aiAgentService = aiAgentService;
        this.aiMemoryService = aiMemoryService;
    }

    @Operation(summary = "查询智能体长期记忆", description = "查询某个智能体已维护的长期记忆，用于后续对话或工具任务的固定上下文。")
    @SecurityRequirement(name = "Sa-Token")
    @GetMapping
    public List<AiMemoryResponse> list(@PathVariable Long agentId) {
        StpUtil.checkRole("super_admin");
        aiAgentService.requireExistingAgent(agentId);
        return aiMemoryService.listByAgent(agentId);
    }

    @Operation(summary = "新增智能体长期记忆", description = "为某个智能体新增或保存一条长期记忆，例如老板需求、固定口径、输出风格等。")
    @SecurityRequirement(name = "Sa-Token")
    @PostMapping
    public AiMemoryResponse save(@PathVariable Long agentId, @Valid @RequestBody AiMemoryRequest request) {
        StpUtil.checkRole("super_admin");
        aiAgentService.requireExistingAgent(agentId);
        return aiMemoryService.saveForAgent(agentId, request);
    }

    @Operation(summary = "修改智能体长期记忆", description = "修改指定智能体下的某一条长期记忆。")
    @SecurityRequirement(name = "Sa-Token")
    @PutMapping("/{memoryId}")
    public AiMemoryResponse update(
            @PathVariable Long agentId,
            @PathVariable Long memoryId,
            @Valid @RequestBody AiMemoryRequest request
    ) {
        StpUtil.checkRole("super_admin");
        aiAgentService.requireExistingAgent(agentId);
        return aiMemoryService.updateForAgent(agentId, memoryId, request);
    }

    @Operation(summary = "删除智能体长期记忆", description = "删除指定智能体下的某一条长期记忆。")
    @SecurityRequirement(name = "Sa-Token")
    @DeleteMapping("/{memoryId}")
    public void delete(@PathVariable Long agentId, @PathVariable Long memoryId) {
        StpUtil.checkRole("super_admin");
        aiAgentService.requireExistingAgent(agentId);
        aiMemoryService.deleteForAgent(agentId, memoryId);
    }
}