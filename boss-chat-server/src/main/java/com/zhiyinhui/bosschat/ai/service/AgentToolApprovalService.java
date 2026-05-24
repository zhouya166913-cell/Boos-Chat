package com.zhiyinhui.bosschat.ai.service;

import com.zhiyinhui.bosschat.ai.dto.AgentToolApprovalResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class AgentToolApprovalService {

    private static final Duration APPROVAL_TIMEOUT = Duration.ofMinutes(5);

    private final Map<String, PendingApproval> approvals = new ConcurrentHashMap<>();

    public AgentToolApprovalResponse create(Long userId, String toolName, String argumentsJson) {
        String approvalId = UUID.randomUUID().toString();
        approvals.put(approvalId, new PendingApproval(userId, toolName, argumentsJson, new CompletableFuture<>()));
        return new AgentToolApprovalResponse(
                approvalId,
                toolName,
                argumentsJson,
                "工具 " + toolName + " 即将访问未授权目录、修改文件或执行本地命令，请确认是否允许。"
        );
    }

    public boolean waitForDecision(Long userId, String approvalId, AgentCancellationToken cancellationToken) {
        PendingApproval approval = approvals.get(approvalId);
        if (approval == null || !approval.userId().equals(userId)) {
            throw new ResponseStatusException(BAD_REQUEST, "审批请求不存在或无权限");
        }
        long deadline = System.currentTimeMillis() + APPROVAL_TIMEOUT.toMillis();
        try {
            while (true) {
                cancellationToken.throwIfCancelled();
                long remaining = deadline - System.currentTimeMillis();
                if (remaining <= 0) {
                    approvals.remove(approvalId);
                    throw new ResponseStatusException(BAD_REQUEST, "等待用户确认超时");
                }
                try {
                    Boolean decision = approval.decision().get(Math.min(300, remaining), TimeUnit.MILLISECONDS);
                    approvals.remove(approvalId);
                    return Boolean.TRUE.equals(decision);
                } catch (java.util.concurrent.TimeoutException ignored) {
                    // 继续短轮询，便于及时响应用户停止任务。
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(BAD_REQUEST, "等待用户确认被中断");
        } catch (java.util.concurrent.ExecutionException exception) {
            throw new ResponseStatusException(BAD_REQUEST, "用户确认失败");
        }
    }

    public void approve(Long userId, String approvalId) {
        complete(userId, approvalId, true);
    }

    public void reject(Long userId, String approvalId) {
        complete(userId, approvalId, false);
    }

    public void remove(String approvalId) {
        approvals.remove(approvalId);
    }

    private void complete(Long userId, String approvalId, boolean approved) {
        PendingApproval approval = approvals.get(approvalId);
        if (approval == null || !approval.userId().equals(userId)) {
            throw new ResponseStatusException(BAD_REQUEST, "审批请求不存在或无权限");
        }
        approval.decision().complete(approved);
    }

    private record PendingApproval(
            Long userId,
            String toolName,
            String argumentsJson,
            CompletableFuture<Boolean> decision
    ) {
    }
}
