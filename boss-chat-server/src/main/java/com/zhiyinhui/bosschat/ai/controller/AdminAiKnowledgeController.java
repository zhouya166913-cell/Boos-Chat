package com.zhiyinhui.bosschat.ai.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhiyinhui.bosschat.ai.dto.AiKnowledgeDocumentRequest;
import com.zhiyinhui.bosschat.ai.dto.AiKnowledgeDocumentResponse;
import com.zhiyinhui.bosschat.ai.service.AiAgentService;
import com.zhiyinhui.bosschat.ai.service.AiKnowledgeService;
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

@Tag(name = "智能体知识库管理")
@RestController
@RequestMapping("/api/admin/agents/{agentId}/knowledge-documents")
public class AdminAiKnowledgeController {

    private final AiAgentService aiAgentService;
    private final AiKnowledgeService aiKnowledgeService;

    public AdminAiKnowledgeController(AiAgentService aiAgentService, AiKnowledgeService aiKnowledgeService) {
        this.aiAgentService = aiAgentService;
        this.aiKnowledgeService = aiKnowledgeService;
    }

    @Operation(summary = "查询智能体知识库", description = "查询某个智能体下维护的知识文档，这些资料会在对话或任务中作为参考上下文。")
    @SecurityRequirement(name = "Sa-Token")
    @GetMapping
    public List<AiKnowledgeDocumentResponse> list(@PathVariable Long agentId) {
        StpUtil.checkRole("super_admin");
        aiAgentService.requireExistingAgent(agentId);
        return aiKnowledgeService.listByAgent(agentId);
    }

    @Operation(summary = "新增智能体知识", description = "为某个智能体新增一条知识资料，例如企业咨询模板、业务方法论、客户背景等。")
    @SecurityRequirement(name = "Sa-Token")
    @PostMapping
    public AiKnowledgeDocumentResponse create(
            @PathVariable Long agentId,
            @Valid @RequestBody AiKnowledgeDocumentRequest request
    ) {
        StpUtil.checkRole("super_admin");
        aiAgentService.requireExistingAgent(agentId);
        return aiKnowledgeService.createForAgent(agentId, request);
    }

    @Operation(summary = "修改智能体知识", description = "修改指定智能体下的某一条知识资料。")
    @SecurityRequirement(name = "Sa-Token")
    @PutMapping("/{documentId}")
    public AiKnowledgeDocumentResponse update(
            @PathVariable Long agentId,
            @PathVariable Long documentId,
            @Valid @RequestBody AiKnowledgeDocumentRequest request
    ) {
        StpUtil.checkRole("super_admin");
        aiAgentService.requireExistingAgent(agentId);
        return aiKnowledgeService.updateForAgent(agentId, documentId, request);
    }

    @Operation(summary = "删除智能体知识", description = "删除指定智能体下的某一条知识资料。")
    @SecurityRequirement(name = "Sa-Token")
    @DeleteMapping("/{documentId}")
    public void delete(@PathVariable Long agentId, @PathVariable Long documentId) {
        StpUtil.checkRole("super_admin");
        aiAgentService.requireExistingAgent(agentId);
        aiKnowledgeService.deleteForAgent(agentId, documentId);
    }
}