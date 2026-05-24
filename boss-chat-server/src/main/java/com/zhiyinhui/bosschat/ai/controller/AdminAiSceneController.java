package com.zhiyinhui.bosschat.ai.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.zhiyinhui.bosschat.ai.dto.AiSceneRequest;
import com.zhiyinhui.bosschat.ai.dto.AiSceneResponse;
import com.zhiyinhui.bosschat.ai.service.AiSceneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "场景管理")
@RestController
@RequestMapping("/api/admin/scenes")
public class AdminAiSceneController {

    private final AiSceneService aiSceneService;

    public AdminAiSceneController(AiSceneService aiSceneService) {
        this.aiSceneService = aiSceneService;
    }

    @Operation(summary = "查询全部场景", description = "管理员查看全部 AI 场景配置，包括单聊场景、团队场景和场景内 AI 助手。")
    @SecurityRequirement(name = "Sa-Token")
    @GetMapping
    public List<AiSceneResponse> listAll() {
        StpUtil.checkRole("super_admin");
        return aiSceneService.listAll();
    }

    @Operation(summary = "新增场景", description = "创建一个业务场景，并配置该场景可使用的 AI 助手。")
    @SecurityRequirement(name = "Sa-Token")
    @PostMapping
    public AiSceneResponse create(@Valid @RequestBody AiSceneRequest request) {
        StpUtil.checkRole("super_admin");
        return aiSceneService.create(request);
    }

    @Operation(summary = "修改场景", description = "修改场景名称、说明、会话模式、启用状态以及场景内 AI 助手。")
    @SecurityRequirement(name = "Sa-Token")
    @PutMapping("/{sceneId}")
    public AiSceneResponse update(@PathVariable Long sceneId, @Valid @RequestBody AiSceneRequest request) {
        StpUtil.checkRole("super_admin");
        return aiSceneService.update(sceneId, request);
    }
}
