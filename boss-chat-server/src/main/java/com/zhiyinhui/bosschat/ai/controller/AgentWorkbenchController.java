package com.zhiyinhui.bosschat.ai.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhiyinhui.bosschat.ai.dto.AgentTaskRequest;
import com.zhiyinhui.bosschat.ai.dto.AgentTaskResponse;
import com.zhiyinhui.bosschat.ai.service.AgentCancellationToken;
import com.zhiyinhui.bosschat.ai.service.AgentToolApprovalService;
import com.zhiyinhui.bosschat.ai.service.AutonomousAgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Tag(name = "智能体工作台")
@RestController
@RequestMapping("/api/agent-workbench")
public class AgentWorkbenchController {

    private final AutonomousAgentService autonomousAgentService;
    private final AgentToolApprovalService agentToolApprovalService;

    public AgentWorkbenchController(
            AutonomousAgentService autonomousAgentService,
            AgentToolApprovalService agentToolApprovalService
    ) {
        this.autonomousAgentService = autonomousAgentService;
        this.agentToolApprovalService = agentToolApprovalService;
    }

    @Operation(summary = "执行智能体任务", description = "供超级管理员执行可调用本地工作区工具的智能体任务。")
    @SecurityRequirement(name = "Sa-Token")
    @PostMapping("/tasks")
    public AgentTaskResponse run(@Valid @RequestBody AgentTaskRequest request) {
        StpUtil.checkRole("super_admin");
        return autonomousAgentService.run(StpUtil.getLoginIdAsLong(), request);
    }

    @Operation(summary = "流式执行智能体任务", description = "供超级管理员以流式方式执行本地工作区智能体任务，实时返回工具执行步骤和最终结果。")
    @SecurityRequirement(name = "Sa-Token")
    @PostMapping(value = "/tasks/stream", produces = "text/event-stream")
    public SseEmitter runStream(@Valid @RequestBody AgentTaskRequest request) {
        StpUtil.checkRole("super_admin");
        Long userId = StpUtil.getLoginIdAsLong();
        SseEmitter emitter = new SseEmitter(0L);
        AgentCancellationToken cancellationToken = new AgentCancellationToken();
        emitter.onCompletion(cancellationToken::cancel);
        emitter.onTimeout(cancellationToken::cancel);
        emitter.onError(error -> cancellationToken.cancel());

        CompletableFuture.runAsync(() -> {
            try {
                if (!sendEvent(emitter, "start", "任务开始执行", cancellationToken)) {
                    return;
                }
                AgentTaskResponse response = autonomousAgentService.run(
                        userId,
                        request,
                        step -> sendEvent(emitter, "step", step, cancellationToken),
                        approval -> sendEvent(emitter, "approval_required", approval, cancellationToken),
                        cancellationToken
                );
                sendEvent(emitter, "done", response, cancellationToken);
                emitter.complete();
            } catch (Exception exception) {
                sendEvent(emitter, "error", exception.getMessage() == null ? "任务执行失败" : exception.getMessage(), cancellationToken);
                emitter.complete();
            }
        });
        return emitter;
    }

    @Operation(summary = "允许执行敏感工具", description = "用户确认允许智能体执行某个需要审批的文件写入或命令执行工具。")
    @SecurityRequirement(name = "Sa-Token")
    @PostMapping("/approvals/{approvalId}/approve")
    public void approve(@PathVariable String approvalId) {
        StpUtil.checkRole("super_admin");
        agentToolApprovalService.approve(StpUtil.getLoginIdAsLong(), approvalId);
    }

    @Operation(summary = "拒绝执行敏感工具", description = "用户拒绝智能体执行某个需要审批的文件写入或命令执行工具。")
    @SecurityRequirement(name = "Sa-Token")
    @PostMapping("/approvals/{approvalId}/reject")
    public void reject(@PathVariable String approvalId) {
        StpUtil.checkRole("super_admin");
        agentToolApprovalService.reject(StpUtil.getLoginIdAsLong(), approvalId);
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
