package com.zhiyinhui.bosschat.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyinhui.bosschat.ai.dto.AgentTaskContextMessage;
import com.zhiyinhui.bosschat.ai.dto.AgentTaskRequest;
import com.zhiyinhui.bosschat.ai.dto.AgentTaskResponse;
import com.zhiyinhui.bosschat.ai.dto.AgentToolApprovalResponse;
import com.zhiyinhui.bosschat.ai.dto.AgentToolStepResponse;
import com.zhiyinhui.bosschat.ai.dto.AiChatAttachmentRequest;
import com.zhiyinhui.bosschat.ai.dto.AiChatMessageResponse;
import com.zhiyinhui.bosschat.ai.dto.AiChatRequest;
import com.zhiyinhui.bosschat.ai.dto.AiChatResponse;
import com.zhiyinhui.bosschat.ai.dto.AiChatStreamMetaResponse;
import com.zhiyinhui.bosschat.ai.dto.LlmChatResult;
import com.zhiyinhui.bosschat.ai.entity.AiAgent;
import com.zhiyinhui.bosschat.ai.entity.AiConversation;
import com.zhiyinhui.bosschat.ai.entity.AiMessage;
import com.zhiyinhui.bosschat.ai.entity.AiScene;
import com.zhiyinhui.bosschat.ai.entity.AiUsageRecord;
import com.zhiyinhui.bosschat.ai.mapper.AiConversationMapper;
import com.zhiyinhui.bosschat.ai.mapper.AiMessageMapper;
import com.zhiyinhui.bosschat.ai.mapper.AiUsageRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class AiChatService {

    private final AiAgentService aiAgentService;
    private final LlmChatService llmChatService;
    private final AiConversationMapper aiConversationMapper;
    private final AiMessageMapper aiMessageMapper;
    private final AiUsageRecordMapper aiUsageRecordMapper;
    private final AutonomousAgentService autonomousAgentService;
    private final AiMemoryService aiMemoryService;
    private final AiKnowledgeService aiKnowledgeService;
    private final AiWorkflowService aiWorkflowService;
    private final AiSceneService aiSceneService;

    public AiChatService(
            AiAgentService aiAgentService,
            LlmChatService llmChatService,
            AiConversationMapper aiConversationMapper,
            AiMessageMapper aiMessageMapper,
            AiUsageRecordMapper aiUsageRecordMapper,
            AutonomousAgentService autonomousAgentService,
            AiMemoryService aiMemoryService,
            AiKnowledgeService aiKnowledgeService,
            AiWorkflowService aiWorkflowService,
            AiSceneService aiSceneService
    ) {
        this.aiAgentService = aiAgentService;
        this.llmChatService = llmChatService;
        this.aiConversationMapper = aiConversationMapper;
        this.aiMessageMapper = aiMessageMapper;
        this.aiUsageRecordMapper = aiUsageRecordMapper;
        this.autonomousAgentService = autonomousAgentService;
        this.aiMemoryService = aiMemoryService;
        this.aiKnowledgeService = aiKnowledgeService;
        this.aiWorkflowService = aiWorkflowService;
        this.aiSceneService = aiSceneService;
    }

    public AiChatResponse send(Long userId, AiChatRequest request) {
        PreparedChat preparedChat = prepare(userId, request);
        if (isToolAgent(preparedChat.agent()) && !hasNativeMultimodalAttachment(preparedChat.attachments())) {
            throw new ResponseStatusException(BAD_REQUEST, "该 AI 需要使用流式接口执行工具能力");
        }
        LlmChatResult result = llmChatService.chat(
                preparedChat.agent(),
                preparedChat.history(),
                buildEnhancedSystemContext(preparedChat, request),
                preparedChat.attachments()
        );
        AiMessage assistantMessage = saveAssistantMessage(userId, preparedChat, result);
        return toResponse(preparedChat, assistantMessage);
    }

    public AiChatResponse stream(
            Long userId,
            AiChatRequest request,
            Consumer<AiChatStreamMetaResponse> onStart,
            Consumer<String> onDelta,
            Consumer<AgentToolStepResponse> onToolStep,
            Consumer<AgentToolApprovalResponse> onApprovalRequired,
            AgentCancellationToken cancellationToken
    ) {
        PreparedChat preparedChat = prepare(userId, request);
        onStart.accept(new AiChatStreamMetaResponse(
                preparedChat.conversation().getId(),
                toMessageResponse(preparedChat.userMessage())
        ));
        if (isToolAgent(preparedChat.agent()) && !hasNativeMultimodalAttachment(preparedChat.attachments())) {
            AgentTaskResponse agentTaskResponse = autonomousAgentService.run(
                    userId,
                    new AgentTaskRequest(
                            preparedChat.agent().getId(),
                            preparedChat.conversation().getId(),
                            buildUserTaskContent(request),
                            request.workflowCode(),
                            toAgentContextMessages(preparedChat)
                    ),
                    onToolStep,
                    onApprovalRequired,
                    cancellationToken
            );
            onDelta.accept(agentTaskResponse.answer());
            AiMessage assistantMessage = saveAssistantMessage(
                    userId,
                    preparedChat,
                    new LlmChatResult(
                            agentTaskResponse.answer(),
                            agentTaskResponse.provider(),
                            agentTaskResponse.model(),
                            0,
                            0,
                            0
                    )
            );
            return toResponse(preparedChat, assistantMessage);
        }
        LlmChatResult result = llmChatService.stream(
                preparedChat.agent(),
                preparedChat.history(),
                buildEnhancedSystemContext(preparedChat, request),
                preparedChat.attachments(),
                onDelta
        );
        AiMessage assistantMessage = saveAssistantMessage(userId, preparedChat, result);
        return toResponse(preparedChat, assistantMessage);
    }

    private PreparedChat prepare(Long userId, AiChatRequest request) {
        List<AiChatAttachmentRequest> attachments = normalizeAttachments(request);
        String content = clean(request.content());
        if (content.isBlank() && attachments.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "请输入消息内容或上传附件");
        }

        AiScene scene = aiSceneService.requireEnabledScene(request.sceneId());
        AiAgent agent = aiSceneService.requireEnabledSceneAgent(scene.getId(), request.agentId());
        AiConversation conversation = resolveConversation(userId, scene, agent, request, attachments);
        AiMessage userMessage = newMessage(
                conversation.getId(),
                userId,
                null,
                "",
                "user",
                buildStoredUserContent(content, attachments),
                "",
                "",
                0,
                0,
                0
        );
        aiMessageMapper.insert(userMessage);
        List<AiMessage> history = aiMessageMapper.selectList(new LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getConversationId, conversation.getId())
                .orderByAsc(AiMessage::getId));
        return new PreparedChat(scene, agent, conversation, userMessage, history, attachments);
    }

    private AiMessage saveAssistantMessage(Long userId, PreparedChat preparedChat, LlmChatResult result) {
        AiMessage assistantMessage = newMessage(
                preparedChat.conversation().getId(),
                userId,
                preparedChat.agent().getId(),
                preparedChat.agent().getAgentName(),
                "assistant",
                result.content(),
                result.provider(),
                result.model(),
                result.promptTokens(),
                result.completionTokens(),
                result.totalTokens()
        );
        aiMessageMapper.insert(assistantMessage);

        AiUsageRecord usageRecord = new AiUsageRecord();
        usageRecord.setUserId(userId);
        usageRecord.setAgentId(preparedChat.agent().getId());
        usageRecord.setConversationId(preparedChat.conversation().getId());
        usageRecord.setModelProvider(result.provider());
        usageRecord.setModelName(result.model());
        usageRecord.setPromptTokens(result.promptTokens());
        usageRecord.setCompletionTokens(result.completionTokens());
        usageRecord.setTotalTokens(result.totalTokens());
        aiUsageRecordMapper.insert(usageRecord);

        preparedChat.conversation().setAgentId(preparedChat.agent().getId());
        preparedChat.conversation().setUpdateTime(LocalDateTime.now());
        aiConversationMapper.updateById(preparedChat.conversation());
        return assistantMessage;
    }

    private AiChatResponse toResponse(PreparedChat preparedChat, AiMessage assistantMessage) {
        return new AiChatResponse(
                preparedChat.conversation().getId(),
                toMessageResponse(preparedChat.userMessage()),
                toMessageResponse(assistantMessage)
        );
    }

    private AiConversation resolveConversation(
            Long userId,
            AiScene scene,
            AiAgent agent,
            AiChatRequest request,
            List<AiChatAttachmentRequest> attachments
    ) {
        if (request.conversationId() != null) {
            AiConversation conversation = aiConversationMapper.selectOne(new LambdaQueryWrapper<AiConversation>()
                    .eq(AiConversation::getId, request.conversationId())
                    .eq(AiConversation::getUserId, userId)
                    .eq(AiConversation::getStatus, "active")
                    .last("LIMIT 1"));
            if (conversation == null) {
                throw new ResponseStatusException(NOT_FOUND, "会话不存在");
            }
            validateConversationScope(conversation, scene, agent);
            return conversation;
        }

        AiConversation conversation = new AiConversation();
        conversation.setUserId(userId);
        conversation.setSceneId(scene.getId());
        conversation.setChatMode(scene.getChatMode());
        conversation.setAgentId(agent.getId());
        conversation.setTitle(createTitle(request.content(), attachments));
        conversation.setStatus("active");
        aiConversationMapper.insert(conversation);
        return conversation;
    }

    private void validateConversationScope(AiConversation conversation, AiScene scene, AiAgent agent) {
        if (!Objects.equals(conversation.getSceneId(), scene.getId())) {
            throw new ResponseStatusException(BAD_REQUEST, "会话不属于当前场景");
        }
        String conversationMode = clean(conversation.getChatMode()).isBlank()
                ? AiSceneService.MODE_SINGLE
                : conversation.getChatMode();
        if (!conversationMode.equals(scene.getChatMode())) {
            throw new ResponseStatusException(BAD_REQUEST, "会话模式和场景模式不一致");
        }
        if (aiSceneService.isSingleMode(scene) && !Objects.equals(conversation.getAgentId(), agent.getId())) {
            throw new ResponseStatusException(BAD_REQUEST, "单聊场景不能切换会话绑定的 AI，请新建对话");
        }
    }

    private AiMessage newMessage(
            Long conversationId,
            Long userId,
            Long agentId,
            String agentName,
            String role,
            String content,
            String provider,
            String model,
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens
    ) {
        AiMessage message = new AiMessage();
        message.setConversationId(conversationId);
        message.setUserId(userId);
        message.setAgentId(agentId);
        message.setAgentName(agentName == null ? "" : agentName);
        message.setRole(role);
        message.setContent(content);
        message.setModelProvider(provider);
        message.setModelName(model);
        message.setPromptTokens(promptTokens);
        message.setCompletionTokens(completionTokens);
        message.setTotalTokens(totalTokens);
        return message;
    }

    private String createTitle(String content, List<AiChatAttachmentRequest> attachments) {
        String title = clean(content).replaceAll("\\s+", " ");
        if (title.isBlank() && !attachments.isEmpty()) {
            title = "附件分析：" + clean(attachments.get(0).fileName());
        }
        if (title.isBlank()) {
            title = "新的对话";
        }
        return title.length() <= 40 ? title : title.substring(0, 40);
    }

    private boolean isToolAgent(AiAgent agent) {
        return (agent.getToolsEnabled() != null && agent.getToolsEnabled() == 1)
                || (agent.getImageGenerationEnabled() != null && agent.getImageGenerationEnabled() == 1);
    }

    private boolean hasNativeMultimodalAttachment(List<AiChatAttachmentRequest> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return false;
        }
        return attachments.stream()
                .anyMatch(attachment -> Set.of("image", "video", "document", "file").contains(clean(attachment.fileType())));
    }

    private String buildEnhancedSystemContext(PreparedChat preparedChat, AiChatRequest request) {
        StringBuilder builder = new StringBuilder();
        appendContext(builder, sceneContext(preparedChat.scene()));
        AiAgent agent = preparedChat.agent();
        if (agent.getMemoryEnabled() != null && agent.getMemoryEnabled() == 1) {
            appendContext(builder, aiMemoryService.buildMemoryContext(agent.getId()));
        }
        if (agent.getKnowledgeEnabled() != null && agent.getKnowledgeEnabled() == 1) {
            appendContext(builder, aiKnowledgeService.buildKnowledgeContext(agent.getId(), clean(request.content())));
        }
        if (agent.getWorkflowEnabled() != null && agent.getWorkflowEnabled() == 1) {
            appendContext(builder, aiWorkflowService.buildWorkflowContext(agent.getId(), request.workflowCode()));
        }
        appendContext(builder, attachmentContext(preparedChat.attachments()));
        return builder.toString();
    }

    private String sceneContext(AiScene scene) {
        if (aiSceneService.isTeamMode(scene)) {
            return "当前场景：" + scene.getSceneName()
                    + "。这是团队会话，多个 AI 共用同一份上下文。历史 assistant 消息中可能带有【AI名称】，代表当时是哪位 AI 回复。请只以当前 AI 的身份回答。";
        }
        return "当前场景：" + scene.getSceneName()
                + "。这是单聊会话，当前 AI 拥有独立上下文。切换 AI 时应进入对应 AI 的独立会话。";
    }

    private String attachmentContext(List<AiChatAttachmentRequest> attachments) {
        if (attachments.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder("用户本轮上传附件：\n");
        for (AiChatAttachmentRequest attachment : attachments) {
            builder.append("- ")
                    .append(labelForType(attachment.fileType()))
                    .append("：")
                    .append(clean(attachment.fileName()))
                    .append("\n");
            if (!clean(attachment.summary()).isBlank()) {
                builder.append(clean(attachment.summary())).append("\n");
            }
        }
        return builder.toString().trim();
    }

    private String buildStoredUserContent(String content, List<AiChatAttachmentRequest> attachments) {
        StringBuilder builder = new StringBuilder(clean(content));
        if (!attachments.isEmpty()) {
            if (!builder.isEmpty()) {
                builder.append("\n\n");
            }
            builder.append("上传附件：");
            for (AiChatAttachmentRequest attachment : attachments) {
                builder.append("\n- ")
                        .append(clean(attachment.fileName()))
                        .append("（")
                        .append(labelForType(attachment.fileType()))
                        .append("）");
            }
        }
        return builder.toString();
    }

    private String buildUserTaskContent(AiChatRequest request) {
        String content = clean(request.content());
        List<AiChatAttachmentRequest> attachments = normalizeAttachments(request);
        if (attachments.isEmpty()) {
            return content;
        }
        String attachmentText = attachmentContext(attachments);
        return content.isBlank()
                ? attachmentText
                : content + "\n\n" + attachmentText;
    }

    private void appendContext(StringBuilder builder, String context) {
        if (context == null || context.isBlank()) {
            return;
        }
        if (!builder.isEmpty()) {
            builder.append("\n");
        }
        builder.append(context.trim()).append("\n");
    }

    private List<AgentTaskContextMessage> toAgentContextMessages(PreparedChat preparedChat) {
        return preparedChat.history()
                .stream()
                .filter(message -> !message.getId().equals(preparedChat.userMessage().getId()))
                .map(message -> new AgentTaskContextMessage(message.getRole(), contextContent(message)))
                .toList();
    }

    private AiChatMessageResponse toMessageResponse(AiMessage message) {
        return new AiChatMessageResponse(
                message.getRole(),
                message.getContent(),
                message.getAgentId(),
                message.getAgentName(),
                message.getModelProvider(),
                message.getModelName()
        );
    }

    private String contextContent(AiMessage message) {
        if ("assistant".equals(message.getRole()) && message.getAgentName() != null && !message.getAgentName().isBlank()) {
            return "【" + message.getAgentName() + "】\n" + message.getContent();
        }
        return message.getContent();
    }

    private List<AiChatAttachmentRequest> normalizeAttachments(AiChatRequest request) {
        if (request.attachments() == null) {
            return List.of();
        }
        return request.attachments()
                .stream()
                .filter(Objects::nonNull)
                .toList();
    }

    private String labelForType(String fileType) {
        return switch (clean(fileType)) {
            case "image" -> "图片";
            case "excel" -> "Excel";
            case "video" -> "视频";
            case "document" -> "文档";
            default -> "文件";
        };
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private record PreparedChat(
            AiScene scene,
            AiAgent agent,
            AiConversation conversation,
            AiMessage userMessage,
            List<AiMessage> history,
            List<AiChatAttachmentRequest> attachments
    ) {
    }
}
