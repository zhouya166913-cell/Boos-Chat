package com.zhiyinhui.bosschat.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiyinhui.bosschat.ai.dto.AiChatAttachmentRequest;
import com.zhiyinhui.bosschat.ai.dto.LlmChatResult;
import com.zhiyinhui.bosschat.ai.dto.LlmToolCall;
import com.zhiyinhui.bosschat.ai.dto.LlmToolResponse;
import com.zhiyinhui.bosschat.ai.entity.AiAgent;
import com.zhiyinhui.bosschat.ai.entity.AiMessage;
import com.zhiyinhui.bosschat.ai.entity.AiModel;
import com.zhiyinhui.bosschat.ai.entity.AiModelApiKey;
import com.zhiyinhui.bosschat.ai.entity.AiModelProvider;
import com.zhiyinhui.bosschat.common.config.LlmProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Service
public class LlmChatService {

    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;
    private final AiModelManagementService aiModelManagementService;
    private final RestClient restClient;
    private final HttpClient httpClient;

    public LlmChatService(
            LlmProperties llmProperties,
            ObjectMapper objectMapper,
            AiModelManagementService aiModelManagementService
    ) {
        this.llmProperties = llmProperties;
        this.objectMapper = objectMapper;
        this.aiModelManagementService = aiModelManagementService;
        this.restClient = RestClient.builder().build();
        this.httpClient = HttpClient.newHttpClient();
    }

    public LlmChatResult stream(AiAgent agent, List<AiMessage> history, Consumer<String> onDelta) {
        return stream(agent, history, "", onDelta);
    }

    public LlmChatResult stream(
            AiAgent agent,
            List<AiMessage> history,
            String extraSystemContext,
            List<AiChatAttachmentRequest> attachments,
            Consumer<String> onDelta
    ) {
        ResolvedConfig config = resolveConfig(agent);
        List<Map<String, Object>> messages = buildMessages(agent, history, extraSystemContext, attachments);
        Map<String, Object> payload = buildPayload(agent, config, messages, true);
        return streamPayload(config, payload, onDelta);
    }

    public LlmChatResult stream(
            AiAgent agent,
            List<AiMessage> history,
            String extraSystemContext,
            Consumer<String> onDelta
    ) {
        ResolvedConfig config = resolveConfig(agent);
        List<Map<String, Object>> messages = buildMessages(agent, history, extraSystemContext, List.of());
        Map<String, Object> payload = buildPayload(agent, config, messages, true);
        return streamPayload(config, payload, onDelta);
    }

    private LlmChatResult streamPayload(
            ResolvedConfig config,
            Map<String, Object> payload,
            Consumer<String> onDelta
    ) {
        StringBuilder contentBuilder = new StringBuilder();
        UsageSnapshot usageSnapshot = new UsageSnapshot();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.apiUrl()))
                    .header("Authorization", "Bearer " + config.apiKey())
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(payload),
                            StandardCharsets.UTF_8
                    ))
                    .build();
            HttpResponse<java.io.InputStream> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofInputStream()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                throw new ResponseStatusException(BAD_GATEWAY, modelErrorMessage("模型流式调用失败", response.statusCode(), errorBody));
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8)
            )) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String data = line.substring("data:".length()).trim();
                    if (data.isBlank() || "[DONE]".equals(data)) {
                        continue;
                    }
                    Map<String, Object> body = objectMapper.readValue(data, new TypeReference<>() {});
                    String delta = parseDelta(body);
                    if (!delta.isBlank()) {
                        contentBuilder.append(delta);
                        onDelta.accept(delta);
                    }
                    usageSnapshot.capture(body);
                }
            }
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(BAD_GATEWAY, "模型流式调用异常：" + safeExceptionMessage(exception));
        }

        String content = contentBuilder.toString().trim();
        if (content.isBlank()) {
            throw new ResponseStatusException(BAD_GATEWAY, "模型未返回有效内容");
        }
        return new LlmChatResult(
                content,
                config.provider(),
                config.model(),
                usageSnapshot.promptTokens,
                usageSnapshot.completionTokens,
                usageSnapshot.totalTokens
        );
    }

    public LlmChatResult chat(AiAgent agent, List<AiMessage> history) {
        return chat(agent, history, "");
    }

    public LlmChatResult chat(AiAgent agent, List<AiMessage> history, String extraSystemContext) {
        return chat(agent, history, extraSystemContext, List.of());
    }

    public LlmChatResult chat(
            AiAgent agent,
            List<AiMessage> history,
            String extraSystemContext,
            List<AiChatAttachmentRequest> attachments
    ) {
        ResolvedConfig config = resolveConfig(agent);
        List<Map<String, Object>> messages = buildMessages(agent, history, extraSystemContext, attachments);
        Map<String, Object> payload = buildPayload(agent, config, messages, false);

        try {
            String raw = restClient.post()
                    .uri(config.apiUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + config.apiKey())
                    .body(payload)
                    .retrieve()
                    .body(String.class);
            Map<String, Object> body = objectMapper.readValue(raw, new TypeReference<>() {});
            return parseResult(body, config.provider(), config.model());
        } catch (RestClientResponseException exception) {
            throw new ResponseStatusException(BAD_GATEWAY, modelErrorMessage("模型服务调用失败", exception.getStatusCode().value(), exception.getResponseBodyAsString()));
        } catch (RestClientException exception) {
            throw new ResponseStatusException(BAD_GATEWAY, "模型服务调用异常：" + safeExceptionMessage(exception));
        } catch (Exception exception) {
            throw new ResponseStatusException(BAD_GATEWAY, "模型响应解析失败：" + safeExceptionMessage(exception));
        }
    }

    public LlmToolResponse chatWithTools(
            AiAgent agent,
            List<Map<String, Object>> messages,
            List<Map<String, Object>> tools
    ) {
        ResolvedConfig config = resolveConfig(agent);
        if (config.hasSelectedModel() && !config.supportsTools()) {
            throw new ResponseStatusException(BAD_REQUEST, "当前模型未标记支持工具调用，请在模型管理中更换模型或开启工具能力标记");
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", config.model());
        payload.put("messages", messages);
        payload.put("tools", tools);
        payload.put("tool_choice", "auto");
        payload.put("temperature", decimal(agent.getTemperature(), BigDecimal.valueOf(0.2)));
        payload.put("max_tokens", agent.getMaxCompletionTokens() == null ? 4096 : agent.getMaxCompletionTokens());
        applyProviderPayloadOptions(payload, config);

        try {
            String raw = restClient.post()
                    .uri(config.apiUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + config.apiKey())
                    .body(payload)
                    .retrieve()
                    .body(String.class);
            Map<String, Object> body = objectMapper.readValue(raw, new TypeReference<>() {});
            return parseToolResponse(body, config.provider(), config.model());
        } catch (RestClientResponseException exception) {
            throw new ResponseStatusException(BAD_GATEWAY, modelErrorMessage("模型工具调用失败", exception.getStatusCode().value(), exception.getResponseBodyAsString()));
        } catch (RestClientException exception) {
            throw new ResponseStatusException(BAD_GATEWAY, "模型工具调用异常：" + safeExceptionMessage(exception));
        } catch (Exception exception) {
            throw new ResponseStatusException(BAD_GATEWAY, "模型工具响应解析失败：" + safeExceptionMessage(exception));
        }
    }

    private ResolvedConfig resolveConfig(AiAgent agent) {
        String baseUrl = trimTrailingSlash(llmProperties.baseUrl());
        String apiKey = text(llmProperties.apiKey());
        String provider = text(agent.getModelProvider()).isBlank()
                ? text(llmProperties.provider())
                : text(agent.getModelProvider());
        String model = text(agent.getModelName()).isBlank()
                ? text(llmProperties.model())
                : text(agent.getModelName());
        boolean hasSelectedModel = false;
        boolean supportsTools = false;
        String compatibilityProfile = "";
        String apiPath = "";

        AiModel selectedModel = null;
        if (agent.getModelId() != null) {
            selectedModel = aiModelManagementService.requireEnabledModel(agent.getModelId());
            AiModelProvider selectedProvider = aiModelManagementService.requireEnabledProvider(selectedModel.getProviderId());
            provider = selectedProvider.getProviderCode();
            model = selectedModel.getModelName();
            baseUrl = trimTrailingSlash(selectedProvider.getBaseUrl());
            hasSelectedModel = true;
            supportsTools = isEnabled(selectedModel.getSupportsTools());
            compatibilityProfile = text(selectedModel.getCompatibilityProfile());
            apiPath = cleanApiPath(selectedModel.getApiPath(), selectedModel.getModelType());
        }

        if (agent.getApiKeyId() != null) {
            AiModelApiKey selectedApiKey = aiModelManagementService.requireEnabledApiKey(agent.getApiKeyId());
            if (selectedModel != null && !selectedApiKey.getModelId().equals(selectedModel.getId())) {
                throw new ResponseStatusException(BAD_REQUEST, "模型和 API Key 不属于同一个模型配置");
            }
            apiKey = aiModelManagementService.decryptApiKey(selectedApiKey);
        }

        String apiUrl = joinUrl(baseUrl, apiPath);
        if (apiUrl.isBlank() || apiKey.isBlank() || model.isBlank()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "模型服务尚未配置");
        }
        return new ResolvedConfig(baseUrl, apiUrl, apiKey, provider, model, compatibilityProfile, hasSelectedModel, supportsTools);
    }

    private List<Map<String, Object>> buildMessages(
            AiAgent agent,
            List<AiMessage> history,
            String extraSystemContext,
            List<AiChatAttachmentRequest> attachments
    ) {
        List<Map<String, Object>> messages = new ArrayList<>();
        String systemPrompt = buildSystemPrompt(agent);
        if (!text(extraSystemContext).isBlank()) {
            systemPrompt = systemPrompt + "\n\n" + extraSystemContext.trim();
        }
        messages.add(Map.of("role", "system", "content", systemPrompt));
        for (int index = 0; index < history.size(); index++) {
            AiMessage message = history.get(index);
            boolean isLatestUserMessage = index == history.size() - 1 && "user".equals(message.getRole());
            messages.add(Map.of(
                    "role", message.getRole(),
                    "content", isLatestUserMessage
                            ? contentWithAttachments(message, attachments)
                            : contextContent(message)
            ));
        }
        return messages;
    }

    private Object contentWithAttachments(AiMessage message, List<AiChatAttachmentRequest> attachments) {
        List<AiChatAttachmentRequest> mediaAttachments = attachments == null
                ? List.of()
                : attachments.stream()
                .filter(attachment -> List.of("image", "video", "document", "file").contains(text(attachment.fileType())))
                .toList();
        if (mediaAttachments.isEmpty()) {
            return contextContent(message);
        }

        List<Map<String, Object>> parts = new ArrayList<>();
        String content = contextContent(message);
        if (mediaAttachments.stream().anyMatch(attachment -> "video".equals(text(attachment.fileType())))) {
            content = content + """

                    请直接按已上传的视频内容进行整体分析，优先描述人物、场景、动作、画面变化和时间顺序。
                    不要把 video_url 当成普通文件名，也不要回答“无法直接播放视频”。
                    如果无法获得音频或字幕，只说明未进行音频转写，不要推断具体声音、歌词或台词。
                    """;
        }
        for (AiChatAttachmentRequest attachment : mediaAttachments) {
            Map<String, Object> mediaPart = mediaPart(attachment);
            if (!mediaPart.isEmpty()) {
                parts.add(mediaPart);
            }
        }
        parts.add(Map.of("type", "text", "text", content));
        return parts;
    }

    private Map<String, Object> mediaPart(AiChatAttachmentRequest attachment) {
        String fileType = text(attachment.fileType());
        if ("image".equals(fileType)) {
            String imageSource = resolveImageSource(attachment);
            return imageSource.isBlank()
                    ? Map.of()
                    : Map.of("type", "image_url", "image_url", Map.of("url", imageSource));
        }
        if ("video".equals(fileType)) {
            String videoSource = resolveRemoteSource(attachment, "视频");
            return Map.of("type", "video_url", "video_url", Map.of("url", videoSource));
        }
        String fileSource = resolveRemoteSource(attachment, "文件");
        return Map.of("type", "file_url", "file_url", Map.of("url", fileSource));
    }

    private String resolveImageSource(AiChatAttachmentRequest attachment) {
        String url = text(attachment.url());
        if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("data:")) {
            return url;
        }
        String localPath = text(attachment.localPath());
        if (localPath.isBlank()) {
            return "";
        }
        Path path = Path.of(localPath).toAbsolutePath().normalize();
        Path uploadRoot = Path.of("uploads").toAbsolutePath().normalize();
        if (!path.startsWith(uploadRoot) || !Files.isRegularFile(path)) {
            throw new ResponseStatusException(BAD_REQUEST, "图片附件路径非法或文件不存在");
        }
        try {
            String mimeType = text(attachment.mimeType()).isBlank() ? "image/png" : text(attachment.mimeType());
            return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(Files.readAllBytes(path));
        } catch (Exception exception) {
            throw new ResponseStatusException(BAD_REQUEST, "图片附件读取失败");
        }
    }

    private String resolveRemoteSource(AiChatAttachmentRequest attachment, String label) {
        String url = text(attachment.url());
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url;
        }
        throw new ResponseStatusException(
                BAD_REQUEST,
                label + "附件需要公网 URL 才能交给云端多模态模型理解，请先配置默认 OSS/COS 图床并重新上传附件"
        );
    }

    private String contextContent(AiMessage message) {
        if ("assistant".equals(message.getRole()) && !text(message.getAgentName()).isBlank()) {
            return "[" + message.getAgentName() + "]\n" + message.getContent();
        }
        return message.getContent();
    }

    private String buildSystemPrompt(AiAgent agent) {
        String abilityIdentity;
        if (isToolAgent(agent) || isEnabled(agent.getImageGenerationEnabled())) {
            abilityIdentity = """
                    当前能力定义：工具型 AI，不只是对话大模型。
                    工具型 AI = 大模型 + 记忆 / 知识库 / 工作流 / 本地工具或图片生成工具。
                    """;
        } else if (isEnhancedAgent(agent)) {
            abilityIdentity = """
                    当前能力定义：增强型 AI，不只是裸模型。
                    增强型 AI = 大模型 + 记忆 / 知识库 / 工作流，用于稳定业务表达和方法论复用。
                    """;
        } else {
            abilityIdentity = """
                    当前能力定义：基础 AI，以对话为主。
                    基础能力 = 理解用户问题，并基于系统提示词和上下文完成回答。
                    """;
        }
        return agent.getSystemPrompt() + "\n\n" + abilityIdentity + """

                如果上下文中存在「AI名称」格式的历史 assistant 消息，代表这是团队场景里的共享上下文。
                这些内容只能作为参考，不要冒充其他 AI。你必须以当前 AI 的身份回答。
                """;
    }


    private Map<String, Object> buildPayload(
            AiAgent agent,
            ResolvedConfig config,
            List<Map<String, Object>> messages,
            boolean stream
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", config.model());
        payload.put("messages", messages);
        payload.put("temperature", decimal(agent.getTemperature(), BigDecimal.valueOf(0.35)));
        payload.put("max_tokens", agent.getMaxCompletionTokens() == null ? 4096 : agent.getMaxCompletionTokens());
        payload.put("stream", stream);
        applyProviderPayloadOptions(payload, config);
        return payload;
    }

    private void applyProviderPayloadOptions(Map<String, Object> payload, ResolvedConfig config) {
        if (isOpenAiGpt5Family(config)) {
            Object maxTokens = payload.remove("max_tokens");
            if (maxTokens != null) {
                payload.put("max_completion_tokens", maxTokens);
            }
            payload.putIfAbsent("reasoning_effort", "medium");
            payload.remove("temperature");
        }
        if (isKimiK2Family(config)) {
            // Kimi K2.6 may return reasoning-only output unless thinking is disabled.
            // The survey and chat flows expect normal message.content text.
            payload.remove("temperature");
            payload.put("thinking", Map.of("type", "disabled"));
        }
    }

    private boolean isOpenAiGpt5Family(ResolvedConfig config) {
        return "openai_gpt5".equalsIgnoreCase(config.compatibilityProfile())
                || ("openai".equalsIgnoreCase(config.provider())
                && config.model().toLowerCase().startsWith("gpt-5"));
    }

    private boolean isKimiK2Family(ResolvedConfig config) {
        return "kimi_k2".equalsIgnoreCase(config.compatibilityProfile())
                || ("kimi".equalsIgnoreCase(config.provider())
                && config.model().toLowerCase().startsWith("kimi-k2"));
    }

    @SuppressWarnings("unchecked")
    private String modelErrorMessage(String prefix, int statusCode, String responseBody) {
        String detail = text(responseBody);
        try {
            Map<String, Object> body = objectMapper.readValue(detail, new TypeReference<>() {});
            Object error = body.get("error");
            if (error instanceof Map<?, ?> errorMap) {
                Object message = ((Map<String, Object>) errorMap).get("message");
                detail = message == null ? "" : String.valueOf(message);
            } else if (body.get("message") != null) {
                detail = String.valueOf(body.get("message"));
            }
        } catch (Exception ignored) {
            // Keep the raw provider body below when it is not JSON.
        }
        detail = detail.replaceAll("\\s+", " ").trim();
        if (detail.length() > 600) {
            detail = detail.substring(0, 600) + "...";
        }
        return detail.isBlank()
                ? prefix + "：" + statusCode
                : prefix + "：" + statusCode + " " + detail;
    }

    private String safeExceptionMessage(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        message = message.replaceAll("\\s+", " ").trim();
        return message.length() > 600 ? message.substring(0, 600) + "..." : message;
    }

    @SuppressWarnings("unchecked")
    private LlmChatResult parseResult(Map<String, Object> body, String provider, String model) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) body.getOrDefault("choices", List.of());
        Map<String, Object> firstChoice = choices.isEmpty() ? Map.of() : choices.get(0);
        Map<String, Object> message = (Map<String, Object>) firstChoice.getOrDefault("message", Map.of());
        String content = String.valueOf(message.getOrDefault("content", "")).trim();
        if (content.isBlank()) {
            throw new ResponseStatusException(BAD_GATEWAY, "模型未返回有效内容");
        }
        Map<String, Object> usage = (Map<String, Object>) body.getOrDefault("usage", Map.of());
        Integer promptTokens = intValue(usage.get("prompt_tokens"));
        Integer completionTokens = intValue(usage.get("completion_tokens"));
        Integer totalTokens = intValue(usage.get("total_tokens"));
        return new LlmChatResult(content, provider, model, promptTokens, completionTokens, totalTokens);
    }

    @SuppressWarnings("unchecked")
    private LlmToolResponse parseToolResponse(Map<String, Object> body, String provider, String model) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) body.getOrDefault("choices", List.of());
        Map<String, Object> firstChoice = choices.isEmpty() ? Map.of() : choices.get(0);
        Map<String, Object> message = (Map<String, Object>) firstChoice.getOrDefault("message", Map.of());
        String content = String.valueOf(message.getOrDefault("content", "")).trim();
        List<Map<String, Object>> rawToolCalls =
                (List<Map<String, Object>>) message.getOrDefault("tool_calls", List.of());
        List<LlmToolCall> toolCalls = rawToolCalls.stream().map(rawCall -> {
            Map<String, Object> function = (Map<String, Object>) rawCall.getOrDefault("function", Map.of());
            return new LlmToolCall(
                    String.valueOf(rawCall.getOrDefault("id", "")),
                    String.valueOf(function.getOrDefault("name", "")),
                    String.valueOf(function.getOrDefault("arguments", "{}"))
            );
        }).toList();
        return new LlmToolResponse(content, toolCalls, message, provider, model);
    }

    private String trimTrailingSlash(String value) {
        String text = text(value);
        return text.endsWith("/") ? text.substring(0, text.length() - 1) : text;
    }

    private String cleanApiPath(String apiPath, String modelType) {
        String path = text(apiPath);
        if (isAbsoluteUrl(path)) {
            return path;
        }
        return path.startsWith("/") ? path : "/" + path;
    }

    private String joinUrl(String baseUrl, String apiPath) {
        String path = cleanApiPath(apiPath, "");
        if (isAbsoluteUrl(path)) {
            return path;
        }
        String base = trimTrailingSlash(baseUrl);
        return base.isBlank() ? "" : base + path;
    }

    private boolean isAbsoluteUrl(String value) {
        String text = text(value).toLowerCase(Locale.ROOT);
        return text.startsWith("http://") || text.startsWith("https://");
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isToolAgent(AiAgent agent) {
        return isEnabled(agent.getToolsEnabled()) || isEnabled(agent.getImageGenerationEnabled());
    }

    private boolean isEnhancedAgent(AiAgent agent) {
        return isEnabled(agent.getMemoryEnabled())
                || isEnabled(agent.getKnowledgeEnabled())
                || isEnabled(agent.getWorkflowEnabled());
    }

    private boolean isEnabled(Integer value) {
        return value != null && value == 1;
    }

    private BigDecimal decimal(BigDecimal value, BigDecimal fallback) {
        return value == null ? fallback : value;
    }

    private Integer intValue(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    @SuppressWarnings("unchecked")
    private String parseDelta(Map<String, Object> body) {
        List<Map<String, Object>> choices = (List<Map<String, Object>>) body.getOrDefault("choices", List.of());
        Map<String, Object> firstChoice = choices.isEmpty() ? Map.of() : choices.get(0);
        Map<String, Object> delta = (Map<String, Object>) firstChoice.getOrDefault("delta", Map.of());
        Object content = delta.get("content");
        return content == null ? "" : String.valueOf(content);
    }

    private record ResolvedConfig(
            String baseUrl,
            String apiUrl,
            String apiKey,
            String provider,
            String model,
            String compatibilityProfile,
            boolean hasSelectedModel,
            boolean supportsTools
    ) {
    }

    @SuppressWarnings("unchecked")
    private class UsageSnapshot {
        private Integer promptTokens = 0;
        private Integer completionTokens = 0;
        private Integer totalTokens = 0;

        private void capture(Map<String, Object> body) {
            Map<String, Object> usage = (Map<String, Object>) body.getOrDefault("usage", Map.of());
            if (usage.isEmpty()) {
                return;
            }
            promptTokens = intValue(usage.get("prompt_tokens"));
            completionTokens = intValue(usage.get("completion_tokens"));
            totalTokens = intValue(usage.get("total_tokens"));
        }
    }
}

