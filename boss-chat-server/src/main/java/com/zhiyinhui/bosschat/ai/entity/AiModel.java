package com.zhiyinhui.bosschat.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("ai_model")
public class AiModel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long providerId;
    private String modelName;
    private String displayName;
    private String modelType;
    private String apiPath;
    private String billingType;
    private String officialDocUrl;
    private String compatibilityProfile;
    private Integer contextWindow;
    private Integer supportsStream;
    private Integer supportsTools;
    private Integer supportsVision;
    private Integer enabled;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProviderId() { return providerId; }
    public void setProviderId(Long providerId) { this.providerId = providerId; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getModelType() { return modelType; }
    public void setModelType(String modelType) { this.modelType = modelType; }
    public String getApiPath() { return apiPath; }
    public void setApiPath(String apiPath) { this.apiPath = apiPath; }
    public String getBillingType() { return billingType; }
    public void setBillingType(String billingType) { this.billingType = billingType; }
    public String getOfficialDocUrl() { return officialDocUrl; }
    public void setOfficialDocUrl(String officialDocUrl) { this.officialDocUrl = officialDocUrl; }
    public String getCompatibilityProfile() { return compatibilityProfile; }
    public void setCompatibilityProfile(String compatibilityProfile) { this.compatibilityProfile = compatibilityProfile; }
    public Integer getContextWindow() { return contextWindow; }
    public void setContextWindow(Integer contextWindow) { this.contextWindow = contextWindow; }
    public Integer getSupportsStream() { return supportsStream; }
    public void setSupportsStream(Integer supportsStream) { this.supportsStream = supportsStream; }
    public Integer getSupportsTools() { return supportsTools; }
    public void setSupportsTools(Integer supportsTools) { this.supportsTools = supportsTools; }
    public Integer getSupportsVision() { return supportsVision; }
    public void setSupportsVision(Integer supportsVision) { this.supportsVision = supportsVision; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
