package com.zhiyinhui.bosschat.ai.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhiyinhui.bosschat.ai.dto.AiImageStorageRequest;
import com.zhiyinhui.bosschat.ai.dto.AiImageStorageResponse;
import com.zhiyinhui.bosschat.ai.dto.AiImageStorageValidationResponse;
import com.zhiyinhui.bosschat.ai.service.AiImageStorageConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "图片存储管理")
@RestController
@RequestMapping("/api/admin/image-storage")
public class AdminAiImageStorageController {

    private final AiImageStorageConfigService storageConfigService;

    public AdminAiImageStorageController(AiImageStorageConfigService storageConfigService) {
        this.storageConfigService = storageConfigService;
    }

    @Operation(summary = "查询图片存储配置", description = "查询本地存储、图床、OSS、COS、七牛、S3 等图片存储配置。")
    @SecurityRequirement(name = "Sa-Token")
    @GetMapping
    public List<AiImageStorageResponse> list() {
        StpUtil.checkRole("super_admin");
        return storageConfigService.list();
    }

    @Operation(summary = "新增图片存储配置", description = "新增一个图片存储或图床配置，AccessKey 会加密保存。")
    @SecurityRequirement(name = "Sa-Token")
    @PostMapping
    public AiImageStorageResponse create(@Valid @RequestBody AiImageStorageRequest request) {
        StpUtil.checkRole("super_admin");
        return storageConfigService.create(request);
    }

    @Operation(summary = "真实验证新增图片存储配置", description = "会向云存储上传一个临时小文件、访问公开 URL，并尽量删除临时文件；可能产生云存储请求费用。")
    @SecurityRequirement(name = "Sa-Token")
    @PostMapping("/validate")
    public AiImageStorageValidationResponse validateCreate(@Valid @RequestBody AiImageStorageRequest request) {
        StpUtil.checkRole("super_admin");
        return storageConfigService.validate(request);
    }

    @Operation(summary = "修改图片存储配置", description = "修改图片存储或图床配置；若密钥字段留空，则保留原密钥。")
    @SecurityRequirement(name = "Sa-Token")
    @PutMapping("/{storageId}")
    public AiImageStorageResponse update(
            @PathVariable Long storageId,
            @Valid @RequestBody AiImageStorageRequest request
    ) {
        StpUtil.checkRole("super_admin");
        return storageConfigService.update(storageId, request);
    }

    @Operation(summary = "真实验证修改后的图片存储配置", description = "会向云存储上传一个临时小文件、访问公开 URL，并尽量删除临时文件；可能产生云存储请求费用。")
    @SecurityRequirement(name = "Sa-Token")
    @PostMapping("/{storageId}/validate")
    public AiImageStorageValidationResponse validateUpdate(
            @PathVariable Long storageId,
            @Valid @RequestBody AiImageStorageRequest request
    ) {
        StpUtil.checkRole("super_admin");
        return storageConfigService.validate(storageId, request);
    }
}
