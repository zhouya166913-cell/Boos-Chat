package com.zhiyinhui.bosschat.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyinhui.bosschat.ai.dto.AiChatMessageResponse;
import com.zhiyinhui.bosschat.ai.dto.AiConversationDetailResponse;
import com.zhiyinhui.bosschat.ai.dto.AiConversationResponse;
import com.zhiyinhui.bosschat.ai.entity.AiConversation;
import com.zhiyinhui.bosschat.ai.entity.AiMessage;
import com.zhiyinhui.bosschat.ai.mapper.AiConversationMapper;
import com.zhiyinhui.bosschat.ai.mapper.AiMessageMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class AiConversationService {

    private final AiConversationMapper aiConversationMapper;
    private final AiMessageMapper aiMessageMapper;

    public AiConversationService(
            AiConversationMapper aiConversationMapper,
            AiMessageMapper aiMessageMapper
    ) {
        this.aiConversationMapper = aiConversationMapper;
        this.aiMessageMapper = aiMessageMapper;
    }

    public List<AiConversationResponse> list(Long userId, Long sceneId, String chatMode, Long agentId) {
        String mode = clean(chatMode);
        LambdaQueryWrapper<AiConversation> wrapper = new LambdaQueryWrapper<AiConversation>()
                .eq(AiConversation::getUserId, userId)
                .eq(AiConversation::getStatus, "active")
                .orderByDesc(AiConversation::getUpdateTime)
                .orderByDesc(AiConversation::getId);
        if (sceneId != null) {
            wrapper.eq(AiConversation::getSceneId, sceneId);
        }
        if (!mode.isBlank()) {
            wrapper.eq(AiConversation::getChatMode, mode);
        }
        if (AiSceneService.MODE_SINGLE.equals(mode) && agentId != null) {
            wrapper.eq(AiConversation::getAgentId, agentId);
        }
        return aiConversationMapper.selectList(wrapper)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AiConversationDetailResponse detail(Long userId, Long conversationId) {
        AiConversation conversation = requireConversation(userId, conversationId);
        List<AiChatMessageResponse> messages = aiMessageMapper.selectList(new LambdaQueryWrapper<AiMessage>()
                        .eq(AiMessage::getConversationId, conversation.getId())
                        .orderByAsc(AiMessage::getId))
                .stream()
                .map(this::toMessageResponse)
                .toList();
        return new AiConversationDetailResponse(toResponse(conversation), messages);
    }

    public void clearContext(Long userId, Long conversationId) {
        AiConversation conversation = requireConversation(userId, conversationId);
        conversation.setStatus("cleared");
        conversation.setUpdateTime(LocalDateTime.now());
        aiConversationMapper.updateById(conversation);
    }

    public void rename(Long userId, Long conversationId, String title) {
        AiConversation conversation = requireConversation(userId, conversationId);
        conversation.setTitle(title.trim());
        conversation.setUpdateTime(LocalDateTime.now());
        aiConversationMapper.updateById(conversation);
    }

    public void softDelete(Long userId, Long conversationId) {
        AiConversation conversation = requireConversation(userId, conversationId);
        conversation.setStatus("deleted");
        conversation.setUpdateTime(LocalDateTime.now());
        aiConversationMapper.updateById(conversation);
    }

    private AiConversation requireConversation(Long userId, Long conversationId) {
        AiConversation conversation = aiConversationMapper.selectOne(new LambdaQueryWrapper<AiConversation>()
                .eq(AiConversation::getId, conversationId)
                .eq(AiConversation::getUserId, userId)
                .eq(AiConversation::getStatus, "active")
                .last("LIMIT 1"));
        if (conversation == null) {
            throw new ResponseStatusException(NOT_FOUND, "会话不存在");
        }
        return conversation;
    }

    private AiConversationResponse toResponse(AiConversation conversation) {
        return new AiConversationResponse(
                conversation.getId(),
                conversation.getSceneId(),
                conversation.getChatMode(),
                conversation.getAgentId(),
                conversation.getTitle(),
                conversation.getCreateTime(),
                conversation.getUpdateTime()
        );
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

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
