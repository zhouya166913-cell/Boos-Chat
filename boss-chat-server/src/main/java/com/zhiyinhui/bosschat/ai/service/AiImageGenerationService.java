package com.zhiyinhui.bosschat.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiyinhui.bosschat.ai.entity.AiAgent;
import com.zhiyinhui.bosschat.ai.entity.AiGeneratedImage;
import com.zhiyinhui.bosschat.ai.entity.AiImageStorageConfig;
import com.zhiyinhui.bosschat.ai.entity.AiModel;
import com.zhiyinhui.bosschat.ai.entity.AiModelApiKey;
import com.zhiyinhui.bosschat.ai.entity.AiModelProvider;
import com.zhiyinhui.bosschat.ai.mapper.AiGeneratedImageMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class AiImageGenerationService {

    private final AiModelManagementService aiModelManagementService;
    private final AiImageStorageConfigService imageStorageConfigService;
    private final AiGeneratedImageMapper generatedImageMapper;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AiImageGenerationService(
            AiModelManagementService aiModelManagementService,
            AiImageStorageConfigService imageStorageConfigService,
            AiGeneratedImageMapper generatedImageMapper,
            ObjectMapper objectMapper
    ) {
        this.aiModelManagementService = aiModelManagementService;
        this.imageStorageConfigService = imageStorageConfigService;
        this.generatedImageMapper = generatedImageMapper;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
    }

    public String generate(
            Long userId,
            AiAgent agent,
            Long conversationId,
            String prompt,
            String size,
            String quality
    ) {
        if (agent.getImageGenerationEnabled() == null || agent.getImageGenerationEnabled() != 1) {
            throw new ResponseStatusException(BAD_REQUEST, "当前 AI 未开启图片生成能力");
        }
        if (agent.getImageModelId() == null || agent.getImageApiKeyId() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "当前 AI 尚未配置图片生成模型 API");
        }
        String cleanPrompt = text(prompt);
        if (cleanPrompt.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "图片生成提示词不能为空");
        }

        AiModel imageModel = aiModelManagementService.requireEnabledModel(agent.getImageModelId());
        if (!"image_generation".equals(imageModel.getModelType())) {
            throw new ResponseStatusException(BAD_REQUEST, "当前绑定的模型不是图片生成模型");
        }
        AiModelProvider provider = aiModelManagementService.requireEnabledProvider(imageModel.getProviderId());
        AiModelApiKey apiKey = aiModelManagementService.requireEnabledApiKey(agent.getImageApiKeyId());
        if (!apiKey.getModelId().equals(imageModel.getId())) {
            throw new ResponseStatusException(BAD_REQUEST, "图片模型和 API Key 不属于同一个模型配置");
        }
        String plainApiKey = aiModelManagementService.decryptApiKey(apiKey);
        AiImageStorageConfig storageConfig = resolveStorageConfig(agent);

        AiGeneratedImage record = createPendingRecord(
                userId,
                agent,
                conversationId,
                provider,
                imageModel,
                apiKey,
                cleanPrompt,
                normalizeSize(size),
                storageConfig
        );

        try {
            String imageUrl = requestImage(provider, imageModel, plainApiKey, cleanPrompt, record.getImageSize(), quality);
            record.setSourceUrl(imageUrl);
            record.setObjectUrl(imageUrl);
            record.setLocalPath("");
            record.setStorageType("provider_url");
            record.setContextSummary("已生成图片：" + summarize(cleanPrompt, 80));
            record.setStatus("success");
            generatedImageMapper.updateById(record);
            return """
                    图片生成成功。
                    图片记录ID：%s
                    图片提示词：%s
                    请在最终回答中原样展示以下 Markdown 图片：
                    ![AI生成图片](%s)
                    """.formatted(record.getId(), cleanPrompt, imageUrl);
        } catch (ResponseStatusException exception) {
            markFailed(record, exception.getReason());
            throw exception;
        } catch (Exception exception) {
            markFailed(record, exception.getMessage());
            throw new ResponseStatusException(BAD_GATEWAY, "图片生成调用失败：" + failureMessage(exception));
        }
    }

    private AiGeneratedImage createPendingRecord(
            Long userId,
            AiAgent agent,
            Long conversationId,
            AiModelProvider provider,
            AiModel imageModel,
            AiModelApiKey apiKey,
            String prompt,
            String size,
            AiImageStorageConfig storageConfig
    ) {
        AiGeneratedImage record = new AiGeneratedImage();
        record.setUserId(userId);
        record.setAgentId(agent.getId());
        record.setConversationId(conversationId);
        record.setMessageId(null);
        record.setProviderId(provider.getId());
        record.setModelId(imageModel.getId());
        record.setApiKeyId(apiKey.getId());
        record.setPrompt(prompt);
        record.setNegativePrompt("");
        record.setImageSize(size);
        record.setImageCount(1);
        record.setStorageType(storageConfig == null ? agent.getImageStorageStrategy() : storageConfig.getStorageType());
        record.setSourceUrl("");
        record.setObjectUrl("");
        record.setLocalPath("");
        record.setContextSummary("图片生成中：" + summarize(prompt, 80));
        record.setStatus("pending");
        record.setErrorMessage("");
        generatedImageMapper.insert(record);
        return record;
    }

    private AiImageStorageConfig resolveStorageConfig(AiAgent agent) {
        if (agent.getImageStorageConfigId() == null) {
            return null;
        }
        return imageStorageConfigService.requireEnabledConfig(agent.getImageStorageConfigId());
    }

    private String requestImage(
            AiModelProvider provider,
            AiModel imageModel,
            String apiKey,
            String prompt,
            String size,
            String quality
    ) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", imageModel.getModelName());
        payload.put("prompt", prompt);
        payload.put("size", size);
        String normalizedQuality = normalizeQuality(provider, quality);
        if (!normalizedQuality.isBlank()) {
            payload.put("quality", normalizedQuality);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(joinUrl(provider.getBaseUrl(), imageModel.getApiPath(), "/images/generations")))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
        HttpResponse<String> response = sendWithRetry(request);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ResponseStatusException(BAD_GATEWAY, "图片模型服务调用失败：" + response.body());
        }

        Map<String, Object> body = objectMapper.readValue(response.body(), new TypeReference<>() {});
        List<Map<String, Object>> data = (List<Map<String, Object>>) body.getOrDefault("data", List.of());
        if (data.isEmpty()) {
            throw new ResponseStatusException(BAD_GATEWAY, "图片模型未返回图片数据");
        }
        String url = text(data.get(0).get("url"));
        if (url.isBlank()) {
            throw new ResponseStatusException(BAD_GATEWAY, "图片模型未返回图片地址");
        }
        return url;
    }

    private HttpResponse<String> sendWithRetry(HttpRequest request) throws Exception {
        Exception lastException = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException exception) {
                lastException = exception;
                if (!isRetryableNetworkError(exception) || attempt == 3) {
                    throw exception;
                }
                Thread.sleep(500L * attempt);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw exception;
            }
        }
        throw lastException == null ? new IOException("图片模型服务无响应") : lastException;
    }

    private boolean isRetryableNetworkError(Exception exception) {
        String message = failureMessage(exception).toLowerCase();
        return message.contains("connection reset")
                || message.contains("connection closed")
                || message.contains("premature")
                || message.contains("timed out")
                || message.contains("timeout");
    }

    private void markFailed(AiGeneratedImage record, String message) {
        record.setStatus("failed");
        record.setErrorMessage(message == null ? "图片生成失败" : message);
        generatedImageMapper.updateById(record);
    }

    private String failureMessage(Exception exception) {
        if (exception == null) {
            return "未知错误";
        }
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private String normalizeSize(String size) {
        String value = text(size);
        return value.isBlank() ? "1024x1024" : value;
    }

    private String normalizeQuality(AiModelProvider provider, String quality) {
        String value = text(quality);
        if (value.isBlank()) {
            return "";
        }
        if ("zhipu".equalsIgnoreCase(text(provider.getProviderCode()))) {
            return "";
        }
        return value;
    }

    private String trimTrailingSlash(String value) {
        String text = text(value);
        return text.endsWith("/") ? text.substring(0, text.length() - 1) : text;
    }

    private String joinUrl(String baseUrl, String apiPath, String fallbackPath) {
        String path = text(apiPath);
        if (path.isBlank()) {
            path = fallbackPath;
        }
        return trimTrailingSlash(baseUrl) + (path.startsWith("/") ? path : "/" + path);
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String summarize(String value, int maxLength) {
        String text = text(value);
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }
}
