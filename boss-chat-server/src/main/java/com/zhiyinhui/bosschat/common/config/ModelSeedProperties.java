package com.zhiyinhui.bosschat.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.model-seed")
public record ModelSeedProperties(
        String zhipuApiKey,
        String kimiApiKey,
        String qwenApiKey,
        String openaiApiKey
) {
}
