package com.zhiyinhui.bosschat.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyinhui.bosschat.ai.dto.AiWorkflowRequest;
import com.zhiyinhui.bosschat.ai.dto.AiWorkflowResponse;
import com.zhiyinhui.bosschat.ai.entity.AiWorkflow;
import com.zhiyinhui.bosschat.ai.mapper.AiWorkflowMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class AiWorkflowService {

    private final AiWorkflowMapper aiWorkflowMapper;

    public AiWorkflowService(AiWorkflowMapper aiWorkflowMapper) {
        this.aiWorkflowMapper = aiWorkflowMapper;
    }

    public List<AiWorkflowResponse> listByAgent(Long agentId) {
        return aiWorkflowMapper.selectList(new LambdaQueryWrapper<AiWorkflow>()
                        .eq(AiWorkflow::getAgentId, agentId)
                        .orderByAsc(AiWorkflow::getId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<AiWorkflowResponse> listEnabledByAgent(Long agentId) {
        return aiWorkflowMapper.selectList(new LambdaQueryWrapper<AiWorkflow>()
                        .eq(AiWorkflow::getAgentId, agentId)
                        .eq(AiWorkflow::getEnabled, 1)
                        .orderByAsc(AiWorkflow::getId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AiWorkflowResponse createForAgent(Long agentId, AiWorkflowRequest request) {
        ensureWorkflowCodeAvailable(agentId, null, request.workflowCode().trim());
        AiWorkflow workflow = new AiWorkflow();
        workflow.setAgentId(agentId);
        applyRequest(workflow, request);
        aiWorkflowMapper.insert(workflow);
        return toResponse(workflow);
    }

    public AiWorkflowResponse updateForAgent(Long agentId, Long workflowId, AiWorkflowRequest request) {
        AiWorkflow workflow = findAgentWorkflow(agentId, workflowId);
        ensureWorkflowCodeAvailable(agentId, workflowId, request.workflowCode().trim());
        applyRequest(workflow, request);
        aiWorkflowMapper.updateById(workflow);
        return toResponse(workflow);
    }

    public void deleteForAgent(Long agentId, Long workflowId) {
        AiWorkflow workflow = findAgentWorkflow(agentId, workflowId);
        aiWorkflowMapper.deleteById(workflow.getId());
    }

    public String buildWorkflowContext(Long agentId, String workflowCode) {
        if (workflowCode == null || workflowCode.isBlank()) {
            return "";
        }
        AiWorkflow workflow = aiWorkflowMapper.selectOne(new LambdaQueryWrapper<AiWorkflow>()
                .eq(AiWorkflow::getAgentId, agentId)
                .eq(AiWorkflow::getWorkflowCode, workflowCode.trim())
                .eq(AiWorkflow::getEnabled, 1)
                .last("LIMIT 1"));
        if (workflow == null) {
            throw new ResponseStatusException(NOT_FOUND, "工作流不存在或不属于当前智能体");
        }
        return """
                当前任务需要遵循以下工作流：
                名称：%s
                说明：%s
                定义：%s
                """.formatted(workflow.getWorkflowName(), workflow.getDescription(), workflow.getDefinitionJson());
    }

    private void applyRequest(AiWorkflow workflow, AiWorkflowRequest request) {
        workflow.setWorkflowCode(request.workflowCode().trim());
        workflow.setWorkflowName(request.workflowName().trim());
        workflow.setDescription(clean(request.description()));
        workflow.setDefinitionJson(request.definitionJson().trim());
        workflow.setEnabled(request.enabled() == null || request.enabled() != 0 ? 1 : 0);
    }

    private AiWorkflow findAgentWorkflow(Long agentId, Long workflowId) {
        AiWorkflow workflow = aiWorkflowMapper.selectOne(new LambdaQueryWrapper<AiWorkflow>()
                .eq(AiWorkflow::getId, workflowId)
                .eq(AiWorkflow::getAgentId, agentId)
                .last("LIMIT 1"));
        if (workflow == null) {
            throw new ResponseStatusException(NOT_FOUND, "工作流不存在");
        }
        return workflow;
    }

    private void ensureWorkflowCodeAvailable(Long agentId, Long currentWorkflowId, String workflowCode) {
        LambdaQueryWrapper<AiWorkflow> wrapper = new LambdaQueryWrapper<AiWorkflow>()
                .eq(AiWorkflow::getAgentId, agentId)
                .eq(AiWorkflow::getWorkflowCode, workflowCode);
        if (currentWorkflowId != null) {
            wrapper.ne(AiWorkflow::getId, currentWorkflowId);
        }
        AiWorkflow duplicate = aiWorkflowMapper.selectOne(wrapper.last("LIMIT 1"));
        if (duplicate != null) {
            throw new ResponseStatusException(BAD_REQUEST, "同一智能体下工作流编码已存在");
        }
    }

    private AiWorkflowResponse toResponse(AiWorkflow workflow) {
        return new AiWorkflowResponse(
                workflow.getId(),
                workflow.getAgentId(),
                workflow.getWorkflowCode(),
                workflow.getWorkflowName(),
                workflow.getDescription(),
                workflow.getDefinitionJson(),
                workflow.getEnabled()
        );
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
