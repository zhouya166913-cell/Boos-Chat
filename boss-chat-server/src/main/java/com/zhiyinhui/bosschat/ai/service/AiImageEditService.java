package com.zhiyinhui.bosschat.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiyinhui.bosschat.ai.entity.AiAgent;
import com.zhiyinhui.bosschat.ai.entity.AiGeneratedImage;
import com.zhiyinhui.bosschat.ai.entity.AiModel;
import com.zhiyinhui.bosschat.ai.entity.AiModelApiKey;
import com.zhiyinhui.bosschat.ai.entity.AiModelProvider;
import com.zhiyinhui.bosschat.ai.mapper.AiGeneratedImageMapper;
import com.zhiyinhui.bosschat.ai.mapper.AiModelApiKeyMapper;
import com.zhiyinhui.bosschat.ai.mapper.AiModelMapper;
import com.zhiyinhui.bosschat.ai.mapper.AiModelProviderMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Service
public class AiImageEditService {

    private static final String PROVIDER_CODE = "qwen";
    private static final String MODEL_TYPE = "image_edit";

    private final AiModelProviderMapper providerMapper;
    private final AiModelMapper modelMapper;
    private final AiModelApiKeyMapper apiKeyMapper;
    private final AiGeneratedImageMapper generatedImageMapper;
    private final AiModelManagementService aiModelManagementService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AiImageEditService(
            AiModelProviderMapper providerMapper,
            AiModelMapper modelMapper,
            AiModelApiKeyMapper apiKeyMapper,
            AiGeneratedImageMapper generatedImageMapper,
            AiModelManagementService aiModelManagementService,
            ObjectMapper objectMapper
    ) {
        this.providerMapper = providerMapper;
        this.modelMapper = modelMapper;
        this.apiKeyMapper = apiKeyMapper;
        this.generatedImageMapper = generatedImageMapper;
        this.aiModelManagementService = aiModelManagementService;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    public String edit(
            Long userId,
            AiAgent agent,
            Long conversationId,
            String sourceImageUrl,
            String instruction,
            String size,
            String negativePrompt
    ) {
        String cleanInstruction = text(instruction);
        if (cleanInstruction.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "图片编辑指令不能为空");
        }
        ResolvedImageEditConfig config = resolveConfig();
        String inputImage = resolveInputImage(conversationId, sourceImageUrl);
        AiGeneratedImage record = createPendingRecord(
                userId,
                agent,
                conversationId,
                config,
                inputImage,
                cleanInstruction,
                normalizeSize(size),
                text(negativePrompt)
        );

        try {
            String editedImageUrl = requestEdit(config, inputImage, cleanInstruction, record.getImageSize(), record.getNegativePrompt());
            record.setObjectUrl(editedImageUrl);
            record.setLocalPath("");
            record.setStorageType("provider_url");
            record.setContextSummary("已编辑图片：" + summarize(cleanInstruction, 80));
            record.setStatus("success");
            generatedImageMapper.updateById(record);
            return """
                    图片编辑成功。
                    源图：%s
                    编辑记录ID：%s
                    编辑指令：%s
                    请在最终回答中原样展示以下 Markdown 图片：
                    ![AI编辑图片](%s)
                    """.formatted(inputImage, record.getId(), cleanInstruction, editedImageUrl);
        } catch (ResponseStatusException exception) {
            markFailed(record, exception.getReason());
            throw exception;
        } catch (Exception exception) {
            markFailed(record, exception.getMessage());
            throw new ResponseStatusException(BAD_GATEWAY, "图片编辑调用失败");
        }
    }

    private ResolvedImageEditConfig resolveConfig() {
        AiModelProvider provider = providerMapper.selectOne(new LambdaQueryWrapper<AiModelProvider>()
                .eq(AiModelProvider::getProviderCode, PROVIDER_CODE)
                .eq(AiModelProvider::getEnabled, 1)
                .last("LIMIT 1"));
        if (provider == null) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "尚未启用通义图片编辑供应商");
        }
        AiModel model = modelMapper.selectOne(new LambdaQueryWrapper<AiModel>()
                .eq(AiModel::getProviderId, provider.getId())
                .eq(AiModel::getModelType, MODEL_TYPE)
                .eq(AiModel::getEnabled, 1)
                .orderByAsc(AiModel::getId)
                .last("LIMIT 1"));
        if (model == null) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "尚未配置通义图片编辑模型");
        }
        AiModelApiKey apiKey = apiKeyMapper.selectOne(new LambdaQueryWrapper<AiModelApiKey>()
                .eq(AiModelApiKey::getModelId, model.getId())
                .eq(AiModelApiKey::getEnabled, 1)
                .orderByAsc(AiModelApiKey::getPriority)
                .orderByAsc(AiModelApiKey::getId)
                .last("LIMIT 1"));
        if (apiKey == null) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "尚未配置通义图片编辑 API Key");
        }
        String plainApiKey = aiModelManagementService.decryptApiKey(apiKey);
        if (plainApiKey.isBlank()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "通义图片编辑 API Key 为空");
        }
        return new ResolvedImageEditConfig(provider, model, apiKey, plainApiKey);
    }

    private String resolveInputImage(Long conversationId, String sourceImageUrl) {
        String image = normalizeImageUrl(sourceImageUrl);
        if (!image.isBlank()) {
            return image;
        }
        if (conversationId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "请提供要编辑的图片，或先在当前会话生成一张图片");
        }
        AiGeneratedImage latest = generatedImageMapper.selectOne(new LambdaQueryWrapper<AiGeneratedImage>()
                .eq(AiGeneratedImage::getConversationId, conversationId)
                .eq(AiGeneratedImage::getStatus, "success")
                .ne(AiGeneratedImage::getObjectUrl, "")
                .orderByDesc(AiGeneratedImage::getId)
                .last("LIMIT 1"));
        if (latest == null || text(latest.getObjectUrl()).isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "当前会话没有可编辑的历史图片，请先生成或上传图片");
        }
        return latest.getObjectUrl();
    }

    private String normalizeImageUrl(String sourceImageUrl) {
        String value = text(sourceImageUrl);
        if (value.startsWith("![") && value.contains("](") && value.endsWith(")")) {
            return value.substring(value.indexOf("](") + 2, value.length() - 1).trim();
        }
        return value;
    }

    private AiGeneratedImage createPendingRecord(
            Long userId,
            AiAgent agent,
            Long conversationId,
            ResolvedImageEditConfig config,
            String sourceImageUrl,
            String instruction,
            String size,
            String negativePrompt
    ) {
        AiGeneratedImage record = new AiGeneratedImage();
        record.setUserId(userId);
        record.setAgentId(agent.getId());
        record.setConversationId(conversationId);
        record.setMessageId(null);
        record.setProviderId(config.provider().getId());
        record.setModelId(config.model().getId());
        record.setApiKeyId(config.apiKey().getId());
        record.setPrompt(instruction);
        record.setNegativePrompt(negativePrompt);
        record.setImageSize(size);
        record.setImageCount(1);
        record.setStorageType("local");
        record.setSourceUrl(sourceImageUrl);
        record.setObjectUrl("");
        record.setLocalPath("");
        record.setContextSummary("图片编辑中：" + summarize(instruction, 80));
        record.setStatus("pending");
        record.setErrorMessage("");
        generatedImageMapper.insert(record);
        return record;
    }

    private String requestEdit(
            ResolvedImageEditConfig config,
            String sourceImageUrl,
            String instruction,
            String size,
            String negativePrompt
    ) throws Exception {
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(Map.of("image", sourceImageUrl));
        content.add(Map.of("text", instruction));

        Map<String, Object> message = new LinkedHashMap<>();
        message.put("role", "user");
        message.put("content", content);

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("messages", List.of(message));

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("n", 1);
        parameters.put("watermark", false);
        parameters.put("prompt_extend", true);
        if (!text(size).isBlank()) {
            parameters.put("size", size);
        }
        if (!text(negativePrompt).isBlank()) {
            parameters.put("negative_prompt", negativePrompt);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", config.model().getModelName());
        payload.put("input", input);
        payload.put("parameters", parameters);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(joinUrl(config.provider().getBaseUrl(), config.model().getApiPath())))
                .header("Authorization", "Bearer " + config.apiKeyPlainText())
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ResponseStatusException(BAD_GATEWAY, "图片编辑模型服务调用失败：" + response.body());
        }
        Map<String, Object> body = objectMapper.readValue(response.body(), new TypeReference<>() {});
        String imageUrl = extractFirstImageUrl(body);
        if (imageUrl.isBlank()) {
            throw new ResponseStatusException(BAD_GATEWAY, "图片编辑模型未返回图片地址");
        }
        return imageUrl;
    }

    private String extractFirstImageUrl(Map<String, Object> body) {
        Map<String, Object> output = map(body.get("output"));
        List<Map<String, Object>> choices = listOfMap(output.get("choices"));
        for (Map<String, Object> choice : choices) {
            Map<String, Object> message = map(choice.get("message"));
            List<Map<String, Object>> content = listOfMap(message.get("content"));
            for (Map<String, Object> item : content) {
                String image = text(item.get("image"));
                if (!image.isBlank()) {
                    return image;
                }
            }
        }
        return "";
    }

    private void markFailed(AiGeneratedImage record, String message) {
        record.setStatus("failed");
        record.setErrorMessage(message == null ? "图片编辑失败" : message);
        generatedImageMapper.updateById(record);
    }

    private String normalizeSize(String size) {
        String value = text(size);
        return value.isBlank() ? "1024*1024" : value.replace("x", "*");
    }

    private String joinUrl(String baseUrl, String apiPath) {
        String base = trimTrailingSlash(baseUrl);
        String path = text(apiPath);
        if (path.isBlank()) {
            path = "/services/aigc/multimodal-generation/generation";
        }
        return base + (path.startsWith("/") ? path : "/" + path);
    }

    private String trimTrailingSlash(String value) {
        String text = text(value);
        return text.endsWith("/") ? text.substring(0, text.length() - 1) : text;
    }

    private Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> raw
                ? raw.entrySet().stream().collect(
                LinkedHashMap::new,
                (target, entry) -> target.put(String.valueOf(entry.getKey()), entry.getValue()),
                LinkedHashMap::putAll
        )
                : Map.of();
    }

    private List<Map<String, Object>> listOfMap(Object value) {
        if (!(value instanceof List<?> raw)) {
            return List.of();
        }
        return raw.stream().map(this::map).toList();
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String summarize(String value, int maxLength) {
        String text = text(value);
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }

    private record ResolvedImageEditConfig(
            AiModelProvider provider,
            AiModel model,
            AiModelApiKey apiKey,
            String apiKeyPlainText
    ) {
    }
}
