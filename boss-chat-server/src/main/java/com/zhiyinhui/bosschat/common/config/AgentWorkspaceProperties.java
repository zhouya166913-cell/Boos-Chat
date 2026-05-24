package com.zhiyinhui.bosschat.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.agent")
public record AgentWorkspaceProperties(
        String workspaceRoot,
        String allowedRoots,
        Integer commandTimeoutSeconds,
        Integer maxReadCharacters,
        Integer maxToolRounds
) {
}
