package com.zhiyinhui.bosschat.common.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Tag(name = "系统健康检查")
@RestController
public class HealthController {

    private final Optional<BuildProperties> buildProperties;

    public HealthController(Optional<BuildProperties> buildProperties) {
        this.buildProperties = buildProperties;
    }

    @Operation(summary = "健康检查", description = "公开接口，用于本机、Nginx 和 Jenkins 部署后确认后端进程已启动。")
    @GetMapping("/api/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("service", "boss-chat-server");
        response.put("version", buildProperties.map(BuildProperties::getVersion).orElse("0.1.0"));
        response.put("time", OffsetDateTime.now().toString());
        return response;
    }
}
