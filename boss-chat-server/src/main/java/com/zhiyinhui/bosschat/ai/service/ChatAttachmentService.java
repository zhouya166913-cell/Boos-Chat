package com.zhiyinhui.bosschat.ai.service;

import com.zhiyinhui.bosschat.ai.dto.AiChatAttachmentResponse;
import com.zhiyinhui.bosschat.ai.entity.AiImageStorageConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Service
public class ChatAttachmentService {

    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final long MAX_EXCEL_BYTES = 20L * 1024 * 1024;
    private static final long MAX_OTHER_BYTES = 50L * 1024 * 1024;
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp", ".gif");
    private static final Set<String> LOCAL_EXTENSIONS = Set.of(".xlsx", ".pdf", ".doc", ".docx", ".txt", ".md", ".mp4", ".mov", ".webm");

    private final WorkspaceToolService workspaceToolService;
    private final AiImageStorageConfigService imageStorageConfigService;
    private final AliyunOssStorageService aliyunOssStorageService;
    private final TencentCosStorageService tencentCosStorageService;
    private final Path uploadRoot;

    public ChatAttachmentService(
            WorkspaceToolService workspaceToolService,
            AiImageStorageConfigService imageStorageConfigService,
            AliyunOssStorageService aliyunOssStorageService,
            TencentCosStorageService tencentCosStorageService
    ) {
        this.workspaceToolService = workspaceToolService;
        this.imageStorageConfigService = imageStorageConfigService;
        this.aliyunOssStorageService = aliyunOssStorageService;
        this.tencentCosStorageService = tencentCosStorageService;
        this.uploadRoot = Path.of("uploads", "chat-attachments").toAbsolutePath().normalize();
    }

    public AiChatAttachmentResponse upload(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "请选择要上传的文件");
        }

        String originalName = file.getOriginalFilename() == null ? "attachment" : file.getOriginalFilename();
        String extension = extensionOf(originalName);
        String mimeType = file.getContentType() == null ? "" : file.getContentType();
        String fileType = resolveFileType(extension, mimeType);
        validateFile(file, extension, fileType);

        String attachmentId = UUID.randomUUID().toString().replace("-", "");
        String datePath = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String storedName = attachmentId + extension;

        AiImageStorageConfig defaultStorageConfig = imageStorageConfigService.findDefaultEnabledConfig();
        if (shouldUploadToCloud(fileType, defaultStorageConfig) && "oss".equals(defaultStorageConfig.getStorageType())) {
            try {
                AliyunOssStorageService.StoredObject storedObject = aliyunOssStorageService.uploadObject(
                        userId,
                        file.getInputStream(),
                        extension,
                        mimeType,
                        defaultStorageConfig
                );
                return new AiChatAttachmentResponse(
                        attachmentId,
                        sanitizeFileName(originalName),
                        fileType,
                        mimeType,
                        file.getSize(),
                        storedObject.url(),
                        "",
                        buildCloudSummary(originalName, fileType)
                );
            } catch (IOException exception) {
                throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "附件上传失败");
            }
        }
        if (shouldUploadToCloud(fileType, defaultStorageConfig) && "cos".equals(defaultStorageConfig.getStorageType())) {
            try {
                TencentCosStorageService.StoredObject storedObject = tencentCosStorageService.uploadObject(
                        userId,
                        file.getInputStream(),
                        extension,
                        mimeType,
                        file.getSize(),
                        defaultStorageConfig
                );
                return new AiChatAttachmentResponse(
                        attachmentId,
                        sanitizeFileName(originalName),
                        fileType,
                        mimeType,
                        file.getSize(),
                        storedObject.url(),
                        "",
                        buildCloudSummary(originalName, fileType)
                );
            } catch (IOException exception) {
                throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "附件上传失败");
            }
        }

        Path targetDir = uploadRoot.resolve(datePath).resolve(String.valueOf(userId)).normalize();
        Path target = targetDir.resolve(storedName).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new ResponseStatusException(BAD_REQUEST, "附件保存路径非法");
        }

        try {
            Files.createDirectories(targetDir);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "附件保存失败");
        }

        String url = "/api/uploads/chat-attachments/" + datePath + "/" + userId + "/" + storedName;
        String summary = buildSummary(target, originalName, fileType);
        return new AiChatAttachmentResponse(
                attachmentId,
                sanitizeFileName(originalName),
                fileType,
                mimeType,
                file.getSize(),
                url,
                target.toString(),
                summary
        );
    }

    private void validateFile(MultipartFile file, String extension, String fileType) {
        if (".xls".equals(extension)) {
            throw new ResponseStatusException(BAD_REQUEST, "暂不支持旧版 .xls，请先另存为 .xlsx");
        }
        if (!IMAGE_EXTENSIONS.contains(extension) && !LOCAL_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(BAD_REQUEST, "暂不支持该文件类型，请先上传图片、.xlsx、PDF、Word、文本或视频文件");
        }

        long maxBytes = "image".equals(fileType)
                ? MAX_IMAGE_BYTES
                : "excel".equals(fileType) ? MAX_EXCEL_BYTES : MAX_OTHER_BYTES;
        if (file.getSize() > maxBytes) {
            throw new ResponseStatusException(BAD_REQUEST, "文件过大，请压缩后再上传");
        }
    }

    private boolean shouldUploadToCloud(String fileType, AiImageStorageConfig config) {
        if (config == null) {
            return false;
        }
        return Set.of("image", "video", "document", "file").contains(fileType);
    }

    private String resolveFileType(String extension, String mimeType) {
        String lowerMime = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        if (lowerMime.startsWith("image/") || IMAGE_EXTENSIONS.contains(extension)) {
            return "image";
        }
        if (".xlsx".equals(extension)) {
            return "excel";
        }
        if (lowerMime.startsWith("video/") || Set.of(".mp4", ".mov", ".webm").contains(extension)) {
            return "video";
        }
        if (Set.of(".pdf", ".doc", ".docx").contains(extension)) {
            return "document";
        }
        return "file";
    }

    private String buildSummary(Path path, String originalName, String fileType) {
        String fileName = sanitizeFileName(originalName);
        if ("image".equals(fileType)) {
            return buildImageSummary(originalName, false);
        }
        if ("excel".equals(fileType)) {
            try {
                return workspaceToolService.readExcel(path.toString(), 50, true);
            } catch (ResponseStatusException exception) {
                return "Excel 附件：" + fileName + "。解析失败：" + exception.getReason();
            }
        }
        if ("video".equals(fileType)) {
            return "视频附件：" + fileName + "。当前版本先保存文件，后续可接入视频理解模型或抽帧解析。";
        }
        if ("document".equals(fileType)) {
            return "文档附件：" + fileName + "。当前版本先保存文件，后续可接入 PDF/Word 正文解析。";
        }
        return "文件附件：" + fileName + "。当前版本先保存文件，可作为人工查看或后续工具解析资源。";
    }

    private String buildCloudSummary(String originalName, String fileType) {
        String fileName = sanitizeFileName(originalName);
        if ("image".equals(fileType)) {
            return buildImageSummary(originalName, true);
        }
        if ("video".equals(fileType)) {
            return "视频附件：" + fileName + "。已上传到公网对象存储，可交给支持 video_url 的多模态模型理解。";
        }
        if ("document".equals(fileType)) {
            return "文档附件：" + fileName + "。已上传到公网对象存储，可交给支持 file_url 的多模态模型理解。";
        }
        return "文件附件：" + fileName + "。已上传到公网对象存储，可作为模型或工具读取资源。";
    }

    private String buildImageSummary(String originalName, boolean uploadedToOss) {
        String fileName = sanitizeFileName(originalName);
        String storageText = uploadedToOss ? "已上传到阿里云 OSS" : "已保存到本地";
        return "图片附件：" + fileName + "。" + storageText + "，本次对话会把图片作为多模态输入传给支持视觉的模型。";
    }

    private String extensionOf(String fileName) {
        String safeName = sanitizeFileName(fileName);
        int index = safeName.lastIndexOf('.');
        return index < 0 ? "" : safeName.substring(index).toLowerCase(Locale.ROOT);
    }

    private String sanitizeFileName(String fileName) {
        String safe = fileName == null ? "attachment" : fileName.replace("\\", "/");
        int slashIndex = safe.lastIndexOf('/');
        safe = slashIndex >= 0 ? safe.substring(slashIndex + 1) : safe;
        safe = safe.replaceAll("[\\r\\n\\t]", " ").trim();
        return safe.isBlank() ? "attachment" : safe;
    }
}
