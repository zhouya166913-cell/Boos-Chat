package com.zhiyinhui.bosschat.ai.service;

import com.zhiyinhui.bosschat.common.config.AgentWorkspaceProperties;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Service
public class WorkspaceToolService {

    private final Path workspaceRoot;
    private final List<Path> allowedRoots;
    private final int commandTimeoutSeconds;
    private final int maxReadCharacters;

    public WorkspaceToolService(AgentWorkspaceProperties properties) {
        this.workspaceRoot = Path.of(properties.workspaceRoot() == null ? ".." : properties.workspaceRoot())
                .toAbsolutePath()
                .normalize();
        this.allowedRoots = buildAllowedRoots(properties.allowedRoots());
        this.commandTimeoutSeconds = properties.commandTimeoutSeconds() == null ? 120 : properties.commandTimeoutSeconds();
        this.maxReadCharacters = properties.maxReadCharacters() == null ? 40000 : properties.maxReadCharacters();
    }

    public String listDirectory(String relativePath) {
        return listDirectory(relativePath, false);
    }

    public String listDirectory(String relativePath, boolean allowUnauthorizedPath) {
        Path path = resolve(relativePath, allowUnauthorizedPath);
        if (!Files.isDirectory(path)) {
            throw new ResponseStatusException(BAD_REQUEST, "目标不是目录");
        }
        try (Stream<Path> stream = Files.list(path)) {
            return stream
                    .sorted()
                    .map(item -> displayPath(item) + (Files.isDirectory(item) ? "/" : ""))
                    .limit(200)
                    .reduce((left, right) -> left + "\n" + right)
                    .orElse("");
        } catch (IOException exception) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "读取目录失败");
        }
    }

    public String readFile(String relativePath) {
        return readFile(relativePath, false);
    }

    public String readFile(String relativePath, boolean allowUnauthorizedPath) {
        Path path = resolve(relativePath, allowUnauthorizedPath);
        if (!Files.isRegularFile(path)) {
            throw new ResponseStatusException(BAD_REQUEST, "文件不存在");
        }
        String fileName = path.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
            throw new ResponseStatusException(BAD_REQUEST, "Excel 文件请使用 read_excel 工具读取");
        }
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            return content.length() <= maxReadCharacters
                    ? content
                    : content.substring(0, maxReadCharacters) + "\n...[内容已截断]";
        } catch (IOException exception) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "读取文件失败");
        }
    }

    public String readExcel(String relativePath, Integer maxRows) {
        return readExcel(relativePath, maxRows, false);
    }

    public String readExcel(String relativePath, Integer maxRows, boolean allowUnauthorizedPath) {
        Path path = resolve(relativePath, allowUnauthorizedPath);
        if (!Files.isRegularFile(path)) {
            throw new ResponseStatusException(BAD_REQUEST, "Excel 文件不存在");
        }
        String fileName = path.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".xls")) {
            throw new ResponseStatusException(BAD_REQUEST, "暂不支持旧版 .xls，请先另存为 .xlsx 后读取");
        }
        if (!fileName.endsWith(".xlsx")) {
            throw new ResponseStatusException(BAD_REQUEST, "目标不是 .xlsx 文件");
        }
        int safeMaxRows = maxRows == null || maxRows < 1 ? 30 : Math.min(maxRows, 100);
        try (ZipFile zipFile = new ZipFile(path.toFile(), StandardCharsets.UTF_8)) {
            List<String> sharedStrings = readSharedStrings(zipFile);
            Map<String, String> relationshipTargets = readWorkbookRelationships(zipFile);
            List<SheetInfo> sheets = readWorkbookSheets(zipFile, relationshipTargets);
            StringBuilder builder = new StringBuilder();
            builder.append("Excel 文件：").append(displayPath(path)).append("\n");
            builder.append("工作表数量：").append(sheets.size()).append("\n\n");
            for (SheetInfo sheet : sheets) {
                builder.append("## 工作表：").append(sheet.name()).append("\n");
                builder.append(readSheet(zipFile, sheet.path(), sharedStrings, safeMaxRows)).append("\n");
            }
            return truncate(builder.toString().trim());
        } catch (IOException exception) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "读取 Excel 失败");
        }
    }

    public String createFile(String relativePath, String content) {
        return createFile(relativePath, content, false);
    }

    public String createFile(String relativePath, String content, boolean allowUnauthorizedPath) {
        Path path = resolve(relativePath, allowUnauthorizedPath);
        if (Files.exists(path)) {
            throw new ResponseStatusException(BAD_REQUEST, "文件已存在");
        }
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, content == null ? "" : content, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            return "已创建文件：" + displayPath(path);
        } catch (IOException exception) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "创建文件失败");
        }
    }

    public String writeFile(String relativePath, String content) {
        return writeFile(relativePath, content, false);
    }

    public String writeFile(String relativePath, String content, boolean allowUnauthorizedPath) {
        Path path = resolve(relativePath, allowUnauthorizedPath);
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, content == null ? "" : content, StandardCharsets.UTF_8);
            return "已写入文件：" + displayPath(path);
        } catch (IOException exception) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "写入文件失败");
        }
    }

    public String searchText(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "搜索关键字不能为空");
        }
        try {
            StringBuilder builder = new StringBuilder();
            for (Path root : allowedRoots) {
                try (Stream<Path> stream = Files.walk(root)) {
                    String result = stream
                            .filter(Files::isRegularFile)
                            .filter(path -> !path.toString().contains("\\node_modules\\"))
                            .filter(path -> !path.toString().contains("\\dist\\"))
                            .limit(3000)
                            .flatMap(path -> searchInFile(path, keyword))
                            .limit(100)
                            .reduce((left, right) -> left + "\n" + right)
                            .orElse("");
                    if (!result.isBlank()) {
                        builder.append(result).append("\n");
                    }
                }
            }
            return builder.toString().trim();
        } catch (IOException exception) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "搜索失败");
        }
    }

    public String runCommand(String command) {
        if (command == null || command.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "命令不能为空");
        }
        ProcessBuilder builder = new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-Command",
                "[Console]::OutputEncoding=[System.Text.Encoding]::UTF8; $OutputEncoding=[System.Text.Encoding]::UTF8; " + command
        );
        builder.directory(workspaceRoot.toFile());
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            boolean finished = process.waitFor(commandTimeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new ResponseStatusException(BAD_REQUEST, "命令执行超时");
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            return "exitCode=" + process.exitValue() + "\n" + truncate(output);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "命令执行失败");
        } catch (IOException exception) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "命令执行失败");
        }
    }

    public String gitStatus() {
        return runCommand("git status --short");
    }

    public String gitDiff() {
        return runCommand("git diff --stat; git diff");
    }

    public String runTests(String projectPath, String command) {
        return runTests(projectPath, command, false);
    }

    public String runTests(String projectPath, String command, boolean allowUnauthorizedPath) {
        String safeProjectPath = projectPath == null || projectPath.isBlank() ? "." : projectPath;
        Path project = resolve(safeProjectPath, allowUnauthorizedPath);
        if (!Files.isDirectory(project)) {
            throw new ResponseStatusException(BAD_REQUEST, "测试目录不存在");
        }
        String escapedPath = "'" + project.toString().replace("'", "''") + "'";
        String finalCommand = "Set-Location " + escapedPath + "; " + command;
        return runCommand(finalCommand);
    }

    private Stream<String> searchInFile(Path path, String keyword) {
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            return java.util.stream.IntStream.range(0, lines.size())
                    .filter(index -> lines.get(index).contains(keyword))
                    .mapToObj(index -> displayPath(path) + ":" + (index + 1) + " " + lines.get(index).trim());
        } catch (Exception ignored) {
            return Stream.empty();
        }
    }

    private String truncate(String content) {
        return content.length() <= maxReadCharacters
                ? content
                : content.substring(0, maxReadCharacters) + "\n...[内容已截断]";
    }

    private List<String> readSharedStrings(ZipFile zipFile) throws IOException {
        ZipEntry entry = zipFile.getEntry("xl/sharedStrings.xml");
        if (entry == null) {
            return List.of();
        }
        Document document = readXml(zipFile, entry);
        NodeList items = document.getElementsByTagNameNS("*", "si");
        List<String> values = new ArrayList<>();
        for (int index = 0; index < items.getLength(); index++) {
            Element item = (Element) items.item(index);
            NodeList texts = item.getElementsByTagNameNS("*", "t");
            StringBuilder text = new StringBuilder();
            for (int textIndex = 0; textIndex < texts.getLength(); textIndex++) {
                text.append(texts.item(textIndex).getTextContent());
            }
            values.add(text.toString());
        }
        return values;
    }

    private Map<String, String> readWorkbookRelationships(ZipFile zipFile) throws IOException {
        ZipEntry entry = zipFile.getEntry("xl/_rels/workbook.xml.rels");
        if (entry == null) {
            return Map.of();
        }
        Document document = readXml(zipFile, entry);
        NodeList relationships = document.getElementsByTagNameNS("*", "Relationship");
        Map<String, String> targets = new HashMap<>();
        for (int index = 0; index < relationships.getLength(); index++) {
            Element relationship = (Element) relationships.item(index);
            String id = relationship.getAttribute("Id");
            String target = relationship.getAttribute("Target");
            if (!id.isBlank() && !target.isBlank()) {
                String normalized = target.startsWith("/")
                        ? target.substring(1)
                        : "xl/" + target;
                targets.put(id, normalized.replace("\\", "/"));
            }
        }
        return targets;
    }

    private List<SheetInfo> readWorkbookSheets(ZipFile zipFile, Map<String, String> relationshipTargets) throws IOException {
        ZipEntry entry = zipFile.getEntry("xl/workbook.xml");
        if (entry == null) {
            return List.of(new SheetInfo("Sheet1", "xl/worksheets/sheet1.xml"));
        }
        Document document = readXml(zipFile, entry);
        NodeList sheetNodes = document.getElementsByTagNameNS("*", "sheet");
        List<SheetInfo> sheets = new ArrayList<>();
        for (int index = 0; index < sheetNodes.getLength(); index++) {
            Element sheet = (Element) sheetNodes.item(index);
            String name = sheet.getAttribute("name");
            String relationshipId = sheet.getAttribute("r:id");
            String target = relationshipTargets.get(relationshipId);
            if (target != null) {
                sheets.add(new SheetInfo(name.isBlank() ? "Sheet" + (index + 1) : name, target));
            }
        }
        return sheets.isEmpty() ? List.of(new SheetInfo("Sheet1", "xl/worksheets/sheet1.xml")) : sheets;
    }

    private String readSheet(ZipFile zipFile, String sheetPath, List<String> sharedStrings, int maxRows) throws IOException {
        ZipEntry entry = zipFile.getEntry(sheetPath);
        if (entry == null) {
            return "[工作表文件不存在：" + sheetPath + "]\n";
        }
        Document document = readXml(zipFile, entry);
        NodeList rowNodes = document.getElementsByTagNameNS("*", "row");
        StringBuilder builder = new StringBuilder();
        int rowCount = Math.min(rowNodes.getLength(), maxRows);
        for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            Element row = (Element) rowNodes.item(rowIndex);
            Map<Integer, String> cells = readRowCells(row, sharedStrings);
            if (cells.isEmpty()) {
                continue;
            }
            int maxColumn = cells.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
            List<String> values = new ArrayList<>();
            for (int column = 1; column <= maxColumn; column++) {
                values.add(cells.getOrDefault(column, ""));
            }
            builder.append("| ").append(String.join(" | ", values)).append(" |\n");
        }
        if (rowNodes.getLength() > maxRows) {
            builder.append("...[仅展示前 ").append(maxRows).append(" 行，共约 ").append(rowNodes.getLength()).append(" 行]\n");
        }
        return builder.isEmpty() ? "[空工作表]\n" : builder.toString();
    }

    private Map<Integer, String> readRowCells(Element row, List<String> sharedStrings) {
        NodeList cellNodes = row.getElementsByTagNameNS("*", "c");
        Map<Integer, String> cells = new HashMap<>();
        for (int index = 0; index < cellNodes.getLength(); index++) {
            Element cell = (Element) cellNodes.item(index);
            String cellReference = cell.getAttribute("r");
            int columnIndex = columnIndex(cellReference);
            if (columnIndex < 1) {
                columnIndex = index + 1;
            }
            cells.put(columnIndex, readCellValue(cell, sharedStrings));
        }
        return cells;
    }

    private String readCellValue(Element cell, List<String> sharedStrings) {
        String type = cell.getAttribute("t");
        if ("inlineStr".equals(type)) {
            NodeList texts = cell.getElementsByTagNameNS("*", "t");
            StringBuilder builder = new StringBuilder();
            for (int index = 0; index < texts.getLength(); index++) {
                builder.append(texts.item(index).getTextContent());
            }
            return cleanCell(builder.toString());
        }
        String value = firstChildText(cell, "v");
        if ("s".equals(type)) {
            try {
                int sharedStringIndex = Integer.parseInt(value);
                return sharedStringIndex >= 0 && sharedStringIndex < sharedStrings.size()
                        ? cleanCell(sharedStrings.get(sharedStringIndex))
                        : "";
            } catch (NumberFormatException exception) {
                return "";
            }
        }
        if ("b".equals(type)) {
            return "1".equals(value) ? "TRUE" : "FALSE";
        }
        return cleanCell(value);
    }

    private String firstChildText(Element element, String localName) {
        NodeList nodes = element.getElementsByTagNameNS("*", localName);
        if (nodes.getLength() == 0) {
            return "";
        }
        Node node = nodes.item(0);
        return node == null ? "" : node.getTextContent();
    }

    private int columnIndex(String cellReference) {
        int value = 0;
        for (int index = 0; index < cellReference.length(); index++) {
            char character = Character.toUpperCase(cellReference.charAt(index));
            if (character < 'A' || character > 'Z') {
                break;
            }
            value = value * 26 + (character - 'A' + 1);
        }
        return value;
    }

    private String cleanCell(String value) {
        return value == null ? "" : value.replace("\r", " ").replace("\n", " ").replace("|", "\\|").trim();
    }

    private Document readXml(ZipFile zipFile, ZipEntry entry) throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            byte[] bytes = zipFile.getInputStream(entry).readAllBytes();
            return factory.newDocumentBuilder().parse(new InputSource(new ByteArrayInputStream(bytes)));
        } catch (Exception exception) {
            throw new IOException("解析 Excel XML 失败", exception);
        }
    }

    public boolean isPathAllowed(String relativePath) {
        return isPathAllowed(resolveRaw(relativePath));
    }

    private Path resolve(String relativePath) {
        return resolve(relativePath, false);
    }

    private Path resolve(String relativePath, boolean allowUnauthorizedPath) {
        Path resolved = resolveRaw(relativePath);
        if (!allowUnauthorizedPath && !isPathAllowed(resolved)) {
            throw new ResponseStatusException(BAD_REQUEST, "不允许访问授权目录之外的路径");
        }
        return resolved;
    }

    private Path resolveRaw(String relativePath) {
        String pathText = relativePath == null || relativePath.isBlank() ? "." : relativePath;
        Path knownLocation = resolveKnownLocation(pathText);
        if (knownLocation != null) {
            return knownLocation.toAbsolutePath().normalize();
        }
        Path input = Path.of(expandHome(pathText));
        return (input.isAbsolute() ? input : workspaceRoot.resolve(input)).toAbsolutePath().normalize();
    }

    private Path resolveKnownLocation(String rawPath) {
        String value = rawPath == null ? "" : rawPath.trim();
        if (value.isBlank()) {
            return null;
        }
        String normalized = value.replace("\\", "/");
        String lower = normalized.toLowerCase(Locale.ROOT);

        Path userHome = Path.of(System.getProperty("user.home"));
        Path desktop = userHome.resolve("Desktop");
        Path downloads = userHome.resolve("Downloads");
        Path documents = userHome.resolve("Documents");

        Path desktopPath = matchKnownLocation(normalized, lower, desktop, "桌面", "desktop", "我的桌面", "电脑桌面", "我的电脑桌面");
        if (desktopPath != null) {
            return desktopPath;
        }
        Path downloadsPath = matchKnownLocation(normalized, lower, downloads, "下载", "下载目录", "downloads");
        if (downloadsPath != null) {
            return downloadsPath;
        }
        return matchKnownLocation(normalized, lower, documents, "文档", "我的文档", "documents");
    }

    private Path matchKnownLocation(String normalized, String lower, Path root, String... aliases) {
        for (String alias : aliases) {
            String aliasLower = alias.toLowerCase(Locale.ROOT);
            if (lower.equals(aliasLower)) {
                return root;
            }
            if (lower.startsWith(aliasLower + "/")) {
                return root.resolve(normalized.substring(alias.length() + 1));
            }
        }
        return null;
    }

    private String expandHome(String pathText) {
        String value = pathText.trim();
        if (value.equals("~")) {
            return System.getProperty("user.home");
        }
        if (value.startsWith("~/") || value.startsWith("~\\")) {
            return System.getProperty("user.home") + value.substring(1);
        }
        String userProfile = System.getenv("USERPROFILE");
        if (userProfile != null && !userProfile.isBlank()) {
            value = value.replace("%USERPROFILE%", userProfile);
        }
        return value;
    }

    private boolean isPathAllowed(Path path) {
        return allowedRoots.stream().anyMatch(path::startsWith);
    }

    private List<Path> buildAllowedRoots(String configuredRoots) {
        List<Path> roots = new ArrayList<>();
        roots.add(workspaceRoot);
        if (configuredRoots != null && !configuredRoots.isBlank()) {
            for (String root : configuredRoots.split("[;,]")) {
                if (!root.isBlank()) {
                    roots.add(Path.of(root.trim()).toAbsolutePath().normalize());
                }
            }
        }
        return roots.stream().distinct().toList();
    }

    private String displayPath(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        if (normalized.startsWith(workspaceRoot)) {
            return workspaceRoot.relativize(normalized).toString();
        }
        return normalized.toString();
    }

    private record SheetInfo(String name, String path) {
    }
}
