package com.zhiyinhui.bosschat.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyinhui.bosschat.ai.dto.AiSceneAgentResponse;
import com.zhiyinhui.bosschat.ai.dto.AiSceneRequest;
import com.zhiyinhui.bosschat.ai.dto.AiSceneResponse;
import com.zhiyinhui.bosschat.ai.entity.AiAgent;
import com.zhiyinhui.bosschat.ai.entity.AiScene;
import com.zhiyinhui.bosschat.ai.entity.AiSceneAgent;
import com.zhiyinhui.bosschat.ai.mapper.AiAgentMapper;
import com.zhiyinhui.bosschat.ai.mapper.AiSceneAgentMapper;
import com.zhiyinhui.bosschat.ai.mapper.AiSceneMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class AiSceneService {

    public static final String MODE_SINGLE = "single";
    public static final String MODE_TEAM = "team";

    private final AiSceneMapper aiSceneMapper;
    private final AiSceneAgentMapper aiSceneAgentMapper;
    private final AiAgentMapper aiAgentMapper;

    public AiSceneService(
            AiSceneMapper aiSceneMapper,
            AiSceneAgentMapper aiSceneAgentMapper,
            AiAgentMapper aiAgentMapper
    ) {
        this.aiSceneMapper = aiSceneMapper;
        this.aiSceneAgentMapper = aiSceneAgentMapper;
        this.aiAgentMapper = aiAgentMapper;
    }

    public List<AiSceneResponse> listAll() {
        return aiSceneMapper.selectList(new LambdaQueryWrapper<AiScene>()
                        .orderByAsc(AiScene::getId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AiSceneResponse> listEnabled() {
        return aiSceneMapper.selectList(new LambdaQueryWrapper<AiScene>()
                        .eq(AiScene::getEnabled, 1)
                        .orderByAsc(AiScene::getId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public AiSceneResponse create(AiSceneRequest request) {
        String sceneCode = normalizeSceneCode(request.sceneCode(), request.sceneName());
        AiScene existing = aiSceneMapper.selectOne(new LambdaQueryWrapper<AiScene>()
                .eq(AiScene::getSceneCode, sceneCode)
                .last("LIMIT 1"));
        if (existing != null) {
            throw new ResponseStatusException(CONFLICT, "场景编码已存在");
        }

        AiScene scene = new AiScene();
        applyRequest(scene, request, sceneCode);
        aiSceneMapper.insert(scene);
        replaceSceneAgents(scene.getId(), request.agentIds());
        return toResponse(aiSceneMapper.selectById(scene.getId()));
    }

    @Transactional
    public AiSceneResponse update(Long sceneId, AiSceneRequest request) {
        AiScene scene = requireScene(sceneId);
        String sceneCode = normalizeSceneCode(
                request.sceneCode() == null || request.sceneCode().isBlank() ? scene.getSceneCode() : request.sceneCode(),
                request.sceneName()
        );
        AiScene sameCode = aiSceneMapper.selectOne(new LambdaQueryWrapper<AiScene>()
                .eq(AiScene::getSceneCode, sceneCode)
                .ne(AiScene::getId, sceneId)
                .last("LIMIT 1"));
        if (sameCode != null) {
            throw new ResponseStatusException(CONFLICT, "场景编码已存在");
        }
        applyRequest(scene, request, sceneCode);
        aiSceneMapper.updateById(scene);
        replaceSceneAgents(scene.getId(), request.agentIds());
        return toResponse(aiSceneMapper.selectById(sceneId));
    }

    public AiScene requireEnabledScene(Long sceneId) {
        AiScene scene = requireScene(sceneId);
        if (!isEnabled(scene.getEnabled())) {
            throw new ResponseStatusException(NOT_FOUND, "场景不存在");
        }
        return scene;
    }

    public AiAgent requireEnabledSceneAgent(Long sceneId, Long agentId) {
        if (agentId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "请选择 AI");
        }
        AiSceneAgent relation = aiSceneAgentMapper.selectOne(new LambdaQueryWrapper<AiSceneAgent>()
                .eq(AiSceneAgent::getSceneId, sceneId)
                .eq(AiSceneAgent::getAgentId, agentId)
                .eq(AiSceneAgent::getEnabled, 1)
                .last("LIMIT 1"));
        if (relation == null) {
            throw new ResponseStatusException(BAD_REQUEST, "当前 AI 不属于所选场景");
        }
        AiAgent agent = aiAgentMapper.selectById(agentId);
        if (agent == null || !isEnabled(agent.getEnabled())) {
            throw new ResponseStatusException(NOT_FOUND, "AI 不存在");
        }
        return agent;
    }

    public boolean isTeamMode(AiScene scene) {
        return MODE_TEAM.equalsIgnoreCase(scene.getChatMode());
    }

    public boolean isSingleMode(AiScene scene) {
        return !isTeamMode(scene);
    }

    private AiScene requireScene(Long sceneId) {
        AiScene scene = aiSceneMapper.selectById(sceneId);
        if (scene == null) {
            throw new ResponseStatusException(NOT_FOUND, "场景不存在");
        }
        return scene;
    }

    private void applyRequest(AiScene scene, AiSceneRequest request, String sceneCode) {
        scene.setSceneCode(sceneCode);
        scene.setSceneName(request.sceneName().trim());
        scene.setDescription(clean(request.description()));
        scene.setChatMode(normalizeMode(request.chatMode()));
        scene.setEnabled(request.enabled() == null || request.enabled() != 0 ? 1 : 0);
    }

    private void replaceSceneAgents(Long sceneId, List<Long> agentIds) {
        aiSceneAgentMapper.delete(new LambdaQueryWrapper<AiSceneAgent>()
                .eq(AiSceneAgent::getSceneId, sceneId));
        if (agentIds == null || agentIds.isEmpty()) {
            return;
        }
        Set<Long> uniqueAgentIds = new LinkedHashSet<>(agentIds);
        int index = 1;
        for (Long agentId : uniqueAgentIds) {
            AiAgent agent = aiAgentMapper.selectById(agentId);
            if (agent == null) {
                throw new ResponseStatusException(BAD_REQUEST, "无效 AI 编号：" + agentId);
            }
            AiSceneAgent relation = new AiSceneAgent();
            relation.setSceneId(sceneId);
            relation.setAgentId(agentId);
            relation.setRoleName(agent.getAgentName());
            relation.setSortOrder(index * 10);
            relation.setEnabled(1);
            aiSceneAgentMapper.insert(relation);
            index++;
        }
    }

    private AiSceneResponse toResponse(AiScene scene) {
        return new AiSceneResponse(
                scene.getId(),
                scene.getSceneCode(),
                scene.getSceneName(),
                scene.getDescription(),
                scene.getChatMode(),
                scene.getEnabled(),
                scene.getCreateTime(),
                scene.getUpdateTime(),
                listSceneAgents(scene.getId())
        );
    }

    private List<AiSceneAgentResponse> listSceneAgents(Long sceneId) {
        return aiSceneAgentMapper.selectList(new LambdaQueryWrapper<AiSceneAgent>()
                        .eq(AiSceneAgent::getSceneId, sceneId)
                        .orderByAsc(AiSceneAgent::getSortOrder)
                        .orderByAsc(AiSceneAgent::getId))
                .stream()
                .map(relation -> {
                    AiAgent agent = aiAgentMapper.selectById(relation.getAgentId());
                    return new AiSceneAgentResponse(
                            relation.getId(),
                            relation.getSceneId(),
                            relation.getAgentId(),
                            relation.getRoleName(),
                            relation.getSortOrder(),
                            relation.getEnabled(),
                            agent == null ? "" : agent.getAgentCode(),
                            agent == null ? "未知 AI" : agent.getAgentName(),
                            agent == null ? "" : agent.getDescription(),
                            agent == null ? 0 : agent.getToolsEnabled(),
                            agent == null ? 0 : agent.getImageGenerationEnabled()
                    );
                })
                .toList();
    }

    private String normalizeSceneCode(String code, String name) {
        String source = clean(code).isBlank() ? clean(name) : clean(code);
        String normalized = source.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");
        if (normalized.isBlank()) {
            normalized = "scene_" + System.currentTimeMillis();
        }
        return normalized;
    }

    private String normalizeMode(String value) {
        return MODE_TEAM.equalsIgnoreCase(clean(value)) ? MODE_TEAM : MODE_SINGLE;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isEnabled(Integer value) {
        return value != null && value == 1;
    }
}
