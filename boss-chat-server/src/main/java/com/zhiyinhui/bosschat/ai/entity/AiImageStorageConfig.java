package com.zhiyinhui.bosschat.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("ai_image_storage_config")
public class AiImageStorageConfig {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String storageCode;
    private String storageName;
    private String storageType;
    private String endpoint;
    private String region;
    private String bucketName;
    private String baseUrl;
    private String rootPath;
    private String extraConfigJson;
    private String accessKeyIdCipher;
    private String accessKeyIdMask;
    private String accessKeySecretCipher;
    private String accessKeySecretMask;
    private Integer enabled;
    private Integer isDefault;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getStorageCode() { return storageCode; }
    public void setStorageCode(String storageCode) { this.storageCode = storageCode; }
    public String getStorageName() { return storageName; }
    public void setStorageName(String storageName) { this.storageName = storageName; }
    public String getStorageType() { return storageType; }
    public void setStorageType(String storageType) { this.storageType = storageType; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getBucketName() { return bucketName; }
    public void setBucketName(String bucketName) { this.bucketName = bucketName; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getRootPath() { return rootPath; }
    public void setRootPath(String rootPath) { this.rootPath = rootPath; }
    public String getExtraConfigJson() { return extraConfigJson; }
    public void setExtraConfigJson(String extraConfigJson) { this.extraConfigJson = extraConfigJson; }
    public String getAccessKeyIdCipher() { return accessKeyIdCipher; }
    public void setAccessKeyIdCipher(String accessKeyIdCipher) { this.accessKeyIdCipher = accessKeyIdCipher; }
    public String getAccessKeyIdMask() { return accessKeyIdMask; }
    public void setAccessKeyIdMask(String accessKeyIdMask) { this.accessKeyIdMask = accessKeyIdMask; }
    public String getAccessKeySecretCipher() { return accessKeySecretCipher; }
    public void setAccessKeySecretCipher(String accessKeySecretCipher) { this.accessKeySecretCipher = accessKeySecretCipher; }
    public String getAccessKeySecretMask() { return accessKeySecretMask; }
    public void setAccessKeySecretMask(String accessKeySecretMask) { this.accessKeySecretMask = accessKeySecretMask; }
    public Integer getEnabled() { return enabled; }
    public void setEnabled(Integer enabled) { this.enabled = enabled; }
    public Integer getIsDefault() { return isDefault; }
    public void setIsDefault(Integer isDefault) { this.isDefault = isDefault; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
