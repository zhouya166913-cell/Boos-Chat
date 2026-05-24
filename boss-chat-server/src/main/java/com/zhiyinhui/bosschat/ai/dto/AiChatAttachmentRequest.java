package com.zhiyinhui.bosschat.ai.dto;

public record AiChatAttachmentRequest(
        String attachmentId,
        String fileName,
        String fileType,
        String mimeType,
        Long size,
        String url,
        String localPath,
        String summary
) {
}
