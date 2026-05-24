package com.zhiyinhui.bosschat.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyinhui.bosschat.ai.dto.AiAgentRequest;
import com.zhiyinhui.bosschat.ai.dto.AiAgentResponse;
import com.zhiyinhui.bosschat.ai.entity.AiAgent;
import com.zhiyinhui.bosschat.ai.mapper.AiAgentMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class AiAgentService {

    private final AiAgentMapper aiAgentMapper;
    private final AiModelManagementService aiModelManagementService;
    private final AiImageStorageConfigService imageStorageConfigService;

    public AiAgentService(
            AiAgentMapper aiAgentMapper,
            AiModelManagementService aiModelManagementService,
            AiImageStorageConfigService imageStorageConfigService
    ) {
        this.aiAgentMapper = aiAgentMapper;
        this.aiModelManagementService = aiModelManagementService;
        this.imageStorageConfigService = imageStorageConfigService;
    }

    public List<AiAgentResponse> listAll() {
        return aiAgentMapper.selectList(new LambdaQueryWrapper<AiAgent>()
                        .orderByAsc(AiAgent::getId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AiAgentResponse> listEnabled() {
        return aiAgentMapper.selectList(new LambdaQueryWrapper<AiAgent>()
                        .eq(AiAgent::getEnabled, 1)
                        .orderByAsc(AiAgent::getId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AiAgentResponse> listEnabledToolAgents() {
        return aiAgentMapper.selectList(new LambdaQueryWrapper<AiAgent>()
                        .eq(AiAgent::getEnabled, 1)
                        .eq(AiAgent::getToolsEnabled, 1)
                        .orderByAsc(AiAgent::getId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AiAgentResponse create(AiAgentRequest request) {
        AiAgent existing = aiAgentMapper.selectOne(new LambdaQueryWrapper<AiAgent>()
                .eq(AiAgent::getAgentCode, request.agentCode().trim())
                .last("LIMIT 1"));
        if (existing != null) {
            throw new ResponseStatusException(CONFLICT, "智能体编码已存在");
        }
        AiAgent agent = new AiAgent();
        applyRequest(agent, request);
        aiAgentMapper.insert(agent);
        return toResponse(aiAgentMapper.selectById(agent.getId()));
    }

    public AiAgentResponse update(Long agentId, AiAgentRequest request) {
        AiAgent agent = requireAgent(agentId);
        AiAgent sameCode = aiAgentMapper.selectOne(new LambdaQueryWrapper<AiAgent>()
                .eq(AiAgent::getAgentCode, request.agentCode().trim())
                .ne(AiAgent::getId, agentId)
                .last("LIMIT 1"));
        if (sameCode != null) {
            throw new ResponseStatusException(CONFLICT, "智能体编码已存在");
        }
        applyRequest(agent, request);
        aiAgentMapper.updateById(agent);
        return toResponse(aiAgentMapper.selectById(agentId));
    }

    public AiAgent requireEnabledAgent(Long agentId) {
        AiAgent agent = requireAgent(agentId);
        if (agent.getEnabled() == null || agent.getEnabled() != 1) {
            throw new ResponseStatusException(NOT_FOUND, "智能体不可用");
        }
        return agent;
    }

    public AiAgent requireExistingAgent(Long agentId) {
        return requireAgent(agentId);
    }

    private AiAgent requireAgent(Long agentId) {
        AiAgent agent = aiAgentMapper.selectById(agentId);
        if (agent == null) {
            throw new ResponseStatusException(NOT_FOUND, "智能体不存在");
        }
        return agent;
    }

    private void applyRequest(AiAgent agent, AiAgentRequest request) {
        agent.setAgentCode(request.agentCode().trim());
        agent.setAgentName(request.agentName().trim());
        agent.setDescription(clean(request.description()));
        agent.setSystemPrompt(request.systemPrompt().trim());
        agent.setModelProvider(clean(request.modelProvider()));
        agent.setModelName(clean(request.modelName()));
        if (request.modelId() != null) {
            var model = aiModelManagementService.requireEnabledModel(request.modelId());
            if ("image_generation".equals(model.getModelType())) {
                throw new ResponseStatusException(BAD_REQUEST, "对话模型不能选择图片生成模型");
            }
        }
        if (request.apiKeyId() != null) {
            aiModelManagementService.requireEnabledApiKey(request.apiKeyId());
        }
        agent.setModelId(request.modelId());
        agent.setApiKeyId(request.apiKeyId());
        agent.setTemperature(defaultTemperature(request.temperature()));
        agent.setMaxCompletionTokens(defaultMaxTokens(request.maxCompletionTokens()));
        agent.setMemoryEnabled(defaultSwitch(request.memoryEnabled(), 0));
        agent.setKnowledgeEnabled(defaultSwitch(request.knowledgeEnabled(), 0));
        agent.setWorkflowEnabled(defaultSwitch(request.workflowEnabled(), 0));
        agent.setToolsEnabled(defaultSwitch(request.toolsEnabled(), 0));
        applyImageGenerationConfig(agent, request);
        agent.setEnabled(request.enabled() == null || request.enabled() != 0 ? 1 : 0);
    }

    private void applyImageGenerationConfig(AiAgent agent, AiAgentRequest request) {
        Integer imageEnabled = defaultSwitch(request.imageGenerationEnabled(), 0);
        agent.setImageGenerationEnabled(imageEnabled);
        agent.setImageStorageStrategy(clean(request.imageStorageStrategy(), "local"));

        if (imageEnabled == 0) {
            agent.setImageModelId(null);
            agent.setImageApiKeyId(null);
            agent.setImageStorageConfigId(null);
            return;
        }

        if (request.imageModelId() == null || request.imageApiKeyId() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "开启图片生成能力时必须选择图片模型 API");
        }

        var imageModel = aiModelManagementService.requireEnabledModel(request.imageModelId());
        if (!"image_generation".equals(imageModel.getModelType())) {
            throw new ResponseStatusException(BAD_REQUEST, "图片生成能力只能绑定图片生成模型");
        }

        var imageApiKey = aiModelManagementService.requireEnabledApiKey(request.imageApiKeyId());
        if (!imageApiKey.getModelId().equals(imageModel.getId())) {
            throw new ResponseStatusException(BAD_REQUEST, "图片 API Key 不属于所选图片模型");
        }

        agent.setImageModelId(imageModel.getId());
        agent.setImageApiKeyId(imageApiKey.getId());

        if (request.imageStorageConfigId() != null) {
            var storageConfig = imageStorageConfigService.requireEnabledConfig(request.imageStorageConfigId());
            agent.setImageStorageConfigId(storageConfig.getId());
            agent.setImageStorageStrategy(storageConfig.getStorageType());
        } else {
            agent.setImageStorageConfigId(null);
        }
    }

    private AiAgentResponse toResponse(AiAgent agent) {
        return new AiAgentResponse(
                agent.getId(),
                agent.getAgentCode(),
                agent.getAgentName(),
                agent.getDescription(),
                agent.getSystemPrompt(),
                agent.getModelProvider(),
                agent.getModelName(),
                agent.getModelId(),
                agent.getApiKeyId(),
                agent.getTemperature(),
                agent.getMaxCompletionTokens(),
                agent.getMemoryEnabled(),
                agent.getKnowledgeEnabled(),
                agent.getWorkflowEnabled(),
                agent.getToolsEnabled(),
                agent.getImageGenerationEnabled(),
                agent.getImageModelId(),
                agent.getImageApiKeyId(),
                agent.getImageStorageStrategy(),
                agent.getImageStorageConfigId(),
                agent.getEnabled()
        );
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private BigDecimal defaultTemperature(BigDecimal value) {
        return value == null ? BigDecimal.valueOf(0.35) : value;
    }

    private Integer defaultMaxTokens(Integer value) {
        return value == null || value < 1 ? 4096 : value;
    }

    private Integer defaultSwitch(Integer value, Integer fallback) {
        return value == null ? fallback : value == 0 ? 0 : 1;
    }
}
