package com.zhiyinhui.bosschat.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyinhui.bosschat.ai.dto.AiMemoryRequest;
import com.zhiyinhui.bosschat.ai.dto.AiMemoryResponse;
import com.zhiyinhui.bosschat.ai.entity.AiMemory;
import com.zhiyinhui.bosschat.ai.mapper.AiMemoryMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class AiMemoryService {

    private final AiMemoryMapper aiMemoryMapper;

    public AiMemoryService(AiMemoryMapper aiMemoryMapper) {
        this.aiMemoryMapper = aiMemoryMapper;
    }

    public List<AiMemoryResponse> listByAgent(Long agentId) {
        return aiMemoryMapper.selectList(new LambdaQueryWrapper<AiMemory>()
                        .eq(AiMemory::getAgentId, agentId)
                        .orderByAsc(AiMemory::getMemoryKey))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AiMemoryResponse saveForAgent(Long agentId, AiMemoryRequest request) {
        String memoryKey = request.memoryKey().trim();
        AiMemory memory = aiMemoryMapper.selectOne(new LambdaQueryWrapper<AiMemory>()
                .eq(AiMemory::getAgentId, agentId)
                .eq(AiMemory::getMemoryKey, memoryKey)
                .last("LIMIT 1"));
        if (memory == null) {
            ensureMemoryKeyAvailable(agentId, null, memoryKey);
            memory = new AiMemory();
            memory.setAgentId(agentId);
            memory.setMemoryKey(memoryKey);
        }
        memory.setMemoryType(clean(request.memoryType(), "profile"));
        memory.setMemoryValue(request.memoryValue().trim());
        memory.setEnabled(request.enabled() == null || request.enabled() != 0 ? 1 : 0);
        if (memory.getId() == null) {
            aiMemoryMapper.insert(memory);
        } else {
            aiMemoryMapper.updateById(memory);
        }
        return toResponse(memory);
    }

    public AiMemoryResponse updateForAgent(Long agentId, Long memoryId, AiMemoryRequest request) {
        AiMemory memory = findAgentMemory(agentId, memoryId);
        String memoryKey = request.memoryKey().trim();
        ensureMemoryKeyAvailable(agentId, memoryId, memoryKey);
        memory.setMemoryKey(memoryKey);
        memory.setMemoryType(clean(request.memoryType(), "profile"));
        memory.setMemoryValue(request.memoryValue().trim());
        memory.setEnabled(request.enabled() == null || request.enabled() != 0 ? 1 : 0);
        aiMemoryMapper.updateById(memory);
        return toResponse(memory);
    }

    public void deleteForAgent(Long agentId, Long memoryId) {
        AiMemory memory = findAgentMemory(agentId, memoryId);
        aiMemoryMapper.deleteById(memory.getId());
    }

    public String buildMemoryContext(Long agentId) {
        List<AiMemory> memories = aiMemoryMapper.selectList(new LambdaQueryWrapper<AiMemory>()
                .eq(AiMemory::getAgentId, agentId)
                .eq(AiMemory::getEnabled, 1)
                .orderByAsc(AiMemory::getMemoryKey));
        if (memories.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder("以下是当前智能体的长期记忆：\n");
        memories.forEach(memory -> builder
                .append("- ")
                .append(memory.getMemoryKey())
                .append(": ")
                .append(memory.getMemoryValue())
                .append("\n"));
        return builder.toString();
    }

    private AiMemoryResponse toResponse(AiMemory memory) {
        return new AiMemoryResponse(
                memory.getId(),
                memory.getAgentId(),
                memory.getMemoryType(),
                memory.getMemoryKey(),
                memory.getMemoryValue(),
                memory.getEnabled()
        );
    }

    private AiMemory findAgentMemory(Long agentId, Long memoryId) {
        AiMemory memory = aiMemoryMapper.selectOne(new LambdaQueryWrapper<AiMemory>()
                .eq(AiMemory::getId, memoryId)
                .eq(AiMemory::getAgentId, agentId)
                .last("LIMIT 1"));
        if (memory == null) {
            throw new ResponseStatusException(NOT_FOUND, "长期记忆不存在");
        }
        return memory;
    }

    private void ensureMemoryKeyAvailable(Long agentId, Long currentMemoryId, String memoryKey) {
        LambdaQueryWrapper<AiMemory> wrapper = new LambdaQueryWrapper<AiMemory>()
                .eq(AiMemory::getAgentId, agentId)
                .eq(AiMemory::getMemoryKey, memoryKey);
        if (currentMemoryId != null) {
            wrapper.ne(AiMemory::getId, currentMemoryId);
        }
        AiMemory duplicate = aiMemoryMapper.selectOne(wrapper.last("LIMIT 1"));
        if (duplicate != null) {
            throw new ResponseStatusException(BAD_REQUEST, "同一智能体下记忆键已存在");
        }
    }

    private String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}