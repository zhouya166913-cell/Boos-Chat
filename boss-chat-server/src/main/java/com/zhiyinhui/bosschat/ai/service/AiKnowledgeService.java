package com.zhiyinhui.bosschat.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyinhui.bosschat.ai.dto.AiKnowledgeDocumentRequest;
import com.zhiyinhui.bosschat.ai.dto.AiKnowledgeDocumentResponse;
import com.zhiyinhui.bosschat.ai.entity.AiKnowledgeDocument;
import com.zhiyinhui.bosschat.ai.mapper.AiKnowledgeDocumentMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class AiKnowledgeService {

    private final AiKnowledgeDocumentMapper aiKnowledgeDocumentMapper;

    public AiKnowledgeService(AiKnowledgeDocumentMapper aiKnowledgeDocumentMapper) {
        this.aiKnowledgeDocumentMapper = aiKnowledgeDocumentMapper;
    }

    public List<AiKnowledgeDocumentResponse> listByAgent(Long agentId) {
        return aiKnowledgeDocumentMapper.selectList(new LambdaQueryWrapper<AiKnowledgeDocument>()
                        .eq(AiKnowledgeDocument::getAgentId, agentId)
                        .orderByDesc(AiKnowledgeDocument::getId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AiKnowledgeDocumentResponse createForAgent(Long agentId, AiKnowledgeDocumentRequest request) {
        AiKnowledgeDocument document = new AiKnowledgeDocument();
        document.setAgentId(agentId);
        applyRequest(document, request);
        aiKnowledgeDocumentMapper.insert(document);
        return toResponse(document);
    }

    public AiKnowledgeDocumentResponse updateForAgent(Long agentId, Long documentId, AiKnowledgeDocumentRequest request) {
        AiKnowledgeDocument document = findAgentDocument(agentId, documentId);
        applyRequest(document, request);
        aiKnowledgeDocumentMapper.updateById(document);
        return toResponse(document);
    }

    public void deleteForAgent(Long agentId, Long documentId) {
        AiKnowledgeDocument document = findAgentDocument(agentId, documentId);
        aiKnowledgeDocumentMapper.deleteById(document.getId());
    }

    public String buildKnowledgeContext(Long agentId, String query) {
        List<AiKnowledgeDocument> documents = aiKnowledgeDocumentMapper.selectList(
                new LambdaQueryWrapper<AiKnowledgeDocument>()
                        .eq(AiKnowledgeDocument::getAgentId, agentId)
                        .eq(AiKnowledgeDocument::getEnabled, 1)
                        .orderByDesc(AiKnowledgeDocument::getId)
        );
        if (documents.isEmpty()) {
            return "";
        }
        String normalizedQuery = query == null ? "" : query.toLowerCase();
        List<AiKnowledgeDocument> selected = documents.stream()
                .filter(document -> matches(document, normalizedQuery))
                .limit(3)
                .toList();
        if (selected.isEmpty()) {
            selected = documents.stream().limit(2).toList();
        }
        StringBuilder builder = new StringBuilder("以下是当前智能体可参考的知识库内容：\n");
        selected.forEach(document -> builder
                .append("## ")
                .append(document.getTitle())
                .append("\n")
                .append(document.getContent())
                .append("\n\n"));
        return builder.toString();
    }

    private void applyRequest(AiKnowledgeDocument document, AiKnowledgeDocumentRequest request) {
        document.setTitle(request.title().trim());
        document.setContent(request.content().trim());
        document.setTags(clean(request.tags()));
        document.setEnabled(request.enabled() == null || request.enabled() != 0 ? 1 : 0);
    }

    private boolean matches(AiKnowledgeDocument document, String query) {
        if (query.isBlank()) {
            return false;
        }
        return text(document.getTitle()).contains(query)
                || text(document.getTags()).contains(query)
                || text(document.getContent()).contains(query);
    }

    private String text(String value) {
        return value == null ? "" : value.toLowerCase();
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private AiKnowledgeDocument findAgentDocument(Long agentId, Long documentId) {
        AiKnowledgeDocument document = aiKnowledgeDocumentMapper.selectOne(new LambdaQueryWrapper<AiKnowledgeDocument>()
                .eq(AiKnowledgeDocument::getId, documentId)
                .eq(AiKnowledgeDocument::getAgentId, agentId)
                .last("LIMIT 1"));
        if (document == null) {
            throw new ResponseStatusException(NOT_FOUND, "知识库文档不存在");
        }
        return document;
    }

    private AiKnowledgeDocumentResponse toResponse(AiKnowledgeDocument document) {
        return new AiKnowledgeDocumentResponse(
                document.getId(),
                document.getAgentId(),
                document.getTitle(),
                document.getContent(),
                document.getTags(),
                document.getEnabled()
        );
    }
}