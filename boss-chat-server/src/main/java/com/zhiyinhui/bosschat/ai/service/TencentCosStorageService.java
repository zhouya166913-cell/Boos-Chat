package com.zhiyinhui.bosschat.ai.service;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import com.zhiyinhui.bosschat.ai.entity.AiImageStorageConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Service
public class TencentCosStorageService {

    private final AiImageStorageConfigService storageConfigService;

    public TencentCosStorageService(AiImageStorageConfigService storageConfigService) {
        this.storageConfigService = storageConfigService;
    }

    public StoredObject uploadImage(
            Long userId,
            InputStream inputStream,
            String extension,
            String contentType,
            long contentLength,
            AiImageStorageConfig config
    ) {
        return uploadObject(userId, inputStream, extension, contentType, contentLength, config);
    }

    public StoredObject uploadObject(
            Long userId,
            InputStream inputStream,
            String extension,
            String contentType,
            long contentLength,
            AiImageStorageConfig config
    ) {
        ensureConfigured(config);
        String secretId = storageConfigService.decryptAccessKeyId(config);
        String secretKey = storageConfigService.decryptAccessKeySecret(config);
        if (secretId.isBlank() || secretKey.isBlank()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "腾讯云 COS SecretId / SecretKey 配置不完整");
        }

        String objectKey = buildObjectKey(userId, normalizeExtension(extension), config);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(clean(contentType).isBlank() ? contentTypeForExtension(extension) : contentType);
        metadata.setContentDisposition("inline");
        metadata.setCacheControl("public, max-age=31536000");
        if (contentLength > 0) {
            metadata.setContentLength(contentLength);
        }

        COSCredentials credentials = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(config.getRegion()));
        clientConfig.setHttpProtocol(HttpProtocol.https);
        COSClient client = new COSClient(credentials, clientConfig);
        try {
            client.putObject(new PutObjectRequest(config.getBucketName(), objectKey, inputStream, metadata));
            return new StoredObject(publicUrl(objectKey, config), objectKey);
        } catch (CosClientException exception) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "上传图片到腾讯云 COS 失败");
        } finally {
            client.shutdown();
        }
    }

    private void ensureConfigured(AiImageStorageConfig config) {
        if (config == null || config.getEnabled() == null || config.getEnabled() != 1) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "腾讯云 COS 图片存储配置未启用");
        }
        if (!"cos".equals(clean(config.getStorageType()))) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "当前默认图片存储不是腾讯云 COS");
        }
        if (clean(config.getRegion()).isBlank()
                || clean(config.getBucketName()).isBlank()
                || clean(config.getBaseUrl()).isBlank()) {
            throw new ResponseStatusException(SERVICE_UNAVAILABLE, "腾讯云 COS 配置不完整");
        }
    }

    private String buildObjectKey(Long userId, String extension, AiImageStorageConfig config) {
        String rootPath = clean(config.getRootPath()).isBlank() ? "chat-images" : clean(config.getRootPath());
        rootPath = trimSlashes(rootPath);
        String datePath = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String imageId = UUID.randomUUID().toString().replace("-", "");
        return rootPath + "/" + datePath + "/" + userId + "/" + imageId + extension;
    }

    private String publicUrl(String objectKey, AiImageStorageConfig config) {
        return trimTrailingSlash(config.getBaseUrl()) + "/" + objectKey;
    }

    private String contentTypeForExtension(String extension) {
        return switch (normalizeExtension(extension).toLowerCase(Locale.ROOT)) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".webp" -> "image/webp";
            case ".gif" -> "image/gif";
            case ".mp4" -> "video/mp4";
            case ".mov" -> "video/quicktime";
            case ".webm" -> "video/webm";
            case ".pdf" -> "application/pdf";
            case ".doc" -> "application/msword";
            case ".docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case ".txt", ".md" -> "text/plain";
            default -> "image/png";
        };
    }

    private String normalizeExtension(String extension) {
        String value = clean(extension).toLowerCase(Locale.ROOT);
        if (value.isBlank()) {
            return ".png";
        }
        return value.startsWith(".") ? value : "." + value;
    }

    private String trimSlashes(String value) {
        String result = value;
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

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    public record StoredObject(String url, String objectKey) {
    }
}
