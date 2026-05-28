package com.zhiyinhui.bosschat.ai.bootstrap;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyinhui.bosschat.ai.entity.AiModel;
import com.zhiyinhui.bosschat.ai.entity.AiModelApiKey;
import com.zhiyinhui.bosschat.ai.entity.AiModelProvider;
import com.zhiyinhui.bosschat.ai.mapper.AiModelApiKeyMapper;
import com.zhiyinhui.bosschat.ai.mapper.AiModelMapper;
import com.zhiyinhui.bosschat.ai.mapper.AiModelProviderMapper;
import com.zhiyinhui.bosschat.ai.service.ApiKeyCryptoService;
import com.zhiyinhui.bosschat.common.config.ModelSeedProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class AiModelBootstrap implements CommandLineRunner {

    private final AiModelProviderMapper providerMapper;
    private final AiModelMapper modelMapper;
    private final AiModelApiKeyMapper apiKeyMapper;
    private final ApiKeyCryptoService cryptoService;
    private final ModelSeedProperties modelSeedProperties;
    private final boolean demoDataEnabled;

    public AiModelBootstrap(
            AiModelProviderMapper providerMapper,
            AiModelMapper modelMapper,
            AiModelApiKeyMapper apiKeyMapper,
            ApiKeyCryptoService cryptoService,
            ModelSeedProperties modelSeedProperties,
            @Value("${app.bootstrap.demo-data-enabled:false}") boolean demoDataEnabled
    ) {
        this.providerMapper = providerMapper;
        this.modelMapper = modelMapper;
        this.apiKeyMapper = apiKeyMapper;
        this.cryptoService = cryptoService;
        this.modelSeedProperties = modelSeedProperties;
        this.demoDataEnabled = demoDataEnabled;
    }

    @Override
    public void run(String... args) {
        if (!demoDataEnabled) {
            return;
        }

        AiModelProvider zhipu = ensureProvider(
                "zhipu",
                "智谱 AI",
                "https://open.bigmodel.cn/api/paas/v4",
                "智谱开放平台通用 API，兼容 Chat Completions。"
        );
        AiModelProvider kimi = ensureProvider(
                "kimi",
                "Kimi / Moonshot",
                "https://api.moonshot.cn/v1",
                "月之暗面 Moonshot API，兼容 OpenAI Chat Completions。"
        );
        AiModelProvider openai = ensureProvider(
                "openai",
                "OpenAI",
                "https://api.openai.com/v1",
                "OpenAI API，用于基础对话、推理和测试验证。"
        );

        AiModel zhipuFlash = ensureModel(new ModelSeed(
                zhipu.getId(),
                "glm-4.5-flash",
                "GLM-4.5-Flash",
                "chat",
                "/chat/completions",
                "free",
                "https://docs.bigmodel.cn/cn/guide/models/free/glm-4.5-flash",
                "",
                128000,
                1,
                1,
                0,
                "智谱免费高效模型，适合基础对话、获客方案和轻量业务分析。"
        ));
        AiModel zhipuImage = ensureModel(new ModelSeed(
                zhipu.getId(),
                "glm-image",
                "GLM-Image",
                "image_generation",
                "/images/generations",
                "paid",
                "https://docs.bigmodel.cn/cn/guide/models/image-generation/glm-image",
                "",
                0,
                0,
                0,
                0,
                "智谱图片生成模型，用于业务配图和创意素材生成。"
        ));
        AiModel zhipuVision = ensureModel(new ModelSeed(
                zhipu.getId(),
                "glm-4.5v",
                "GLM-4.5V 多模态",
                "chat",
                "/chat/completions",
                "paid",
                "https://docs.bigmodel.cn/cn/guide/models/vlm/glm-4.5v",
                "zhipu_vision",
                64000,
                1,
                1,
                1,
                "智谱视觉推理多模态模型，支持视频、图像、文本和文件理解，适合附件内容分析。"
        ));
        AiModel kimiK26 = ensureModel(new ModelSeed(
                kimi.getId(),
                "kimi-k2.6",
                "Kimi K2.6",
                "chat",
                "/chat/completions",
                "paid",
                "https://platform.kimi.com/docs/api/overview",
                "kimi_k2",
                256000,
                1,
                1,
                0,
                "Kimi 长上下文模型，适合企业资料分析、方案梳理和交付文档。"
        ));
        AiModel openaiGpt55 = ensureModel(new ModelSeed(
                openai.getId(),
                "gpt-5.5",
                "ChatGPT 5.5",
                "chat",
                "/chat/completions",
                "free",
                "https://platform.openai.com/docs/models",
                "openai_gpt5",
                1050000,
                1,
                0,
                0,
                "OpenAI 对话模型，用于基础对话、推理和接口测试。"
        ));

        ensureApiKey(zhipuFlash, "智谱 GLM-4.5-Flash Key", "free", modelSeedProperties.zhipuApiKey(), 10, "测试版默认智谱对话 Key。");
        ensureApiKey(zhipuImage, "智谱 GLM-Image Key", "paid", modelSeedProperties.zhipuApiKey(), 20, "测试版默认智谱图片 Key。");
        ensureApiKey(zhipuVision, "智谱 GLM-4.5V Key", "paid", modelSeedProperties.zhipuApiKey(), 30, "测试版默认智谱多模态 Key，用于图片、视频和文件理解。");
        ensureApiKey(kimiK26, "Kimi K2.6 Key", "paid", modelSeedProperties.kimiApiKey(), 10, "测试版默认 Kimi Key。");
        ensureApiKey(openaiGpt55, "OpenAI ChatGPT 5.5 Key", "free", modelSeedProperties.openaiApiKey(), 10, "测试版默认 OpenAI Key。");
    }

    private AiModelProvider ensureProvider(String code, String name, String baseUrl, String remark) {
        AiModelProvider provider = providerMapper.selectOne(new LambdaQueryWrapper<AiModelProvider>()
                .eq(AiModelProvider::getProviderCode, code)
                .last("LIMIT 1"));
        if (provider != null) {
            return provider;
        }
        provider = new AiModelProvider();
        provider.setProviderCode(code);
        provider.setProviderName(name);
        provider.setBaseUrl(baseUrl);
        provider.setAuthType("bearer");
        provider.setEnabled(1);
        provider.setRemark(remark);
        providerMapper.insert(provider);
        return providerMapper.selectById(provider.getId());
    }

    private AiModel ensureModel(ModelSeed seed) {
        AiModel model = modelMapper.selectOne(new LambdaQueryWrapper<AiModel>()
                .eq(AiModel::getProviderId, seed.providerId())
                .eq(AiModel::getModelName, seed.modelName())
                .last("LIMIT 1"));
        if (model != null) {
            return model;
        }
        model = new AiModel();
        model.setProviderId(seed.providerId());
        model.setModelName(seed.modelName());
        model.setDisplayName(seed.displayName());
        model.setModelType(seed.modelType());
        model.setApiPath(seed.apiPath());
        model.setBillingType(seed.billingType());
        model.setOfficialDocUrl(seed.officialDocUrl());
        model.setCompatibilityProfile(seed.compatibilityProfile());
        model.setContextWindow(seed.contextWindow());
        model.setSupportsStream(seed.supportsStream());
        model.setSupportsTools(seed.supportsTools());
        model.setSupportsVision(seed.supportsVision());
        model.setEnabled(1);
        model.setRemark(seed.remark());
        modelMapper.insert(model);
        return modelMapper.selectById(model.getId());
    }

    private void ensureApiKey(AiModel model, String keyName, String keyType, String plainKey, int priority, String remark) {
        if (model == null || plainKey == null || plainKey.isBlank()) {
            return;
        }
        AiModelApiKey apiKey = apiKeyMapper.selectOne(new LambdaQueryWrapper<AiModelApiKey>()
                .eq(AiModelApiKey::getModelId, model.getId())
                .eq(AiModelApiKey::getKeyName, keyName)
                .last("LIMIT 1"));
        if (apiKey != null) {
            return;
        }
        apiKey = new AiModelApiKey();
        apiKey.setProviderId(model.getProviderId());
        apiKey.setModelId(model.getId());
        apiKey.setKeyName(keyName);
        String cleanKey = plainKey.trim();
        apiKey.setProviderId(model.getProviderId());
        apiKey.setModelId(model.getId());
        apiKey.setKeyType(keyType);
        apiKey.setApiKeyCipher(cryptoService.encrypt(cleanKey));
        apiKey.setApiKeyMask(cryptoService.mask(cleanKey));
        apiKey.setPriority(priority);
        apiKey.setEnabled(1);
        apiKey.setRemark(remark);
        apiKeyMapper.insert(apiKey);
    }

    private record ModelSeed(
            Long providerId,
            String modelName,
            String displayName,
            String modelType,
            String apiPath,
            String billingType,
            String officialDocUrl,
            String compatibilityProfile,
            Integer contextWindow,
            Integer supportsStream,
            Integer supportsTools,
            Integer supportsVision,
            String remark
    ) {
    }
}
