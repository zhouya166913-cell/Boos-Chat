package com.zhiyinhui.bosschat.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiyinhui.bosschat.ai.entity.AiAgent;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class AgentToolRegistry {

    private final WorkspaceToolService workspaceToolService;
    private final AiImageGenerationService imageGenerationService;
    private final AiImageEditService imageEditService;
    private final ObjectMapper objectMapper;

    public AgentToolRegistry(
            WorkspaceToolService workspaceToolService,
            AiImageGenerationService imageGenerationService,
            AiImageEditService imageEditService,
            ObjectMapper objectMapper
    ) {
        this.workspaceToolService = workspaceToolService;
        this.imageGenerationService = imageGenerationService;
        this.imageEditService = imageEditService;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> definitions(AiAgent agent) {
        if (!isEnabled(agent.getToolsEnabled()) && !isEnabled(agent.getImageGenerationEnabled())) {
            return List.of();
        }
        List<Map<String, Object>> tools = new ArrayList<>();
        if (isEnabled(agent.getToolsEnabled())) {
            tools.addAll(List.of(
                    tool("list_directory", "列出指定目录的文件与子目录。必须传入明确路径；用户说“桌面”时 path 使用“桌面”或“desktop”，不要用空路径。", objectSchema(Map.of(
                            "path", stringProperty("明确目录路径。可用“桌面/desktop/下载/downloads/文档/documents”等常用位置别名，也可使用授权目录内路径或绝对路径。")
                    ), List.of("path"))),
                    tool("read_file", "读取授权目录内文本文件内容", objectSchema(Map.of(
                            "path", stringProperty("明确文件路径。可用“桌面/desktop/下载/downloads/文档/documents”等常用位置别名，也可使用授权目录内路径或绝对路径。")
                    ), List.of("path"))),
                    tool("read_excel", "读取授权目录内 .xlsx Excel 文件内容，返回工作表名称和前若干行数据", objectSchema(Map.of(
                            "path", stringProperty("明确 Excel 文件路径。可用“桌面/desktop/下载/downloads/文档/documents”等常用位置别名，也可使用授权目录内路径或绝对路径。"),
                            "maxRows", Map.of(
                                    "type", "integer",
                                    "description", "每个工作表最多读取的行数，默认 30，最大 100"
                            )
                    ), List.of("path"))),
                    tool("create_file", "在授权目录内新建文件，执行前需要用户确认", objectSchema(Map.of(
                            "path", stringProperty("明确文件路径。可用“桌面/desktop/下载/downloads/文档/documents”等常用位置别名，也可使用授权目录内路径或绝对路径。"),
                            "content", stringProperty("文件内容")
                    ), List.of("path"))),
                    tool("write_file", "覆盖写入授权目录内文件，执行前需要用户确认", objectSchema(Map.of(
                            "path", stringProperty("明确文件路径。可用“桌面/desktop/下载/downloads/文档/documents”等常用位置别名，也可使用授权目录内路径或绝对路径。"),
                            "content", stringProperty("完整文件内容")
                    ), List.of("path", "content"))),
                    tool("search_text", "在授权目录内搜索文本", objectSchema(Map.of(
                            "keyword", stringProperty("要搜索的关键字")
                    ), List.of("keyword"))),
                    tool("run_command", "在工作区根目录执行 PowerShell 命令，执行前需要用户确认", objectSchema(Map.of(
                            "command", stringProperty("要执行的 PowerShell 命令")
                    ), List.of("command"))),
                    tool("git_status", "查看当前工作区的 Git 状态", objectSchema(Map.of(), List.of())),
                    tool("git_diff", "查看当前工作区的 Git 变更", objectSchema(Map.of(), List.of())),
                    tool("run_tests", "在指定目录执行测试命令，执行前需要用户确认", objectSchema(Map.of(
                            "projectPath", stringProperty("相对工作区根目录的项目路径，或授权目录内的绝对路径"),
                            "command", stringProperty("测试命令")
                    ), List.of("projectPath", "command")))
            ));
        }
        if (isEnabled(agent.getImageGenerationEnabled())) {
            tools.add(tool("generate_image", "当用户明确要求从零生成图片、画图、配图、海报、流程图、示意图、视觉素材时调用。该工具是文生图能力，不用于编辑已有图片。调用后会返回可展示的图片 Markdown。", objectSchema(Map.of(
                    "prompt", stringProperty("详细图片提示词。应把用户需求整理成适合图片模型理解的画面描述，而不是简单复述。"),
                    "size", stringProperty("图片尺寸，默认 1024x1024。常用：1024x1024、768x1344、1344x768。"),
                    "quality", stringProperty("可选。图片质量参数；使用智谱 GLM-Image 时必须留空。")
            ), List.of("prompt"))));
            tools.add(tool("edit_image", "当用户要求修改上一张图片、基于原图换发型/换衣服/换背景/增删元素/改风格时调用。若用户没有明确给出 sourceImageUrl，可留空，后端会使用当前会话最近一张成功生成的图片。调用后会返回可展示的图片 Markdown。", objectSchema(Map.of(
                    "instruction", stringProperty("图片编辑指令。必须清楚说明保留什么、修改什么，例如：保持人物脸部和构图不变，把头发改成红色卷发。"),
                    "sourceImageUrl", stringProperty("可选。要编辑的源图 URL 或上一轮 Markdown 图片链接；用户说上一张图时可以留空。"),
                    "size", stringProperty("可选。输出尺寸，使用 1024*1024、1024*1536、1536*1024 等格式。"),
                    "negativePrompt", stringProperty("可选。反向提示词，例如：低清晰度、五官变形、额外手指。")
            ), List.of("instruction"))));
        }
        return tools;
    }

    public String execute(String toolName, String argumentsJson) {
        return execute(toolName, argumentsJson, false);
    }

    public String execute(String toolName, String argumentsJson, boolean approvalGranted) {
        return execute(toolName, argumentsJson, approvalGranted, null, null, null);
    }

    public String execute(
            String toolName,
            String argumentsJson,
            boolean approvalGranted,
            Long userId,
            AiAgent agent,
            Long conversationId
    ) {
        Map<String, Object> arguments = parse(argumentsJson);
        return switch (toolName) {
            case "list_directory" -> workspaceToolService.listDirectory(
                    required(arguments, "path"),
                    approvalGranted
            );
            case "read_file" -> workspaceToolService.readFile(
                    required(arguments, "path"),
                    approvalGranted
            );
            case "read_excel" -> workspaceToolService.readExcel(
                    required(arguments, "path"),
                    integer(arguments.get("maxRows")),
                    approvalGranted
            );
            case "create_file" -> workspaceToolService.createFile(
                    required(arguments, "path"),
                    text(arguments.get("content")),
                    approvalGranted
            );
            case "write_file" -> workspaceToolService.writeFile(
                    required(arguments, "path"),
                    required(arguments, "content"),
                    approvalGranted
            );
            case "search_text" -> workspaceToolService.searchText(required(arguments, "keyword"));
            case "run_command" -> workspaceToolService.runCommand(required(arguments, "command"));
            case "git_status" -> workspaceToolService.gitStatus();
            case "git_diff" -> workspaceToolService.gitDiff();
            case "run_tests" -> workspaceToolService.runTests(
                    required(arguments, "projectPath"),
                    required(arguments, "command"),
                    approvalGranted
            );
            case "generate_image" -> imageGenerationService.generate(
                    userId,
                    agent,
                    conversationId,
                    required(arguments, "prompt"),
                    text(arguments.get("size")),
                    text(arguments.get("quality"))
            );
            case "edit_image" -> imageEditService.edit(
                    userId,
                    agent,
                    conversationId,
                    text(arguments.get("sourceImageUrl")),
                    required(arguments, "instruction"),
                    text(arguments.get("size")),
                    text(arguments.get("negativePrompt"))
            );
            default -> throw new ResponseStatusException(BAD_REQUEST, "未知工具：" + toolName);
        };
    }

    public boolean requiresApproval(String toolName, String argumentsJson) {
        Map<String, Object> arguments = parse(argumentsJson);
        return switch (toolName) {
            case "create_file", "write_file", "run_command", "run_tests" -> true;
            case "list_directory" -> !workspaceToolService.isPathAllowed(required(arguments, "path"));
            case "read_file" -> !workspaceToolService.isPathAllowed(required(arguments, "path"));
            case "read_excel" -> !workspaceToolService.isPathAllowed(required(arguments, "path"));
            default -> false;
        };
    }

    private boolean isEnabled(Integer value) {
        return value != null && value == 1;
    }

    private Map<String, Object> tool(String name, String description, Map<String, Object> parameters) {
        Map<String, Object> function = new LinkedHashMap<>();
        function.put("name", name);
        function.put("description", description);
        function.put("parameters", parameters);
        Map<String, Object> wrapper = new LinkedHashMap<>();
        wrapper.put("type", "function");
        wrapper.put("function", function);
        return wrapper;
    }

    private Map<String, Object> objectSchema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        return schema;
    }

    private Map<String, Object> stringProperty(String description) {
        return Map.of(
                "type", "string",
                "description", description
        );
    }

    private Map<String, Object> parse(String json) {
        try {
            return objectMapper.readValue(json == null || json.isBlank() ? "{}" : json, new TypeReference<>() {});
        } catch (Exception exception) {
            throw new ResponseStatusException(BAD_REQUEST, "工具参数格式错误");
        }
    }

    private String required(Map<String, Object> arguments, String key) {
        String value = text(arguments.get(key));
        if (value.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "缺少工具参数：" + key);
        }
        return value;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private Integer integer(Object value) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
