package com.zhiyinhui.bosschat.ai.dto;

public record AiImageStorageResponse(
        Long id,
        String storageCode,
        String storageName,
        String storageType,
        String endpoint,
        String region,
        String bucketName,
        String baseUrl,
        String rootPath,
        String extraConfigJson,
        String accessKeyIdMask,
        String accessKeySecretMask,
        Integer enabled,
        Integer isDefault,
        String remark
) {
}
