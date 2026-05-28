package com.zhiyinhui.bosschat.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyinhui.bosschat.ai.dto.AiModelApiKeyRequest;
import com.zhiyinhui.bosschat.ai.dto.AiModelApiKeyResponse;
import com.zhiyinhui.bosschat.ai.dto.AiModelProviderRequest;
import com.zhiyinhui.bosschat.ai.dto.AiModelProviderResponse;
import com.zhiyinhui.bosschat.ai.dto.AiModelRequest;
import com.zhiyinhui.bosschat.ai.dto.AiModelResponse;
import com.zhiyinhui.bosschat.ai.entity.AiAgent;
import com.zhiyinhui.bosschat.ai.entity.AiGeneratedImage;
import com.zhiyinhui.bosschat.ai.entity.AiModel;
import com.zhiyinhui.bosschat.ai.entity.AiModelApiKey;
import com.zhiyinhui.bosschat.ai.entity.AiModelProvider;
import com.zhiyinhui.bosschat.ai.mapper.AiAgentMapper;
import com.zhiyinhui.bosschat.ai.mapper.AiGeneratedImageMapper;
import com.zhiyinhui.bosschat.ai.mapper.AiModelApiKeyMapper;
import com.zhiyinhui.bosschat.ai.mapper.AiModelMapper;
import com.zhiyinhui.bosschat.ai.mapper.AiModelProviderMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class AiModelManagementService {
    private final AiModelProviderMapper providerMapper;
    private final AiModelApiKeyMapper apiKeyMapper;
    private final AiModelMapper modelMapper;
    private final AiAgentMapper agentMapper;
    private final AiGeneratedImageMapper generatedImageMapper;
    private final ApiKeyCryptoService cryptoService;

    public AiModelManagementService(
            AiModelProviderMapper providerMapper,
            AiModelApiKeyMapper apiKeyMapper,
            AiModelMapper modelMapper,
            AiAgentMapper agentMapper,
            AiGeneratedImageMapper generatedImageMapper,
            ApiKeyCryptoService cryptoService
    ) {
        this.providerMapper = providerMapper;
        this.apiKeyMapper = apiKeyMapper;
        this.modelMapper = modelMapper;
        this.agentMapper = agentMapper;
        this.generatedImageMapper = generatedImageMapper;
        this.cryptoService = cryptoService;
    }

    public List<AiModelProviderResponse> listProviders() {
        return providerMapper.selectList(new LambdaQueryWrapper<AiModelProvider>().orderByAsc(AiModelProvider::getId))
                .stream()
                .map(this::toProviderResponse)
                .toList();
    }

    public AiModelProviderResponse createProvider(AiModelProviderRequest request) {
        ensureProviderCodeAvailable(null, request.providerCode());
        AiModelProvider provider = new AiModelProvider();
        applyProvider(provider, request);
        providerMapper.insert(provider);
        return toProviderResponse(providerMapper.selectById(provider.getId()));
    }

    public AiModelProviderResponse updateProvider(Long providerId, AiModelProviderRequest request) {
        AiModelProvider provider = requireProvider(providerId);
        ensureProviderCodeAvailable(providerId, request.providerCode());
        applyProvider(provider, request);
        providerMapper.updateById(provider);
        return toProviderResponse(providerMapper.selectById(providerId));
    }

    public List<AiModelResponse> listModels() {
        Map<Long, AiModelProvider> providers = providerMap();
        return modelMapper.selectList(new LambdaQueryWrapper<AiModel>()
                        .orderByAsc(AiModel::getProviderId)
                        .orderByAsc(AiModel::getId))
                .stream()
                .map(model -> toModelResponse(model, providers.get(model.getProviderId())))
                .toList();
    }

    public AiModelResponse createModel(AiModelRequest request) {
        requireProvider(request.providerId());
        ensureModelNameAvailable(null, request.providerId(), request.modelName());
        AiModel model = new AiModel();
        applyModel(model, request);
        modelMapper.insert(model);
        return toModelResponse(modelMapper.selectById(model.getId()), providerMapper.selectById(model.getProviderId()));
    }

    public AiModelResponse updateModel(Long modelId, AiModelRequest request) {
        AiModel model = requireModel(modelId);
        requireProvider(request.providerId());
        ensureModelNameAvailable(modelId, request.providerId(), request.modelName());
        applyModel(model, request);
        modelMapper.updateById(model);
        return toModelResponse(modelMapper.selectById(modelId), providerMapper.selectById(model.getProviderId()));
    }

    public void deleteModel(Long modelId) {
        AiModel model = requireModel(modelId);
        long apiKeyCount = apiKeyMapper.selectCount(new LambdaQueryWrapper<AiModelApiKey>()
                .eq(AiModelApiKey::getModelId, model.getId()));
        if (apiKeyCount > 0) {
            throw new ResponseStatusException(CONFLICT, "该模型下仍有 API Key，请先删除 API Key");
        }
        long agentCount = agentMapper.selectCount(new LambdaQueryWrapper<AiAgent>()
                .eq(AiAgent::getModelId, model.getId())
                .or()
                .eq(AiAgent::getImageModelId, model.getId()));
        if (agentCount > 0) {
            throw new ResponseStatusException(CONFLICT, "该模型仍被智能体引用，请先在智能体管理中解除绑定");
        }
        long imageRecordCount = generatedImageMapper.selectCount(new LambdaQueryWrapper<AiGeneratedImage>()
                .eq(AiGeneratedImage::getModelId, model.getId()));
        if (imageRecordCount > 0) {
            throw new ResponseStatusException(CONFLICT, "该模型已有图片生成记录，建议停用而不是删除");
        }
        modelMapper.deleteById(model.getId());
    }

    public List<AiModelApiKeyResponse> listApiKeys() {
        Map<Long, AiModelProvider> providers = providerMap();
        Map<Long, AiModel> models = modelMap();
        return apiKeyMapper.selectList(new LambdaQueryWrapper<AiModelApiKey>()
                        .orderByAsc(AiModelApiKey::getProviderId)
                        .orderByAsc(AiModelApiKey::getModelId)
                        .orderByAsc(AiModelApiKey::getPriority)
                        .orderByAsc(AiModelApiKey::getId))
                .stream()
                .map(apiKey -> toApiKeyResponse(
                        apiKey,
                        providers.get(apiKey.getProviderId()),
                        models.get(apiKey.getModelId())
                ))
                .toList();
    }

    public AiModelApiKeyResponse createApiKey(AiModelApiKeyRequest request) {
        AiModel model = requireModel(request.modelId());
        AiModelProvider provider = requireProvider(request.providerId());
        ensureModelBelongsToProvider(model, provider.getId());
        if (request.apiKey() == null || request.apiKey().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "新增 API Key 时必须填写 Key 内容");
        }
        AiModelApiKey apiKey = new AiModelApiKey();
        applyApiKey(apiKey, request, model, true);
        apiKeyMapper.insert(apiKey);
        return toApiKeyResponse(apiKeyMapper.selectById(apiKey.getId()), provider, model);
    }

    public AiModelApiKeyResponse updateApiKey(Long apiKeyId, AiModelApiKeyRequest request) {
        AiModelApiKey apiKey = requireApiKey(apiKeyId);
        AiModel model = requireModel(request.modelId());
        AiModelProvider provider = requireProvider(request.providerId());
        ensureModelBelongsToProvider(model, provider.getId());
        applyApiKey(apiKey, request, model, false);
        apiKeyMapper.updateById(apiKey);
        return toApiKeyResponse(apiKeyMapper.selectById(apiKeyId), provider, model);
    }

    public void deleteApiKey(Long apiKeyId) {
        AiModelApiKey apiKey = requireApiKey(apiKeyId);
        apiKeyMapper.deleteById(apiKey.getId());
    }

    public AiModel requireEnabledModel(Long modelId) {
        AiModel model = requireModel(modelId);
        if (model.getEnabled() == null || model.getEnabled() != 1) {
            throw new ResponseStatusException(NOT_FOUND, "模型未启用");
        }
        return model;
    }

    public AiModelProvider requireEnabledProvider(Long providerId) {
        AiModelProvider provider = requireProvider(providerId);
        if (provider.getEnabled() == null || provider.getEnabled() != 1) {
            throw new ResponseStatusException(NOT_FOUND, "模型供应商未启用");
        }
        return provider;
    }

    public AiModelApiKey requireEnabledApiKey(Long apiKeyId) {
        AiModelApiKey apiKey = requireApiKey(apiKeyId);
        if (apiKey.getEnabled() == null || apiKey.getEnabled() != 1) {
            throw new ResponseStatusException(NOT_FOUND, "API Key 未启用");
        }
        return apiKey;
    }

    public String decryptApiKey(AiModelApiKey apiKey) {
        return cryptoService.decrypt(apiKey.getApiKeyCipher());
    }

    private String displayApiKey(AiModelApiKey apiKey) {
        String cipher = apiKey.getApiKeyCipher();
        if (cipher == null || cipher.isBlank()) {
            return "";
        }
        return cryptoService.decrypt(cipher);
    }

    private void applyProvider(AiModelProvider provider, AiModelProviderRequest request) {
        provider.setProviderCode(request.providerCode().trim());
        provider.setProviderName(request.providerName().trim());
        provider.setBaseUrl(clean(request.baseUrl()));
        provider.setAuthType(clean(request.authType(), "bearer"));
        provider.setEnabled(switchValue(request.enabled(), 1));
        provider.setRemark(clean(request.remark()));
    }

    private void applyModel(AiModel model, AiModelRequest request) {
        model.setProviderId(request.providerId());
        model.setModelName(request.modelName().trim());
        model.setDisplayName(clean(request.displayName(), request.modelName().trim()));
        model.setModelType(clean(request.modelType(), "chat"));
        model.setApiPath(clean(request.apiPath(), defaultApiPath(request.modelType())));
        model.setBillingType(clean(request.billingType(), "unknown"));
        model.setOfficialDocUrl(clean(request.officialDocUrl()));
        model.setCompatibilityProfile(clean(request.compatibilityProfile(), inferCompatibilityProfile(model)));
        model.setContextWindow(request.contextWindow() == null ? 0 : Math.max(request.contextWindow(), 0));
        model.setSupportsStream(switchValue(request.supportsStream(), 1));
        model.setSupportsTools(switchValue(request.supportsTools(), 0));
        model.setSupportsVision(switchValue(request.supportsVision(), 0));
        model.setEnabled(switchValue(request.enabled(), 1));
        model.setRemark(clean(request.remark()));
    }

    private void applyApiKey(AiModelApiKey apiKey, AiModelApiKeyRequest request, AiModel model, boolean requireKey) {
        apiKey.setProviderId(model.getProviderId());
        apiKey.setModelId(model.getId());
        apiKey.setKeyName(request.keyName().trim());
        apiKey.setKeyType(clean(request.keyType(), "paid"));
        if (request.apiKey() != null && !request.apiKey().isBlank()) {
            String plainKey = request.apiKey().trim();
            apiKey.setApiKeyCipher(cryptoService.encrypt(plainKey));
            apiKey.setApiKeyMask(cryptoService.mask(plainKey));
        } else if (requireKey) {
            throw new ResponseStatusException(BAD_REQUEST, "API Key 不能为空");
        }
        apiKey.setPriority(request.priority() == null ? 100 : request.priority());
        apiKey.setEnabled(switchValue(request.enabled(), 1));
        apiKey.setRemark(clean(request.remark()));
    }

    private AiModelProvider requireProvider(Long providerId) {
        AiModelProvider provider = providerMapper.selectById(providerId);
        if (provider == null) {
            throw new ResponseStatusException(NOT_FOUND, "模型供应商不存在");
        }
        return provider;
    }

    private AiModel requireModel(Long modelId) {
        AiModel model = modelMapper.selectById(modelId);
        if (model == null) {
            throw new ResponseStatusException(NOT_FOUND, "模型不存在");
        }
        return model;
    }

    private AiModelApiKey requireApiKey(Long apiKeyId) {
        AiModelApiKey apiKey = apiKeyMapper.selectById(apiKeyId);
        if (apiKey == null) {
            throw new ResponseStatusException(NOT_FOUND, "API Key 不存在");
        }
        return apiKey;
    }

    private void ensureModelBelongsToProvider(AiModel model, Long providerId) {
        if (!model.getProviderId().equals(providerId)) {
            throw new ResponseStatusException(BAD_REQUEST, "所选模型不属于当前供应商");
        }
    }

    private void ensureProviderCodeAvailable(Long currentId, String providerCode) {
        LambdaQueryWrapper<AiModelProvider> wrapper = new LambdaQueryWrapper<AiModelProvider>()
                .eq(AiModelProvider::getProviderCode, providerCode.trim());
        if (currentId != null) {
            wrapper.ne(AiModelProvider::getId, currentId);
        }
        if (providerMapper.selectOne(wrapper.last("LIMIT 1")) != null) {
            throw new ResponseStatusException(CONFLICT, "供应商编码已存在");
        }
    }

    private void ensureModelNameAvailable(Long currentId, Long providerId, String modelName) {
        LambdaQueryWrapper<AiModel> wrapper = new LambdaQueryWrapper<AiModel>()
                .eq(AiModel::getProviderId, providerId)
                .eq(AiModel::getModelName, modelName.trim());
        if (currentId != null) {
            wrapper.ne(AiModel::getId, currentId);
        }
        if (modelMapper.selectOne(wrapper.last("LIMIT 1")) != null) {
            throw new ResponseStatusException(CONFLICT, "同一供应商下模型名称已存在");
        }
    }

    private Map<Long, AiModelProvider> providerMap() {
        return providerMapper.selectList(new LambdaQueryWrapper<AiModelProvider>())
                .stream()
                .collect(Collectors.toMap(AiModelProvider::getId, Function.identity()));
    }

    private Map<Long, AiModel> modelMap() {
        return modelMapper.selectList(new LambdaQueryWrapper<AiModel>())
                .stream()
                .collect(Collectors.toMap(AiModel::getId, Function.identity()));
    }

    private AiModelProviderResponse toProviderResponse(AiModelProvider provider) {
        return new AiModelProviderResponse(
                provider.getId(),
                provider.getProviderCode(),
                provider.getProviderName(),
                provider.getBaseUrl(),
                provider.getAuthType(),
                provider.getEnabled(),
                provider.getRemark()
        );
    }

    private AiModelResponse toModelResponse(AiModel model, AiModelProvider provider) {
        return new AiModelResponse(
                model.getId(),
                model.getProviderId(),
                provider == null ? "" : provider.getProviderName(),
                provider == null ? "" : provider.getProviderCode(),
                model.getModelName(),
                model.getDisplayName(),
                model.getModelType(),
                model.getApiPath(),
                model.getBillingType(),
                model.getOfficialDocUrl(),
                model.getCompatibilityProfile(),
                model.getContextWindow(),
                model.getSupportsStream(),
                model.getSupportsTools(),
                model.getSupportsVision(),
                model.getEnabled(),
                model.getRemark()
        );
    }

    private AiModelApiKeyResponse toApiKeyResponse(AiModelApiKey apiKey, AiModelProvider provider, AiModel model) {
        return new AiModelApiKeyResponse(
                apiKey.getId(),
                apiKey.getProviderId(),
                provider == null ? "" : provider.getProviderName(),
                provider == null ? "" : provider.getProviderCode(),
                apiKey.getModelId(),
                model == null ? "" : model.getModelName(),
                model == null ? "" : model.getDisplayName(),
                model == null ? "" : model.getModelType(),
                apiKey.getKeyName(),
                apiKey.getKeyType(),
                displayApiKey(apiKey),
                apiKey.getApiKeyMask(),
                apiKey.getPriority(),
                apiKey.getEnabled(),
                apiKey.getRemark()
        );
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String defaultApiPath(String modelType) {
        if ("image_generation".equals(clean(modelType))) {
            return "/images/generations";
        }
        return "/chat/completions";
    }

    private String inferCompatibilityProfile(AiModel model) {
        AiModelProvider provider = providerMapper.selectById(model.getProviderId());
        String providerCode = provider == null ? "" : clean(provider.getProviderCode()).toLowerCase();
        String modelName = clean(model.getModelName()).toLowerCase();
        if ("openai".equals(providerCode) && modelName.startsWith("gpt-5")) {
            return "openai_gpt5";
        }
        if ("kimi".equals(providerCode) && modelName.startsWith("kimi-k2")) {
            return "kimi_k2";
        }
        return "";
    }

    private Integer switchValue(Integer value, Integer fallback) {
        return value == null ? fallback : value == 0 ? 0 : 1;
    }
}
