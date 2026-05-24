package com.zhiyinhui.bosschat.ai.service;

import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Service
public class LocalAiImageStorageService {

    private final HttpClient httpClient;
    private final Path uploadRoot;

    public LocalAiImageStorageService() {
        this.httpClient = HttpClient.newHttpClient();
        this.uploadRoot = Path.of("uploads", "ai-images").toAbsolutePath().normalize();
    }

    public StoredImage saveRemoteImage(Long userId, String imageUrl, String fallbackExtension) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .GET()
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(BAD_GATEWAY, "下载模型返回图片失败：" + response.statusCode());
            }
            String extension = extensionFromContentType(response.headers().firstValue("Content-Type").orElse(""));
            if (extension.isBlank()) {
                extension = normalizeExtension(fallbackExtension);
            }
            return saveStream(userId, response.body(), extension);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "保存模型返回图片失败");
        }
    }

    private StoredImage saveStream(Long userId, InputStream inputStream, String extension) throws Exception {
        String safeExtension = extension.isBlank() ? ".png" : extension;
        String imageId = UUID.randomUUID().toString().replace("-", "");
        String datePath = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String storedName = imageId + safeExtension;
        Path targetDir = uploadRoot.resolve(datePath).resolve(String.valueOf(userId)).normalize();
        Path target = targetDir.resolve(storedName).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "图片保存路径非法");
        }
        Files.createDirectories(targetDir);
        Files.copy(inputStream, target);
        String url = "/api/uploads/ai-images/" + datePath + "/" + userId + "/" + storedName;
        return new StoredImage(url, target.toString());
    }

    private String extensionFromContentType(String contentType) {
        return switch (contentType == null ? "" : contentType.toLowerCase(Locale.ROOT).split(";")[0].trim()) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            case "image/bmp" -> ".bmp";
            case "image/tiff" -> ".tiff";
            default -> "";
        };
    }

    private String normalizeExtension(String extension) {
        String value = extension == null ? "" : extension.trim().toLowerCase(Locale.ROOT);
        if (value.isBlank()) {
            return ".png";
        }
        return value.startsWith(".") ? value : "." + value;
    }

    public record StoredImage(String url, String localPath) {
    }
}
