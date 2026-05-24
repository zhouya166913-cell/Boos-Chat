package com.zhiyinhui.bosschat.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhiyinhui.bosschat.ai.dto.AiImageStorageRequest;
import com.zhiyinhui.bosschat.ai.dto.AiImageStorageResponse;
import com.zhiyinhui.bosschat.ai.entity.AiImageStorageConfig;
import com.zhiyinhui.bosschat.ai.mapper.AiImageStorageConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class AiImageStorageConfigService {

    private final AiImageStorageConfigMapper storageMapper;
    private final ApiKeyCryptoService cryptoService;

    public AiImageStorageConfigService(
            AiImageStorageConfigMapper storageMapper,
            ApiKeyCryptoService cryptoService
    ) {
        this.storageMapper = storageMapper;
        this.cryptoService = cryptoService;
    }

    public List<AiImageStorageResponse> list() {
        return storageMapper.selectList(new LambdaQueryWrapper<AiImageStorageConfig>()
                        .orderByDesc(AiImageStorageConfig::getIsDefault)
                        .orderByDesc(AiImageStorageConfig::getEnabled)
                        .orderByAsc(AiImageStorageConfig::getId))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public AiImageStorageResponse create(AiImageStorageRequest request) {
        ensureStorageCodeAvailable(null, request.storageCode());
        AiImageStorageConfig config = new AiImageStorageConfig();
        applyRequest(config, request, true);
        storageMapper.insert(config);
        if (config.getIsDefault() != null && config.getIsDefault() == 1) {
            clearOtherDefaults(config.getId());
        }
        return toResponse(storageMapper.selectById(config.getId()));
    }

    public AiImageStorageResponse update(Long storageId, AiImageStorageRequest request) {
        AiImageStorageConfig config = requireConfig(storageId);
        ensureStorageCodeAvailable(storageId, request.storageCode());
        applyRequest(config, request, false);
        storageMapper.updateById(config);
        if (config.getIsDefault() != null && config.getIsDefault() == 1) {
            clearOtherDefaults(config.getId());
        }
        return toResponse(storageMapper.selectById(storageId));
    }

    public AiImageStorageConfig requireEnabledConfig(Long storageId) {
        AiImageStorageConfig config = requireConfig(storageId);
        if (config.getEnabled() == null || config.getEnabled() != 1) {
            throw new ResponseStatusException(NOT_FOUND, "图片存储配置未启用");
        }
        return config;
    }

    public AiImageStorageConfig findDefaultEnabledConfig() {
        return storageMapper.selectOne(new LambdaQueryWrapper<AiImageStorageConfig>()
                .eq(AiImageStorageConfig::getEnabled, 1)
                .eq(AiImageStorageConfig::getIsDefault, 1)
                .orderByAsc(AiImageStorageConfig::getId)
                .last("LIMIT 1"));
    }

    public String decryptAccessKeyId(AiImageStorageConfig config) {
        String cipher = config == null ? "" : clean(config.getAccessKeyIdCipher());
        return cipher.isBlank() ? "" : cryptoService.decrypt(cipher);
    }

    public String decryptAccessKeySecret(AiImageStorageConfig config) {
        String cipher = config == null ? "" : clean(config.getAccessKeySecretCipher());
        return cipher.isBlank() ? "" : cryptoService.decrypt(cipher);
    }

    private void applyRequest(AiImageStorageConfig config, AiImageStorageRequest request, boolean requireSecretWhenProvided) {
        config.setStorageCode(request.storageCode().trim());
        config.setStorageName(request.storageName().trim());
        config.setStorageType(clean(request.storageType(), "local"));
        config.setEndpoint(clean(request.endpoint()));
        config.setRegion(clean(request.region()));
        config.setBucketName(clean(request.bucketName()));
        config.setBaseUrl(clean(request.baseUrl()));
        config.setRootPath(clean(request.rootPath()));
        config.setExtraConfigJson(clean(request.extraConfigJson()));
        config.setEnabled(switchValue(request.enabled(), 1));
        config.setIsDefault(switchValue(request.isDefault(), 0));
        config.setRemark(clean(request.remark()));

        if (request.accessKeyId() != null && !request.accessKeyId().isBlank()) {
            String accessKeyId = request.accessKeyId().trim();
            config.setAccessKeyIdCipher(cryptoService.encrypt(accessKeyId));
            config.setAccessKeyIdMask(cryptoService.mask(accessKeyId));
        } else if (requireSecretWhenProvided && config.getAccessKeyIdMask() == null) {
            config.setAccessKeyIdMask("");
        }

        if (request.accessKeySecret() != null && !request.accessKeySecret().isBlank()) {
            String accessKeySecret = request.accessKeySecret().trim();
            config.setAccessKeySecretCipher(cryptoService.encrypt(accessKeySecret));
            config.setAccessKeySecretMask(cryptoService.mask(accessKeySecret));
        } else if (requireSecretWhenProvided && config.getAccessKeySecretMask() == null) {
            config.setAccessKeySecretMask("");
        }
    }

    private AiImageStorageConfig requireConfig(Long storageId) {
        AiImageStorageConfig config = storageMapper.selectById(storageId);
        if (config == null) {
            throw new ResponseStatusException(NOT_FOUND, "图片存储配置不存在");
        }
        return config;
    }

    private void ensureStorageCodeAvailable(Long currentId, String storageCode) {
        LambdaQueryWrapper<AiImageStorageConfig> wrapper = new LambdaQueryWrapper<AiImageStorageConfig>()
                .eq(AiImageStorageConfig::getStorageCode, storageCode.trim());
        if (currentId != null) {
            wrapper.ne(AiImageStorageConfig::getId, currentId);
        }
        if (storageMapper.selectOne(wrapper.last("LIMIT 1")) != null) {
            throw new ResponseStatusException(CONFLICT, "图片存储编码已存在");
        }
    }

    private void clearOtherDefaults(Long currentId) {
        storageMapper.update(new LambdaUpdateWrapper<AiImageStorageConfig>()
                .ne(AiImageStorageConfig::getId, currentId)
                .set(AiImageStorageConfig::getIsDefault, 0));
    }

    private AiImageStorageResponse toResponse(AiImageStorageConfig config) {
        return new AiImageStorageResponse(
                config.getId(),
                config.getStorageCode(),
                config.getStorageName(),
                config.getStorageType(),
                config.getEndpoint(),
                config.getRegion(),
                config.getBucketName(),
                config.getBaseUrl(),
                config.getRootPath(),
                config.getExtraConfigJson(),
                config.getAccessKeyIdMask(),
                config.getAccessKeySecretMask(),
                config.getEnabled(),
                config.getIsDefault(),
                config.getRemark()
        );
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private Integer switchValue(Integer value, Integer fallback) {
        return value == null ? fallback : value == 0 ? 0 : 1;
    }
}
