package com.zhiyinhui.bosschat.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiyinhui.bosschat.ai.dto.AgentTaskRequest;
import com.zhiyinhui.bosschat.ai.dto.AgentTaskResponse;
import com.zhiyinhui.bosschat.ai.dto.AgentTaskContextMessage;
import com.zhiyinhui.bosschat.ai.dto.AgentToolApprovalResponse;
import com.zhiyinhui.bosschat.ai.dto.AgentToolStepResponse;
import com.zhiyinhui.bosschat.ai.dto.LlmToolCall;
import com.zhiyinhui.bosschat.ai.dto.LlmToolResponse;
import com.zhiyinhui.bosschat.ai.entity.AiAgent;
import com.zhiyinhui.bosschat.ai.entity.AiToolExecution;
import com.zhiyinhui.bosschat.ai.mapper.AiToolExecutionMapper;
import com.zhiyinhui.bosschat.common.config.AgentWorkspaceProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class AutonomousAgentService {

    private final AiAgentService aiAgentService;
    private final AiMemoryService aiMemoryService;
    private final AiKnowledgeService aiKnowledgeService;
    private final AiWorkflowService aiWorkflowService;
    private final AgentToolRegistry agentToolRegistry;
    private final AgentToolApprovalService agentToolApprovalService;
    private final LlmChatService llmChatService;
    private final AiToolExecutionMapper aiToolExecutionMapper;
    private final ObjectMapper objectMapper;
    private final int maxToolRounds;

    public AutonomousAgentService(
            AiAgentService aiAgentService,
            AiMemoryService aiMemoryService,
            AiKnowledgeService aiKnowledgeService,
            AiWorkflowService aiWorkflowService,
            AgentToolRegistry agentToolRegistry,
            AgentToolApprovalService agentToolApprovalService,
            LlmChatService llmChatService,
            AiToolExecutionMapper aiToolExecutionMapper,
            ObjectMapper objectMapper,
            AgentWorkspaceProperties agentWorkspaceProperties
    ) {
        this.aiAgentService = aiAgentService;
        this.aiMemoryService = aiMemoryService;
        this.aiKnowledgeService = aiKnowledgeService;
        this.aiWorkflowService = aiWorkflowService;
        this.agentToolRegistry = agentToolRegistry;
        this.agentToolApprovalService = agentToolApprovalService;
        this.llmChatService = llmChatService;
        this.aiToolExecutionMapper = aiToolExecutionMapper;
        this.objectMapper = objectMapper;
        this.maxToolRounds = agentWorkspaceProperties.maxToolRounds() == null
                ? 30
                : agentWorkspaceProperties.maxToolRounds();
    }

    public AgentTaskResponse run(Long userId, AgentTaskRequest request) {
        return run(
                userId,
                request,
                step -> {},
                approval -> {
                    throw new IllegalStateException("敏感工具需要使用流式接口确认");
                },
                new AgentCancellationToken()
        );
    }

    public AgentTaskResponse run(
            Long userId,
            AgentTaskRequest request,
            Consumer<AgentToolStepResponse> onToolStep
    ) {
        return run(
                userId,
                request,
                onToolStep,
                approval -> {
                    throw new IllegalStateException("敏感工具需要使用流式接口确认");
                },
                new AgentCancellationToken()
        );
    }

    public AgentTaskResponse run(
            Long userId,
            AgentTaskRequest request,
            Consumer<AgentToolStepResponse> onToolStep,
            Consumer<AgentToolApprovalResponse> onApprovalRequired,
            AgentCancellationToken cancellationToken
    ) {
        AiAgent agent = aiAgentService.requireEnabledAgent(request.agentId());
        if (!isEnabled(agent.getToolsEnabled()) && !isEnabled(agent.getImageGenerationEnabled())) {
            throw new ResponseStatusException(BAD_REQUEST, "该智能体尚未启用工具调用或图片生成能力");
        }

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(message("system", buildSystemPrompt(userId, agent, request)));
        messages.addAll(buildContextMessages(request.contextMessages()));
        messages.add(message("user", request.prompt().trim()));
        List<Map<String, Object>> tools = agentToolRegistry.definitions(agent);
        List<AgentToolStepResponse> steps = new ArrayList<>();
        List<String> generatedImageMarkdowns = new ArrayList<>();

        for (int round = 0; round < maxToolRounds; round++) {
            cancellationToken.throwIfCancelled();
            LlmToolResponse response = llmChatService.chatWithTools(agent, messages, tools);
            cancellationToken.throwIfCancelled();
            if (response.toolCalls().isEmpty()) {
                return new AgentTaskResponse(
                        appendMissingImages(response.content(), generatedImageMarkdowns),
                        steps,
                        response.provider(),
                        response.model()
                );
            }

            messages.add(response.assistantMessage());
            for (LlmToolCall toolCall : response.toolCalls()) {
                cancellationToken.throwIfCancelled();
                String result;
                String status = "success";
                AgentToolApprovalResponse pendingApproval = null;
                boolean approvalGranted = false;
                try {
                    if (agentToolRegistry.requiresApproval(toolCall.name(), toolCall.argumentsJson())) {
                        AgentToolApprovalResponse approval = agentToolApprovalService.create(
                                userId,
                                toolCall.name(),
                                toolCall.argumentsJson()
                        );
                        pendingApproval = approval;
                        AgentToolStepResponse approvalStep = new AgentToolStepResponse(
                                toolCall.name(),
                                toolCall.argumentsJson(),
                                approval.message(),
                                "approval_required",
                                approval.approvalId()
                        );
                        steps.add(approvalStep);
                        onToolStep.accept(approvalStep);
                        onApprovalRequired.accept(approval);
                        boolean approved = agentToolApprovalService.waitForDecision(
                                userId,
                                approval.approvalId(),
                                cancellationToken
                        );
                        if (!approved) {
                            status = "rejected";
                            result = "用户拒绝执行该工具，本次未进行修改或命令执行。";
                            saveToolExecution(userId, agent.getId(), toolCall, result, status);
                            AgentToolStepResponse rejectedStep = new AgentToolStepResponse(
                                    toolCall.name(),
                                    toolCall.argumentsJson(),
                                    summarize(result),
                                    status
                            );
                            steps.add(rejectedStep);
                            onToolStep.accept(rejectedStep);
                            messages.add(toolMessage(toolCall.id(), result));
                            continue;
                        }
                        approvalGranted = true;
                    }
                    result = agentToolRegistry.execute(
                            toolCall.name(),
                            toolCall.argumentsJson(),
                            approvalGranted,
                            userId,
                            agent,
                            request.conversationId()
                    );
                    if ("generate_image".equals(toolCall.name()) || "edit_image".equals(toolCall.name())) {
                        extractImageMarkdown(result).forEach(generatedImageMarkdowns::add);
                    }
                } catch (Exception exception) {
                    if (pendingApproval != null) {
                        agentToolApprovalService.remove(pendingApproval.approvalId());
                    }
                    status = "failed";
                    result = exception.getMessage() == null ? "工具执行失败" : exception.getMessage();
                }
                saveToolExecution(userId, agent.getId(), toolCall, result, status);
                AgentToolStepResponse step = new AgentToolStepResponse(
                        toolCall.name(),
                        toolCall.argumentsJson(),
                        summarize(result),
                        status
                );
                steps.add(step);
                onToolStep.accept(step);
                messages.add(toolMessage(toolCall.id(), result));
            }
        }

        throw new ResponseStatusException(BAD_REQUEST, "工具调用轮次超过上限，请缩小任务范围，或让智能体先总结当前已读取的信息");
    }

    private String buildSystemPrompt(Long userId, AiAgent agent, AgentTaskRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append(agent.getSystemPrompt()).append("\n\n");
        builder.append("""
                你现在是一个可执行任务的企业 AI 落地智能体。
                你可以通过工具读取、搜索授权目录内的企业资料、表格、方案文档和交付材料，也可以在用户确认后创建或修改文件。
                访问未授权目录、新建文件、覆盖写入文件、执行本地命令之前，系统会要求用户确认；用户拒绝时必须尊重拒绝结果。
                你会收到当前工作台会话的上下文消息。用户提到“刚才”“上一步”“继续”“这个文件”时，必须结合上下文理解。
                操作前先理解任务，必要时先读取文件再处理。
                当用户明确说“桌面”“电脑桌面”“我的桌面”时，工具路径必须使用“桌面”或“desktop”，不要先读取当前项目工作区。
                当用户没有给出明确位置时，先追问文件或目录在哪里；不要用空路径默认访问工作区。
                读取 .xlsx Excel 文件时必须优先使用 read_excel 工具，不要用 read_file 或 PowerShell 的 Import-Excel 猜测读取。
                如果用户要求生成图片、画图、配图、海报、流程图、示意图或视觉素材，并且 generate_image 工具可用，必须调用 generate_image，而不是只用文本或字符画代替。
                如果用户要求“把上一张图改成/换成/继续调整”，或明确要求基于原图换发型、换衣服、换背景、增删元素、改风格，并且 edit_image 工具可用，必须优先调用 edit_image。
                调用 generate_image 或 edit_image 后，最终回答必须展示工具返回的 Markdown 图片链接。
                不要假装执行工具；需要外部信息时必须调用工具。
                不要重复调用同一个工具读取相同路径或执行相同命令；已获得足够信息时应停止调用工具并直接总结。
                如果任务较大，先输出阶段性结论和下一步建议，不要试图一次性穷尽所有文件。
                处理完成后，优先说明读取或整理了哪些资料、得到什么结论、下一步建议是什么。
                """);
        if (agent.getMemoryEnabled() != null && agent.getMemoryEnabled() == 1) {
            builder.append("\n").append(aiMemoryService.buildMemoryContext(agent.getId()));
        }
        if (agent.getKnowledgeEnabled() != null && agent.getKnowledgeEnabled() == 1) {
            builder.append("\n").append(aiKnowledgeService.buildKnowledgeContext(agent.getId(), request.prompt()));
        }
        if (agent.getWorkflowEnabled() != null && agent.getWorkflowEnabled() == 1) {
            builder.append("\n").append(aiWorkflowService.buildWorkflowContext(agent.getId(), request.workflowCode()));
        }
        return builder.toString();
    }

    private List<Map<String, Object>> buildContextMessages(List<AgentTaskContextMessage> contextMessages) {
        if (contextMessages == null || contextMessages.isEmpty()) {
            return List.of();
        }
        int start = Math.max(0, contextMessages.size() - 12);
        return contextMessages.subList(start, contextMessages.size())
                .stream()
                .filter(item -> item != null && item.content() != null && !item.content().isBlank())
                .map(item -> message(normalizeContextRole(item.role()), summarizeContext(item.content())))
                .toList();
    }

    private String normalizeContextRole(String role) {
        return "assistant".equals(role) ? "assistant" : "user";
    }

    private String summarizeContext(String content) {
        String value = content.trim();
        return value.length() <= 3000 ? value : value.substring(0, 3000) + "\n...[上下文已截断]";
    }

    private void saveToolExecution(
            Long userId,
            Long agentId,
            LlmToolCall toolCall,
            String result,
            String status
    ) {
        AiToolExecution execution = new AiToolExecution();
        execution.setUserId(userId);
        execution.setAgentId(agentId);
        execution.setToolName(toolCall.name());
        execution.setArgumentsJson(toolCall.argumentsJson());
        execution.setResultSummary(summarize(result));
        execution.setStatus(status);
        aiToolExecutionMapper.insert(execution);
    }

    private Map<String, Object> message(String role, String content) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private Map<String, Object> toolMessage(String toolCallId, String content) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "tool");
        message.put("tool_call_id", toolCallId);
        message.put("content", content);
        return message;
    }

    private String summarize(String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= 1200 ? value : value.substring(0, 1200) + "\n...[内容已截断]";
    }

    private boolean isEnabled(Integer value) {
        return value != null && value == 1;
    }

    private List<String> extractImageMarkdown(String result) {
        if (result == null || result.isBlank()) {
            return List.of();
        }
        return result.lines()
                .map(String::trim)
                .filter(line -> line.startsWith("![") && line.contains("](") && line.endsWith(")"))
                .toList();
    }

    private String appendMissingImages(String content, List<String> generatedImageMarkdowns) {
        if (generatedImageMarkdowns.isEmpty()) {
            return content;
        }
        String value = content == null ? "" : content.trim();
        StringBuilder builder = new StringBuilder(value);
        for (String markdown : generatedImageMarkdowns) {
            if (!value.contains(markdown)) {
                if (!builder.isEmpty()) {
                    builder.append("\n\n");
                }
                builder.append(markdown);
            }
        }
        return builder.toString();
    }
}
