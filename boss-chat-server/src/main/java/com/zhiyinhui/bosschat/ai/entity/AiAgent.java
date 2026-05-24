package com.zhiyinhui.bosschat.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("ai_agent")
public class AiAgent {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String agentCode;
    private String agentName;
    private String description;
    private String systemPrompt;
    private String modelProvider;
    private String modelName;
    private Long modelId;
    private Long apiKeyId;
    private BigDecimal temperature;
    private Integer maxCompletionTokens;
    private Integer memoryEnabled;
    private Integer knowledgeEnabled;
    private Integer workflowEnabled;
    private Integer toolsEnabled;
    private Integer imageGenerationEnabled;
    private Long imageModelId;
    private Long imageApiKeyId;
    private String imageStorageStrategy;
    private Long imageStorageConfigId;
    private Integer enabled;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAgentCode() { return agentCode; }
    public void setAgentCode(String agentCode) { this.agentCode = agentCode; }
    public String getAgentName() { return agentName; }
    public void setAgentName(String agentName) { this.agentName = agentName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    public String getModelProvider() { return modelProvider; }
    public void setModelProvider(String modelProvider) { this.modelProvider = modelProvider; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public Long getModelId() { return modelId; }
    public void setModelId(Long modelId) { this.modelId = modelId; }
    public Long getApiKeyId() { return apiKeyId; }
    public void setApiKeyId(Long apiKeyId) { this.apiKeyId = apiKeyId; }
    public BigDecimal getTemperature() { return temperature; }
    public void setTemperature(BigDecimal temperature) { this.temperature = temperature; }
    public Integer getMaxCompletionTokens() { return maxCompletionTokens; }
    public void setMaxCompletionTokens(Integer maxCompletionTokens) { this.maxCompletionTokens = maxCompletionTokens; }
    public Integer getMemoryEnabled() { return memoryEnabled; }
    public void setMemoryEnabled(Integer memoryEnabled) { this.memoryEnabled = memoryEnabled; }
    public Integer getKnowledgeEnabled() { return knowledgeEnabled; }
    public void setKnowledgeEnabled(Integer knowledgeEnabled) { this.knowledgeEnabled = knowledgeEnabled; }
    public Integer getWorkflowEnabled() { return workflowEnabled; }
    public void setWorkflowEnabled(Integer workflowEnabled) { this.workflowEnabled = workflowEnabled; }
    public Integer getToolsEnabled() { return toolsEnabled; }
    public void setToolsEnabled(Integer toolsEnabled) { this.toolsEnabled = toolsEnabled; }
    public Integer getImageGenerationEnabled() { return imageGenerationEnabled; }
    public void setImageGenerationEnabled(Integer imageGenerationEnabled) { this.imageGenerationEnabled = imageGenerationEnabled; }
    public Long getImageModelId() { return imageModelId; }
    public void setImageModelId(Long imageModelId) { this.imageModelId = imageModelId; }
    public Long getImageApiKeyId() { return imageApiKeyId; }
    public void setImageApiKeyId(Long imageApiKeyId) { this.imageApiKeyId = imageApiKeyId; }
    public String getImageStorageStrategy() { return imageStorageStrategy; }
    public void setImageStorageStrategy(String imageStorageStrategy) { this.imageStorageStrategy = imageStorageStrategy; }
    public Long getImageStorageConfigId() { return imageStorageConfigId; }
    public void setImageStorageConfigId(Long imageStorageConfigId) { this.imageStorageConfigId = imageStorageConfigId; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
