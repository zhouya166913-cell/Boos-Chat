package com.zhiyinhui.bosschat.ai.dto;

public record AiImageStorageValidationResponse(
        boolean success,
        String message,
        String objectUrl
) {
    public static AiImageStorageValidationResponse success(String message, String objectUrl) {
        return new AiImageStorageValidationResponse(true, message, objectUrl);
    }

    public static AiImageStorageValidationResponse failure(String message) {
        return new AiImageStorageValidationResponse(false, message, "");
    }
}
