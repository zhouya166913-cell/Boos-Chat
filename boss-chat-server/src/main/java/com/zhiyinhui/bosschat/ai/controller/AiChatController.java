package com.zhiyinhui.bosschat.ai.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhiyinhui.bosschat.ai.dto.AiAgentResponse;
import com.zhiyinhui.bosschat.ai.dto.AiChatAttachmentResponse;
import com.zhiyinhui.bosschat.ai.dto.AiChatRequest;
import com.zhiyinhui.bosschat.ai.dto.AiChatResponse;
import com.zhiyinhui.bosschat.ai.dto.AiChatStreamDeltaResponse;
import com.zhiyinhui.bosschat.ai.dto.AiConversationDetailResponse;
import com.zhiyinhui.bosschat.ai.dto.AiConversationResponse;
import com.zhiyinhui.bosschat.ai.dto.AiConversationTitleRequest;
import com.zhiyinhui.bosschat.ai.dto.AiSceneResponse;
import com.zhiyinhui.bosschat.ai.dto.AiWorkflowResponse;
import com.zhiyinhui.bosschat.ai.service.AgentCancellationToken;
import com.zhiyinhui.bosschat.ai.service.AgentToolApprovalService;
import com.zhiyinhui.bosschat.ai.service.AiAgentService;
import com.zhiyinhui.bosschat.ai.service.AiChatService;
import com.zhiyinhui.bosschat.ai.service.AiConversationService;
import com.zhiyinhui.bosschat.ai.service.AiSceneService;
import com.zhiyinhui.bosschat.ai.service.AiWorkflowService;
import com.zhiyinhui.bosschat.ai.service.ChatAttachmentService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Tag(name = "AI 对话")
@RestController
@RequestMapping("/api/chat")
public class AiChatController {

    private final AiAgentService aiAgentService;
    private final AiChatService aiChatService;
    private final AiConversationService aiConversationService;
    private final AiWorkflowService aiWorkflowService;
    private final AgentToolApprovalService agentToolApprovalService;
    private final AiSceneService aiSceneService;
    private final ChatAttachmentService chatAttachmentService;

    public AiChatController(
            AiAgentService aiAgentService,
            AiChatService aiChatService,
            AiConversationService aiConversationService,
            AiWorkflowService aiWorkflowService,
            AgentToolApprovalService agentToolApprovalService,
            AiSceneService aiSceneService,
            ChatAttachmentService chatAttachmentService
    ) {
        this.aiAgentService = aiAgentService;
        this.aiChatService = aiChatService;
        this.aiConversationService = aiConversationService;
        this.aiWorkflowService = aiWorkflowService;
        this.agentToolApprovalService = agentToolApprovalService;
        this.aiSceneService = aiSceneService;
        this.chatAttachmentService = chatAttachmentService;
    }

    @Operation(summary = "查询可用场景", description = "查询当前用户可进入的 AI 对话场景。单聊场景下每个 AI 独立上下文，团队场景下多个 AI 共用上下文。")
    @SecurityRequirement(name = "Sa-Token")
    @GetMapping("/scenes")
    public List<AiSceneResponse> listEnabledScenes() {
        StpUtil.checkLogin();
        return aiSceneService.listEnabled();
    }

    @Operation(summary = "查询可用 AI", description = "查询当前可用的 AI 助手列表，用于对话入口选择 AI。")
    @SecurityRequirement(name = "Sa-Token")
    @GetMapping("/agents")
    public List<AiAgentResponse> listEnabledAgents() {
        StpUtil.checkLogin();
        return aiAgentService.listEnabled();
    }

    @Operation(summary = "查询对话可用工作流", description = "查询当前 AI 在对话入口可选择的已启用工作流。")
    @SecurityRequirement(name = "Sa-Token")
    @GetMapping("/workflows")
    public List<AiWorkflowResponse> listEnabledWorkflows(@RequestParam Long agentId) {
        StpUtil.checkLogin();
        aiAgentService.requireEnabledAgent(agentId);
        return aiWorkflowService.listEnabledByAgent(agentId);
    }

    @Operation(summary = "发送对话消息", description = "向指定场景和指定 AI 发送消息，并返回用户消息与 AI 回复。")
    @SecurityRequirement(name = "Sa-Token")
    @PostMapping("/messages")
    public AiChatResponse send(@Valid @RequestBody AiChatRequest request) {
        StpUtil.checkLogin();
        return aiChatService.send(StpUtil.getLoginIdAsLong(), request);
    }

    @Operation(summary = "上传对话附件", description = "上传图片、Excel、文档或视频附件，用于多模态对话或文件上下文分析。")
    @SecurityRequirement(name = "Sa-Token")
    @PostMapping(value = "/attachments", consumes = "multipart/form-data")
    public AiChatAttachmentResponse uploadAttachment(@RequestParam("file") MultipartFile file) {
        StpUtil.checkLogin();
        return chatAttachmentService.upload(StpUtil.getLoginIdAsLong(), file);
    }

    @Operation(summary = "流式发送对话消息", description = "向指定场景和指定 AI 发送消息，以 SSE 方式实时返回 AI 文本、工具调用步骤和最终结果。")
    @SecurityRequirement(name = "Sa-Token")
    @PostMapping(value = "/messages/stream", produces = "text/event-stream")
    public SseEmitter stream(@Valid @RequestBody AiChatRequest request) {
        StpUtil.checkLogin();
        Long userId = StpUtil.getLoginIdAsLong();
        var agent = aiAgentService.requireEnabledAgent(request.agentId());
        if (agent.getToolsEnabled() != null && agent.getToolsEnabled() == 1) {
            StpUtil.checkRole("super_admin");
        }
        SseEmitter emitter = new SseEmitter(0L);
        AgentCancellationToken cancellationToken = new AgentCancellationToken();
        emitter.onCompletion(cancellationToken::cancel);
        emitter.onTimeout(cancellationToken::cancel);
        emitter.onError(error -> cancellationToken.cancel());

        CompletableFuture.runAsync(() -> {
            try {
                AiChatResponse response = aiChatService.stream(
                        userId,
                        request,
                        meta -> sendEvent(emitter, "meta", meta, cancellationToken),
                        delta -> sendEvent(emitter, "delta", new AiChatStreamDeltaResponse(delta), cancellationToken),
                        step -> sendEvent(emitter, "step", step, cancellationToken),
                        approval -> sendEvent(emitter, "approval_required", approval, cancellationToken),
                        cancellationToken
                );
                sendEvent(emitter, "done", response, cancellationToken);
                emitter.complete();
            } catch (Exception exception) {
                sendEvent(emitter, "error", exception.getMessage() == null ? "请求失败" : exception.getMessage(), cancellationToken);
                emitter.complete();
            }
        });
        return emitter;
    }

    @Operation(summary = "确认执行工具调用", description = "用户确认允许 AI 执行待审批的本地工具、命令或文件操作。")
    @SecurityRequirement(name = "Sa-Token")
    @PostMapping("/approvals/{approvalId}/approve")
    public void approve(@PathVariable String approvalId) {
        StpUtil.checkRole("super_admin");
        agentToolApprovalService.approve(StpUtil.getLoginIdAsLong(), approvalId);
    }

    @Operation(summary = "拒绝执行工具调用", description = "用户拒绝 AI 执行待审批的本地工具、命令或文件操作。")
    @SecurityRequirement(name = "Sa-Token")
    @PostMapping("/approvals/{approvalId}/reject")
    public void reject(@PathVariable String approvalId) {
        StpUtil.checkRole("super_admin");
        agentToolApprovalService.reject(StpUtil.getLoginIdAsLong(), approvalId);
    }

    @Operation(summary = "查询会话列表", description = "查询当前用户的会话列表。单聊场景按 AI 过滤，团队场景按场景共享会话。")
    @SecurityRequirement(name = "Sa-Token")
    @GetMapping("/conversations")
    public List<AiConversationResponse> listConversations(
            @RequestParam(required = false) Long sceneId,
            @RequestParam(required = false) String chatMode,
            @RequestParam(required = false) Long agentId
    ) {
        StpUtil.checkLogin();
        return aiConversationService.list(StpUtil.getLoginIdAsLong(), sceneId, chatMode, agentId);
    }

    @Operation(summary = "查询会话详情", description = "查询指定会话的消息记录，用于恢复聊天窗口和上下文展示。")
    @SecurityRequirement(name = "Sa-Token")
    @GetMapping("/conversations/{conversationId}")
    public AiConversationDetailResponse conversationDetail(@PathVariable Long conversationId) {
        StpUtil.checkLogin();
        return aiConversationService.detail(StpUtil.getLoginIdAsLong(), conversationId);
    }

    @Operation(summary = "清空上下文", description = "清空当前会话上下文消息，用于开启一个干净的新对话；该接口当前页面暂不展示。")
    @SecurityRequirement(name = "Sa-Token")
    @PostMapping("/conversations/{conversationId}/clear-context")
    public void clearContext(@PathVariable Long conversationId) {
        StpUtil.checkLogin();
        aiConversationService.clearContext(StpUtil.getLoginIdAsLong(), conversationId);
    }

    @Operation(summary = "修改会话标题", description = "修改左侧会话列表展示的标题，不影响 AI 消息内容。")
    @SecurityRequirement(name = "Sa-Token")
    @PutMapping("/conversations/{conversationId}/title")
    public void renameConversation(
            @PathVariable Long conversationId,
            @Valid @RequestBody AiConversationTitleRequest request
    ) {
        StpUtil.checkLogin();
        aiConversationService.rename(StpUtil.getLoginIdAsLong(), conversationId, request.title());
    }

    @Operation(summary = "删除会话", description = "软删除当前会话。数据库仍保留记录，但前端列表不再展示。")
    @SecurityRequirement(name = "Sa-Token")
    @DeleteMapping("/conversations/{conversationId}")
    public void deleteConversation(@PathVariable Long conversationId) {
        StpUtil.checkLogin();
        aiConversationService.softDelete(StpUtil.getLoginIdAsLong(), conversationId);
    }

    private boolean sendEvent(SseEmitter emitter, String name, Object data, AgentCancellationToken cancellationToken) {
        if (cancellationToken.isCancelled()) {
            return false;
        }
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
            return true;
        } catch (IOException exception) {
            cancellationToken.cancel();
            emitter.completeWithError(exception);
            return false;
        }
    }
}
