package com.zhiyinhui.bosschat.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record AiImageStorageRequest(
        @NotBlank(message = "存储编码不能为空") String storageCode,
        @NotBlank(message = "存储名称不能为空") String storageName,
        @NotBlank(message = "存储类型不能为空") String storageType,
        String endpoint,
        String region,
        String bucketName,
        String baseUrl,
        String rootPath,
        String extraConfigJson,
        String accessKeyId,
        String accessKeySecret,
        Integer enabled,
        Integer isDefault,
        String remark
) {
}
