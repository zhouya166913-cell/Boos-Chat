package com.zhiyinhui.bosschat.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import com.zhiyinhui.bosschat.ai.dto.AiImageStorageRequest;
import com.zhiyinhui.bosschat.ai.dto.AiImageStorageResponse;
import com.zhiyinhui.bosschat.ai.dto.AiImageStorageValidationResponse;
import com.zhiyinhui.bosschat.ai.entity.AiImageStorageConfig;
import com.zhiyinhui.bosschat.ai.mapper.AiImageStorageConfigMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

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

    public AiImageStorageValidationResponse validate(AiImageStorageRequest request) {
        return validate(null, request);
    }

    public AiImageStorageValidationResponse validate(Long storageId, AiImageStorageRequest request) {
        try {
            return validateStorageConfig(storageId, request);
        } catch (Exception exception) {
            return AiImageStorageValidationResponse.failure(clean(exception.getMessage(), "验证失败，请检查配置"));
        }
    }

    private AiImageStorageValidationResponse validateStorageConfig(Long storageId, AiImageStorageRequest request) {
        String storageType = clean(request.storageType());
        if ("oss".equals(storageType)) {
            return validateAliyunOss(storageId, request);
        }
        if ("cos".equals(storageType)) {
            return validateTencentCos(storageId, request);
        }
        return validateCustomStorage(request);
    }

    private AiImageStorageValidationResponse validateAliyunOss(Long storageId, AiImageStorageRequest request) {
        String endpoint = clean(request.endpoint());
        String bucketName = clean(request.bucketName());
        String baseUrl = clean(request.baseUrl());
        String accessKeyId = resolveAccessKeyId(storageId, request);
        String accessKeySecret = resolveAccessKeySecret(storageId, request);
        if (endpoint.isBlank()) {
            throw new IllegalArgumentException("阿里云 OSS Endpoint 不能为空");
        }
        validateCommonCloudFields(bucketName, baseUrl, accessKeyId, accessKeySecret);

        String objectKey = validationObjectKey(request);
        byte[] content = validationContent();
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("text/plain; charset=UTF-8");
        metadata.setContentLength(content.length);

        OSS client = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            client.putObject(bucketName, objectKey, new ByteArrayInputStream(content), metadata);
            String objectUrl = verifyPublicUrl(baseUrl, objectKey);
            String cleanupMessage = deleteAliyunObject(client, bucketName, objectKey);
            return AiImageStorageValidationResponse.success("阿里云 OSS 验证成功：上传、公开访问均可用。" + cleanupMessage, objectUrl);
        } catch (OSSException exception) {
            throw new IllegalArgumentException("阿里云 OSS 返回错误：" + cloudError(exception.getErrorCode(), exception.getErrorMessage()));
        } catch (ClientException exception) {
            throw new IllegalArgumentException("阿里云 OSS 连接失败：" + clean(exception.getMessage(), "客户端异常"));
        } finally {
            client.shutdown();
        }
    }

    private AiImageStorageValidationResponse validateTencentCos(Long storageId, AiImageStorageRequest request) {
        String region = clean(request.region());
        String bucketName = clean(request.bucketName());
        String baseUrl = clean(request.baseUrl());
        String secretId = resolveAccessKeyId(storageId, request);
        String secretKey = resolveAccessKeySecret(storageId, request);
        if (region.isBlank()) {
            throw new IllegalArgumentException("腾讯云 COS 地域不能为空");
        }
        validateCommonCloudFields(bucketName, baseUrl, secretId, secretKey);

        String objectKey = validationObjectKey(request);
        byte[] content = validationContent();
        com.qcloud.cos.model.ObjectMetadata metadata = new com.qcloud.cos.model.ObjectMetadata();
        metadata.setContentType("text/plain; charset=UTF-8");
        metadata.setContentLength(content.length);

        COSCredentials credentials = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        clientConfig.setHttpProtocol(HttpProtocol.https);
        COSClient client = new COSClient(credentials, clientConfig);
        try {
            client.putObject(new PutObjectRequest(bucketName, objectKey, new ByteArrayInputStream(content), metadata));
            String objectUrl = verifyPublicUrl(baseUrl, objectKey);
            String cleanupMessage = deleteTencentObject(client, bucketName, objectKey);
            return AiImageStorageValidationResponse.success("腾讯云 COS 验证成功：上传、公开访问均可用。" + cleanupMessage, objectUrl);
        } catch (CosServiceException exception) {
            throw new IllegalArgumentException("腾讯云 COS 返回错误：" + cloudError(exception.getErrorCode(), exception.getErrorMessage()));
        } catch (CosClientException exception) {
            throw new IllegalArgumentException("腾讯云 COS 连接失败：" + clean(exception.getMessage(), "客户端异常"));
        } finally {
            client.shutdown();
        }
    }

    private AiImageStorageValidationResponse validateCustomStorage(AiImageStorageRequest request) {
        if (clean(request.storageName()).isBlank()) {
            throw new IllegalArgumentException("其他存储需要填写存储名称");
        }
        String baseUrl = clean(request.baseUrl());
        if (baseUrl.isBlank()) {
            throw new IllegalArgumentException("其他存储需要填写访问域名");
        }
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            throw new IllegalArgumentException("访问域名必须以 http:// 或 https:// 开头");
        }
        return AiImageStorageValidationResponse.success("其他存储没有统一上传协议，仅完成字段格式验证；真实可用性请通过实际上传流程确认。", "");
    }

    private void validateCommonCloudFields(String bucketName, String baseUrl, String accessKeyId, String accessKeySecret) {
        if (bucketName.isBlank()) {
            throw new IllegalArgumentException("Bucket 不能为空");
        }
        if (baseUrl.isBlank()) {
            throw new IllegalArgumentException("访问域名不能为空");
        }
        if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            throw new IllegalArgumentException("访问域名必须以 http:// 或 https:// 开头");
        }
        if (accessKeyId.isBlank() || accessKeySecret.isBlank()) {
            throw new IllegalArgumentException("访问密钥不能为空");
        }
    }

    private String resolveAccessKeyId(Long storageId, AiImageStorageRequest request) {
        String value = clean(request.accessKeyId());
        if (!value.isBlank()) {
            return value;
        }
        return storageId == null ? "" : decryptAccessKeyId(requireConfig(storageId));
    }

    private String resolveAccessKeySecret(Long storageId, AiImageStorageRequest request) {
        String value = clean(request.accessKeySecret());
        if (!value.isBlank()) {
            return value;
        }
        return storageId == null ? "" : decryptAccessKeySecret(requireConfig(storageId));
    }

    private String validationObjectKey(AiImageStorageRequest request) {
        String rootPath = clean(request.rootPath()).isBlank() ? "chat-images" : clean(request.rootPath());
        return trimSlashes(rootPath) + "/_boss-chat-validation/" + UUID.randomUUID().toString().replace("-", "") + ".txt";
    }

    private byte[] validationContent() {
        return ("boss-chat storage validation\n" + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
    }

    private String verifyPublicUrl(String baseUrl, String objectKey) {
        String url = trimTrailingSlash(baseUrl) + "/" + objectKey;
        try {
            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(6))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 300) {
                return url;
            }
            if (statusCode == 403) {
                throw new IllegalArgumentException("临时文件上传成功，但公开访问返回 403。请检查 Bucket 公有读、对象 ACL、防盗链或 CDN 访问规则");
            }
            if (statusCode == 404) {
                throw new IllegalArgumentException("临时文件上传成功，但公开访问返回 404。请检查访问域名是否与 Bucket 和地域匹配");
            }
            throw new IllegalArgumentException("临时文件上传成功，但公开访问返回 HTTP " + statusCode);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("临时文件上传成功，但公开访问失败：" + clean(exception.getMessage(), "网络或域名不可达"));
        }
    }

    private String deleteAliyunObject(OSS client, String bucketName, String objectKey) {
        try {
            client.deleteObject(bucketName, objectKey);
            return "临时文件已清理。";
        } catch (Exception exception) {
            return "临时文件清理失败，请稍后在存储桶中删除 " + objectKey + "。";
        }
    }

    private String deleteTencentObject(COSClient client, String bucketName, String objectKey) {
        try {
            client.deleteObject(bucketName, objectKey);
            return "临时文件已清理。";
        } catch (Exception exception) {
            return "临时文件清理失败，请稍后在存储桶中删除 " + objectKey + "。";
        }
    }

    private String cloudError(String code, String message) {
        String errorCode = clean(code);
        String errorMessage = clean(message);
        if (errorCode.isBlank()) {
            return errorMessage.isBlank() ? "未知错误" : errorMessage;
        }
        return errorMessage.isBlank() ? errorCode : errorCode + " - " + errorMessage;
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
                decryptAccessKeyId(config),
                decryptAccessKeySecret(config),
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

    private String trimSlashes(String value) {
        String result = clean(value);
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result.isBlank() ? "chat-images" : result;
    }

    private String trimTrailingSlash(String value) {
        String text = clean(value);
        return text.endsWith("/") ? text.substring(0, text.length() - 1) : text;
    }

    private Integer switchValue(Integer value, Integer fallback) {
        return value == null ? fallback : value == 0 ? 0 : 1;
    }
}
