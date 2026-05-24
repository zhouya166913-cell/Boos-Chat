package com.zhiyinhui.bosschat.ai.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhiyinhui.bosschat.ai.dto.AiModelApiKeyRequest;
import com.zhiyinhui.bosschat.ai.dto.AiModelApiKeyResponse;
import com.zhiyinhui.bosschat.ai.dto.AiModelProviderRequest;
import com.zhiyinhui.bosschat.ai.dto.AiModelProviderResponse;
import com.zhiyinhui.bosschat.ai.dto.AiModelRequest;
import com.zhiyinhui.bosschat.ai.dto.AiModelResponse;
import com.zhiyinhui.bosschat.ai.service.AiModelManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "模型统一管理")
@RestController
@RequestMapping("/api/admin/model-management")
public class AdminAiModelController {
    private final AiModelManagementService aiModelManagementService;

    public AdminAiModelController(AiModelManagementService aiModelManagementService) {
        this.aiModelManagementService = aiModelManagementService;
    }

    @Operation(summary = "查询模型供应商", description = "查询智谱、Kimi、DeepSeek 等模型供应商配置。")
    @SecurityRequirement(name = "Sa-Token")
    @GetMapping("/providers")
    public List<AiModelProviderResponse> listProviders() {
        StpUtil.checkRole("super_admin");
        return aiModelManagementService.listProviders();
    }

    @Operation(summary = "新增模型供应商", description = "新增一个可统一管理的大模型供应商。")
    @SecurityRequirement(name = "Sa-Token")
    @PostMapping("/providers")
    public AiModelProviderResponse createProvider(@Valid @RequestBody AiModelProviderRequest request) {
        StpUtil.checkRole("super_admin");
        return aiModelManagementService.createProvider(request);
    }

    @Operation(summary = "修改模型供应商", description = "修改模型供应商名称、Base URL、启用状态等配置。")
    @SecurityRequirement(name = "Sa-Token")
    @PutMapping("/providers/{providerId}")
    public AiModelProviderResponse updateProvider(
            @PathVariable Long providerId,
            @Valid @RequestBody AiModelProviderRequest request
    ) {
        StpUtil.checkRole("super_admin");
        return aiModelManagementService.updateProvider(providerId, request);
    }

    @Operation(summary = "查询模型列表", description = "查询所有供应商下已经登记的模型。")
    @SecurityRequirement(name = "Sa-Token")
    @GetMapping("/models")
    public List<AiModelResponse> listModels() {
        StpUtil.checkRole("super_admin");
        return aiModelManagementService.listModels();
    }

    @Operation(summary = "新增模型", description = "为某个供应商新增一个可调用模型。")
    @SecurityRequirement(name = "Sa-Token")
    @PostMapping("/models")
    public AiModelResponse createModel(@Valid @RequestBody AiModelRequest request) {
        StpUtil.checkRole("super_admin");
        return aiModelManagementService.createModel(request);
    }

    @Operation(summary = "修改模型", description = "修改模型名称、能力标记、启用状态等信息。")
    @SecurityRequirement(name = "Sa-Token")
    @PutMapping("/models/{modelId}")
    public AiModelResponse updateModel(@PathVariable Long modelId, @Valid @RequestBody AiModelRequest request) {
        StpUtil.checkRole("super_admin");
        return aiModelManagementService.updateModel(modelId, request);
    }

    @Operation(summary = "删除模型", description = "删除不再使用的模型。若模型仍被 API Key、智能体或生成记录引用，则需要先解除引用。")
    @SecurityRequirement(name = "Sa-Token")
    @DeleteMapping("/models/{modelId}")
    public void deleteModel(@PathVariable Long modelId) {
        StpUtil.checkRole("super_admin");
        aiModelManagementService.deleteModel(modelId);
    }

    @Operation(summary = "查询模型 API Key", description = "查询各个模型下的 API Key。接口只返回脱敏值，不返回明文 Key。")
    @SecurityRequirement(name = "Sa-Token")
    @GetMapping("/api-keys")
    public List<AiModelApiKeyResponse> listApiKeys() {
        StpUtil.checkRole("super_admin");
        return aiModelManagementService.listApiKeys();
    }

    @Operation(summary = "新增模型 API Key", description = "为某个具体模型新增 API Key。Key 会加密后保存到数据库。")
    @SecurityRequirement(name = "Sa-Token")
    @PostMapping("/api-keys")
    public AiModelApiKeyResponse createApiKey(@Valid @RequestBody AiModelApiKeyRequest request) {
        StpUtil.checkRole("super_admin");
        return aiModelManagementService.createApiKey(request);
    }

    @Operation(summary = "修改模型 API Key", description = "修改某个模型下的 API Key 名称、类型、优先级、启用状态。若不填写 Key 内容，则保留旧 Key。")
    @SecurityRequirement(name = "Sa-Token")
    @PutMapping("/api-keys/{apiKeyId}")
    public AiModelApiKeyResponse updateApiKey(
            @PathVariable Long apiKeyId,
            @Valid @RequestBody AiModelApiKeyRequest request
    ) {
        StpUtil.checkRole("super_admin");
        return aiModelManagementService.updateApiKey(apiKeyId, request);
    }

    @Operation(summary = "删除模型 API Key", description = "删除某个模型下不再使用的 API Key。")
    @SecurityRequirement(name = "Sa-Token")
    @DeleteMapping("/api-keys/{apiKeyId}")
    public void deleteApiKey(@PathVariable Long apiKeyId) {
        StpUtil.checkRole("super_admin");
        aiModelManagementService.deleteApiKey(apiKeyId);
    }
}
