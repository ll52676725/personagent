package com.example.agentplatform.tools.service;

import com.example.agentplatform.tools.dto.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistryCleanerService {

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    @Value("${agent.platform.default-model:spring-ai}")
    private String modelName;

    @Value("${agent.platform.ai.fallback-enabled:true}")
    private boolean fallbackEnabled;

    private static final String DISCLAIMER = """
        免责声明：
        本工具仅提供注册表分析功能，不会直接修改您的系统注册表。
        生成的清理脚本仅供参考，请在执行前务必备份注册表并创建系统还原点。
        请在执行任何注册表修改操作前，务必确认您了解操作的后果。
        对于因使用本工具生成的脚本导致的任何问题，本工具不承担任何责任。
        """;

    private static final Map<String, String> CATEGORY_LABEL_MAP = new LinkedHashMap<>();
    private static final Map<String, String> ISSUE_TYPE_LABEL_MAP = new HashMap<>();
    private static final Map<String, String> CATEGORY_SEVERITY_MAP = new HashMap<>();

    static {
        CATEGORY_LABEL_MAP.put("file_association", "文件关联");
        CATEGORY_LABEL_MAP.put("startup_entry", "启动项");
        CATEGORY_LABEL_MAP.put("uninstall_info", "卸载残留");
        CATEGORY_LABEL_MAP.put("com_component", "COM组件");
        CATEGORY_LABEL_MAP.put("service_entry", "服务项");
        CATEGORY_LABEL_MAP.put("help_file", "帮助文件");
        CATEGORY_LABEL_MAP.put("dll_reference", "DLL引用");
        CATEGORY_LABEL_MAP.put("menu_extension", "菜单扩展");
        CATEGORY_LABEL_MAP.put("recent_docs", "最近文档");
        CATEGORY_LABEL_MAP.put("software_reg", "软件注册");

        ISSUE_TYPE_LABEL_MAP.put("missing_file", "文件不存在");
        ISSUE_TYPE_LABEL_MAP.put("missing_program", "程序不存在");
        ISSUE_TYPE_LABEL_MAP.put("broken_reference", "无效引用");
        ISSUE_TYPE_LABEL_MAP.put("orphan_entry", "孤立项");
        ISSUE_TYPE_LABEL_MAP.put("unused_entry", "未使用项");
        ISSUE_TYPE_LABEL_MAP.put("duplicate_entry", "重复项");

        CATEGORY_SEVERITY_MAP.put("file_association", "low");
        CATEGORY_SEVERITY_MAP.put("startup_entry", "medium");
        CATEGORY_SEVERITY_MAP.put("uninstall_info", "low");
        CATEGORY_SEVERITY_MAP.put("com_component", "high");
        CATEGORY_SEVERITY_MAP.put("service_entry", "high");
        CATEGORY_SEVERITY_MAP.put("help_file", "low");
        CATEGORY_SEVERITY_MAP.put("dll_reference", "medium");
        CATEGORY_SEVERITY_MAP.put("menu_extension", "low");
        CATEGORY_SEVERITY_MAP.put("recent_docs", "low");
        CATEGORY_SEVERITY_MAP.put("software_reg", "low");
    }

    private final List<RegistryScanRule> scanRules = Arrays.asList(
        new RegistryScanRule(
            "file_association", "HKCU\\Software\\Classes", null,
            "检查无效的文件扩展名关联", "missing_file",
            "文件扩展名关联指向不存在的程序文件，清理后可修复打开方式菜单，提升系统响应速度",
            "low", this::checkFileAssociation
        ),
        new RegistryScanRule(
            "startup_entry", "HKLM\\Software\\Microsoft\\Windows\\CurrentVersion\\Run", null,
            "检查无效的启动项", "missing_program",
            "启动项指向的程序已卸载，清理后可加快系统启动速度",
            "low", this::checkStartupEntry
        ),
        new RegistryScanRule(
            "startup_entry", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run", null,
            "检查无效的用户启动项", "missing_program",
            "用户启动项指向的程序已卸载，清理后可加快系统启动速度",
            "low", this::checkStartupEntry
        ),
        new RegistryScanRule(
            "uninstall_info", "HKLM\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall", null,
            "检查已卸载程序残留", "orphan_entry",
            "软件已卸载但注册表中仍保留卸载信息，清理后可使程序和功能列表更整洁",
            "low", this::checkUninstallEntry
        ),
        new RegistryScanRule(
            "uninstall_info", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall", null,
            "检查用户已卸载程序残留", "orphan_entry",
            "软件已卸载但注册表中仍保留卸载信息",
            "low", this::checkUninstallEntry
        ),
        new RegistryScanRule(
            "com_component", "HKCR\\CLSID", "InprocServer32",
            "检查无效的COM组件", "missing_file",
            "COM组件指向的DLL文件不存在，可能导致程序运行异常",
            "high", this::checkComComponent
        ),
        new RegistryScanRule(
            "service_entry", "HKLM\\System\\CurrentControlSet\\Services", "ImagePath",
            "检查无效的系统服务", "missing_file",
            "服务项指向的程序文件不存在，清理后可减少系统错误",
            "high", this::checkServiceEntry
        ),
        new RegistryScanRule(
            "recent_docs", "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\RecentDocs", null,
            "检查最近文档历史", "unused_entry",
            "最近访问的文档历史记录，清理后可保护隐私",
            "low", this::checkRecentDocs
        ),
        new RegistryScanRule(
            "menu_extension", "HKCR\\*\\shellex\\ContextMenuHandlers", null,
            "检查无效的右键菜单扩展", "missing_file",
            "右键菜单扩展指向的程序不存在，清理后可加快右键菜单响应",
            "low", this::checkMenuExtension
        ),
        new RegistryScanRule(
            "dll_reference", "HKLM\\Software\\Microsoft\\Windows\\CurrentVersion\\SharedDLLs", null,
            "检查无效的DLL引用", "broken_reference",
            "共享DLL引用计数为0或指向不存在的文件，清理后可释放系统资源",
            "medium", this::checkDllReference
        )
    );

    public RegistryAnalysisResultDTO analyzeRegistry() {
        long startTime = System.currentTimeMillis();
        List<RegistryIssueDTO> allIssues = new ArrayList<>();

        for (RegistryScanRule rule : scanRules) {
            try {
                List<RegistryIssueDTO> issues = rule.scanner.apply(rule);
                allIssues.addAll(issues);
            } catch (Exception e) {
                log.warn("扫描注册表规则失败: {}", rule.description, e);
            }
        }

        allIssues.sort((a, b) -> {
            int severityCompare = getSeverityOrder(b.getSeverity()) - getSeverityOrder(a.getSeverity());
            if (severityCompare != 0) return severityCompare;
            return a.getCategory().compareTo(b.getCategory());
        });

        Map<String, Integer> categoryStats = new LinkedHashMap<>();
        Map<String, Integer> severityStats = new LinkedHashMap<>();
        for (RegistryIssueDTO issue : allIssues) {
            categoryStats.merge(issue.getCategory(), 1, Integer::sum);
            severityStats.merge(issue.getSeverity(), 1, Integer::sum);
        }

        Map<String, Integer> sortedCategoryStats = new LinkedHashMap<>();
        CATEGORY_LABEL_MAP.keySet().forEach(key -> {
            if (categoryStats.containsKey(key)) {
                sortedCategoryStats.put(key, categoryStats.get(key));
            }
        });

        Map<String, Integer> sortedSeverityStats = new LinkedHashMap<>();
        sortedSeverityStats.put("high", severityStats.getOrDefault("high", 0));
        sortedSeverityStats.put("medium", severityStats.getOrDefault("medium", 0));
        sortedSeverityStats.put("low", severityStats.getOrDefault("low", 0));

        String summary = buildSummary(allIssues.size(), sortedSeverityStats);

        return RegistryAnalysisResultDTO.builder()
                .summary(summary)
                .totalIssues(allIssues.size())
                .analysisDurationMs(System.currentTimeMillis() - startTime)
                .categoryStats(sortedCategoryStats)
                .severityStats(sortedSeverityStats)
                .issues(allIssues)
                .disclaimer(DISCLAIMER)
                .build();
    }

    private int getSeverityOrder(String severity) {
        return switch (severity) {
            case "high" -> 3;
            case "medium" -> 2;
            case "low" -> 1;
            default -> 0;
        };
    }

    private String buildSummary(int totalIssues, Map<String, Integer> severityStats) {
        int high = severityStats.getOrDefault("high", 0);
        int medium = severityStats.getOrDefault("medium", 0);
        int low = severityStats.getOrDefault("low", 0);

        if (totalIssues == 0) {
            return "恭喜！您的注册表状态良好，未发现明显的冗余项。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("共发现 ").append(totalIssues).append(" 个注册表问题，");
        if (high > 0) sb.append("其中高危 ").append(high).append(" 个，");
        if (medium > 0) sb.append("中危 ").append(medium).append(" 个，");
        if (low > 0) sb.append("低危 ").append(low).append(" 个。");
        sb.append("建议优先处理高危和中危项以提升系统性能。");

        return sb.toString();
    }

    public CleanupScriptDTO generateCleanupScript(GenerateScriptRequestDTO request, List<RegistryIssueDTO> allIssues) {
        List<RegistryIssueDTO> selectedIssues = allIssues.stream()
                .filter(issue -> request.getSelectedIssueIds().contains(issue.getId()))
                .toList();

        if (selectedIssues.isEmpty()) {
            throw new IllegalArgumentException("未选择任何要清理的注册表项");
        }

        String scriptContent;
        String scriptType = request.getScriptType() != null ? request.getScriptType() : "reg";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        if ("bat".equalsIgnoreCase(scriptType)) {
            scriptContent = generateBatchScript(selectedIssues, request.isIncludeBackup(), timestamp);
        } else {
            scriptContent = generateRegScript(selectedIssues, request.isIncludeBackup(), timestamp);
        }

        return CleanupScriptDTO.builder()
                .scriptName("registry_cleanup_" + timestamp + "." + ("bat".equalsIgnoreCase(scriptType) ? "bat" : "reg"))
                .scriptContent(scriptContent)
                .scriptType(scriptType)
                .encoding("UTF-16LE")
                .issueCount(selectedIssues.size())
                .warning("重要提示：执行前请务必备份注册表并创建系统还原点！")
                .usageInstructions("""
                    使用说明：
                    1. 双击执行脚本前，请确保已备份注册表
                    2. 右键点击脚本，选择"以管理员身份运行"
                    3. 执行完成后建议重启电脑
                    4. 如遇问题，可使用备份的注册表文件恢复
                    """)
                .build();
    }

    private String generateRegScript(List<RegistryIssueDTO> issues, boolean includeBackup, String timestamp) {
        StringBuilder sb = new StringBuilder();
        sb.append("Windows Registry Editor Version 5.00\r\n\r\n");
        sb.append("; ================================================\r\n");
        sb.append("; 注册表清理脚本 - 生成时间: ").append(LocalDateTime.now()).append("\r\n");
        sb.append("; 包含问题数: ").append(issues.size()).append("\r\n");
        sb.append("; ================================================\r\n\r\n");

        if (includeBackup) {
            sb.append("; 建议在执行此脚本前，请先运行以下命令备份注册表：\r\n");
            sb.append("; reg export HKLM /ea \"%USERPROFILE%\\registry_backup_").append(timestamp).append(".reg\r\n\r\n");
        }

        Map<String, List<RegistryIssueDTO>> groupedByPath = new LinkedHashMap<>();
        for (RegistryIssueDTO issue : issues) {
            groupedByPath.computeIfAbsent(issue.getRegistryPath(), k -> new ArrayList<>()).add(issue);
        }

        for (Map.Entry<String, List<RegistryIssueDTO>> entry : groupedByPath.entrySet()) {
            String path = entry.getKey();
            List<RegistryIssueDTO> pathIssues = entry.getValue();

            sb.append("; ").append(CATEGORY_LABEL_MAP.getOrDefault(pathIssues.get(0).getCategory(), pathIssues.get(0).getCategory())).append("\r\n");
            sb.append("; 路径: ").append(path).append("\r\n");

            for (RegistryIssueDTO issue : pathIssues) {
                sb.append(";   - ").append(issue.getDescription()).append("\r\n");
                sb.append(";     原因: ").append(issue.getReason()).append("\r\n");
                sb.append(";     风险等级: ").append(issue.getSeverity()).append("\r\n");

                if (issue.getValueName() != null && !issue.getValueName().isEmpty()) {
                    sb.append("[-").append(path).append("]\r\n");
                    sb.append("\"").append(issue.getValueName()).append("\"=-\r\n\r\n");
                } else {
                    sb.append("[-").append(path).append("]\r\n\r\n");
                }
            }
            sb.append("\r\n");
        }

        return sb.toString();
    }

    private String generateBatchScript(List<RegistryIssueDTO> issues, boolean includeBackup, String timestamp) {
        StringBuilder sb = new StringBuilder();
        sb.append("@echo off\r\n");
        sb.append("chcp 65001 >nul\r\n\r\n");
        sb.append("echo ================================================\r\n");
        sb.append("echo 注册表清理脚本\r\n");
        sb.append("echo 生成时间: ").append(LocalDateTime.now()).append("\r\n");
        sb.append("echo 包含问题数: ").append(issues.size()).append("\r\n");
        sb.append("echo ================================================\r\n");
        sb.append("echo.\r\n");

        sb.append("echo [警告] 执行前请确保已备份注册表！\r\n");
        sb.append("echo 建议先创建系统还原点\r\n");
        sb.append("echo.\r\n");

        if (includeBackup) {
            sb.append("echo [1/3] 正在备份注册表...\r\n");
            sb.append("reg export HKCU \"%USERPROFILE%\\registry_backup_").append(timestamp).append(".reg\" /y\r\n");
            sb.append("if errorlevel 1 (\r\n");
            sb.append("    echo 备份失败，请手动备份后再运行此脚本\r\n");
            sb.append("    pause\r\n");
            sb.append("    exit /b 1\r\n");
            sb.append(")\r\n");
            sb.append("echo 备份完成，备份文件: %USERPROFILE%\\registry_backup_").append(timestamp).append(".reg\r\n");
            sb.append("echo.\r\n");
        }

        sb.append("echo [2/3] 正在清理注册表...\r\n");
        sb.append("echo.\r\n");

        for (RegistryIssueDTO issue : issues) {
            String path = issue.getRegistryPath();
            String valueName = issue.getValueName();
            String category = CATEGORY_LABEL_MAP.getOrDefault(issue.getCategory(), issue.getCategory());

            sb.append("echo 正在清理: ").append(category).append(" - ").append(escapeBatch(issue.getDescription())).append("\r\n");

            if (valueName != null && !valueName.isEmpty()) {
                sb.append("reg delete \"").append(path).append("\" /v \"").append(escapeBatch(valueName)).append("\" /f\r\n");
            } else {
                sb.append("reg delete \"").append(path).append("\" /f\r\n");
            }
            sb.append("echo.\r\n");
        }

        sb.append("echo [3/3] 清理完成！\r\n");
        sb.append("echo.\r\n");
        sb.append("echo 建议重启电脑以使更改生效。\r\n");
        sb.append("echo 如遇问题，可使用备份的注册表文件恢复。\r\n");
        sb.append("pause\r\n");

        return sb.toString();
    }

    private String escapeBatch(String input) {
        return input.replace("\"", "\\\"").replace("&", "^&").replace("|", "^|").replace("<", "^<").replace(">", "^>");
    }

    private List<RegistryIssueDTO> checkFileAssociation(RegistryScanRule rule) {
        List<RegistryIssueDTO> issues = new ArrayList<>();
        List<String> subKeys = queryRegistrySubKeys(rule.registryPath);

        for (String subKey : subKeys) {
            if (!subKey.startsWith(".")) continue;

            String fullPath = rule.registryPath + "\\" + subKey;
            List<String> values = queryRegistryValues(fullPath);

            for (String value : values) {
                if (value.toLowerCase().contains("\\shell\\open\\command") || value.toLowerCase().contains("\\shell\\edit\\command")) {
                    String commandPath = rule.registryPath + "\\" + subKey + "\\shell\\open\\command";
                    String commandValue = queryRegistryValue(commandPath, "");
                    String exePath = extractExePath(commandValue);
                    if (exePath != null && !fileExists(exePath)) {
                        issues.add(createIssue(rule, subKey, "(默认)", "文件关联指向不存在的程序: " + exePath, commandPath));
                    }
                }
            }
        }
        return issues;
    }

    private List<RegistryIssueDTO> checkStartupEntry(RegistryScanRule rule) {
        List<RegistryIssueDTO> issues = new ArrayList<>();
        List<String> values = queryRegistryValues(rule.registryPath);

        for (String valueName : values) {
            if (valueName.equalsIgnoreCase("(默认)")) continue;

            String valueData = queryRegistryValue(rule.registryPath, valueName);
            String exePath = extractExePath(valueData);

            if (exePath != null && !fileExists(exePath)) {
                issues.add(createIssue(rule, valueName, valueName, "启动项指向不存在的程序: " + exePath, rule.registryPath));
            }
        }

        return issues;
    }

    private List<RegistryIssueDTO> checkUninstallEntry(RegistryScanRule rule) {
        List<RegistryIssueDTO> issues = new ArrayList<>();
        List<String> subKeys = queryRegistrySubKeys(rule.registryPath);

        for (String subKey : subKeys) {
            String fullPath = rule.registryPath + "\\" + subKey;
            String displayName = queryRegistryValue(fullPath, "DisplayName");
            String uninstallString = queryRegistryValue(fullPath, "UninstallString");
            String installLocation = queryRegistryValue(fullPath, "InstallLocation");

            if (displayName.isEmpty()) continue;

            boolean isOrphan = true;

            if (!uninstallString.isEmpty()) {
                String uninstallExe = extractExePath(uninstallString);
                if (uninstallExe != null && fileExists(uninstallExe)) {
                    isOrphan = false;
                }
            }

            if (isOrphan && !installLocation.isEmpty()) {
                if (fileExists(installLocation)) {
                    isOrphan = false;
                }
            }

            if (isOrphan) {
                issues.add(createIssue(rule, subKey, "", "卸载残留: " + displayName + " 安装路径或卸载程序不存在", fullPath));
            }
        }

        return issues;
    }

    private List<RegistryIssueDTO> checkComComponent(RegistryScanRule rule) {
        List<RegistryIssueDTO> issues = new ArrayList<>();
        List<String> subKeys = queryRegistrySubKeys(rule.registryPath);
        int count = 0;

        for (String subKey : subKeys) {
            if (count >= 20) break;
            String fullPath = rule.registryPath + "\\" + subKey + "\\" + rule.valueName;
            String dllPath = queryRegistryValue(fullPath, "");

            if (!dllPath.isEmpty()) {
                String actualPath = extractExePath(dllPath);
                if (actualPath != null && !fileExists(actualPath)) {
                    count++;
                    issues.add(createIssue(rule, subKey, "", "COM组件指向不存在的DLL: " + actualPath, fullPath));
                }
            }
        }

        return issues;
    }

    private List<RegistryIssueDTO> checkServiceEntry(RegistryScanRule rule) {
        List<RegistryIssueDTO> issues = new ArrayList<>();
        List<String> subKeys = queryRegistrySubKeys(rule.registryPath);
        int count = 0;

        for (String subKey : subKeys) {
            if (count >= 10) break;

            String fullPath = rule.registryPath + "\\" + subKey;
            String imagePath = queryRegistryValue(fullPath, rule.valueName);
            String startType = queryRegistryValue(fullPath, "Start");

            if (!imagePath.isEmpty() && !"4".equals(startType)) {
                String actualPath = extractExePath(imagePath);
                if (actualPath != null && !fileExists(actualPath)) {
                    count++;
                    issues.add(createIssue(rule, subKey, rule.valueName, "服务项指向不存在的程序: " + actualPath, fullPath));
                }
            }
        }

        return issues;
    }

    private List<RegistryIssueDTO> checkRecentDocs(RegistryScanRule rule) {
        List<RegistryIssueDTO> issues = new ArrayList<>();
        List<String> subKeys = queryRegistrySubKeys(rule.registryPath);

        for (String subKey : subKeys) {
            String fullPath = rule.registryPath + "\\" + subKey;
            List<String> values = queryRegistryValues(fullPath);
            int docCount = 0;

            for (String valueName : values) {
                if (!valueName.equalsIgnoreCase("MRUListEx")) continue;
                docCount++;
            }

            if (docCount > 0) {
                issues.add(createIssue(rule, subKey, "", "最近访问文档历史记录(" + docCount + "条)", fullPath));
            }
        }

        return issues;
    }

    private List<RegistryIssueDTO> checkMenuExtension(RegistryScanRule rule) {
        List<RegistryIssueDTO> issues = new ArrayList<>();
        List<String> subKeys = queryRegistrySubKeys(rule.registryPath);

        for (String subKey : subKeys) {
            String handlerPath = rule.registryPath + "\\" + subKey;
            String defaultVal = queryRegistryValue(handlerPath, "");
            if (!defaultVal.isEmpty()) continue;

            String clsidPath = "HKCR\\CLSID\\" + defaultVal + "\\InprocServer32";
            String dllPath = queryRegistryValue(clsidPath, "");

            if (!dllPath.isEmpty()) {
                String actualPath = extractExePath(dllPath);
                if (actualPath != null && !fileExists(actualPath)) {
                    issues.add(createIssue(rule, subKey, "", "右键菜单扩展指向不存在的DLL: " + actualPath, handlerPath));
                }
            }
        }

        return issues;
    }

    private List<RegistryIssueDTO> checkDllReference(RegistryScanRule rule) {
        List<RegistryIssueDTO> issues = new ArrayList<>();
        List<String> values = queryRegistryValues(rule.registryPath);

        for (String valueName : values) {
            if (valueName.equalsIgnoreCase("(默认)")) continue;

            String countValue = queryRegistryValue(rule.registryPath, valueName);
            try {
                int count = Integer.parseInt(countValue);
                if (count == 0 || !fileExists(valueName)) {
                    issues.add(createIssue(rule, valueName, valueName,
                        count == 0 ? "DLL引用计数为0" : "DLL文件不存在: " + valueName,
                        rule.registryPath));
                }
            } catch (NumberFormatException ignored) {
            }
        }

        return issues;
    }

    private RegistryIssueDTO createIssue(RegistryScanRule rule, String id, String valueName, String description, String registryPath) {
        String categoryLabel = CATEGORY_LABEL_MAP.getOrDefault(rule.category, rule.category);
        String issueTypeLabel = ISSUE_TYPE_LABEL_MAP.getOrDefault(rule.issueType, rule.issueType);
        String severity = CATEGORY_SEVERITY_MAP.getOrDefault(rule.category, "low");

        String regCommand = valueName != null && !valueName.isEmpty()
            ? "reg delete \"" + registryPath + "\" /v \"" + valueName + "\" /f"
            : "reg delete \"" + registryPath + "\" /f";

        return RegistryIssueDTO.builder()
                .id(UUID.randomUUID().toString())
                .category(rule.category)
                .categoryLabel(categoryLabel)
                .registryPath(registryPath)
                .valueName(valueName)
                .issueType(rule.issueType)
                .issueTypeLabel(issueTypeLabel)
                .description(description)
                .reason(rule.reasonTemplate)
                .riskLevel(rule.riskLevel)
                .severity(severity)
                .selected(true)
                .regCommand(regCommand)
                .build();
    }

    private List<String> queryRegistrySubKeys(String path) {
        return executeRegCommand("reg query \"" + path + "\"");
    }

    private List<String> queryRegistryValues(String path) {
        List<String> result = new ArrayList<>();
        List<String> lines = executeRegCommand("reg query \"" + path + "\"");
        Pattern pattern = Pattern.compile("^\\s+(\\S+)\\s+(REG_\\S+)\\s+");
        for (String line : lines) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                String valueName = matcher.group(1).trim();
                if (!valueName.isEmpty()) {
                    result.add(valueName);
                }
            }
        }
        return result;
    }

    private String queryRegistryValue(String path, String valueName) {
        String command = valueName == null || valueName.isEmpty()
            ? "reg query \"" + path + "\" /ve"
            : "reg query \"" + path + "\" /v \"" + valueName + "\"";
        List<String> lines = executeRegCommand(command);
        for (String line : lines) {
            if (line.contains("REG_")) {
                int idx = line.indexOf("REG_");
                if (idx > 0) {
                    int valueStart = line.indexOf(" ", idx + 5);
                    if (valueStart > 0) {
                        return line.substring(valueStart).trim();
                    }
                }
                int lastSpace = line.lastIndexOf(" ");
                if (lastSpace > idx) {
                    return line.substring(lastSpace).trim();
                }
            }
        }
        return "";
    }

    private List<String> executeRegCommand(String command) {
        List<String> result = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), Charset.forName("GBK")))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty() && !line.startsWith("HKEY_")) {
                        result.add(line.trim());
                    }
                }
            }

            process.waitFor();
        } catch (Exception e) {
            log.debug("执行注册表命令失败: {}", command, e);
        }
        return result;
    }

    private String extractExePath(String command) {
        if (command == null || command.trim().isEmpty()) {
            return null;
        }

        String trimmed = command.trim();

        if (trimmed.startsWith("\"")) {
            int endQuote = trimmed.indexOf("\"", 1);
            if (endQuote > 1) {
                return trimmed.substring(1, endQuote);
            }
        }

        int exeIdx = trimmed.toLowerCase().indexOf(".exe");
        if (exeIdx > 0) {
            int spaceIdx = trimmed.indexOf(" ", exeIdx);
            if (spaceIdx < 0) spaceIdx = trimmed.length();
            return trimmed.substring(0, spaceIdx);
        }

        int dllIdx = trimmed.toLowerCase().indexOf(".dll");
        if (dllIdx > 0) {
            int spaceIdx = trimmed.indexOf(" ", dllIdx);
            if (spaceIdx < 0) spaceIdx = trimmed.length();
            return trimmed.substring(0, spaceIdx);
        }

        return null;
    }

    private boolean fileExists(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        try {
            String normalizedPath = path.replace("\"", "").trim();
            normalizedPath = expandEnvironmentVariables(normalizedPath);
            Path filePath = Paths.get(normalizedPath);
            return Files.exists(filePath);
        } catch (Exception e) {
            return false;
        }
    }

    private String expandEnvironmentVariables(String path) {
        Pattern pattern = Pattern.compile("%([^%]+)%");
        Matcher matcher = pattern.matcher(path);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String envVar = matcher.group(1);
            String envValue = System.getenv(envVar);
            matcher.appendReplacement(sb, envValue != null ? Matcher.quoteReplacement(envValue) : matcher.group(0));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static class RegistryScanRule {
        String category;
        String registryPath;
        String valueName;
        String description;
        String issueType;
        String reasonTemplate;
        String riskLevel;
        java.util.function.Function<RegistryScanRule, List<RegistryIssueDTO>> scanner;

        RegistryScanRule(String category, String registryPath, String valueName,
                      String description, String issueType, String reasonTemplate,
                      String riskLevel,
                      java.util.function.Function<RegistryScanRule, List<RegistryIssueDTO>> scanner) {
            this.category = category;
            this.registryPath = registryPath;
            this.valueName = valueName;
            this.description = description;
            this.issueType = issueType;
            this.reasonTemplate = reasonTemplate;
            this.riskLevel = riskLevel;
            this.scanner = scanner;
        }
    }

    private static final String AI_REGISTRY_SYSTEM_PROMPT = """
        你是一个专业的Windows注册表分析专家，拥有10年以上的Windows系统维护经验。
        你精通Windows注册表的结构、各个根键的作用，以及各类注册表项对系统性能和稳定性的影响。
        
        你的任务：
        1. 根据提供的注册表扫描数据，进行深入分析和诊断
        2. 评估系统注册表的健康状况，给出0-100的健康评分
        3. 按优先级提供清理建议，每项建议需要说明风险和注意事项
        4. 给出系统优化建议，帮助用户提升系统性能和稳定性
        
        分析原则：
        - 安全第一：始终把系统稳定性放在首位，对于可能影响系统正常运行的项目要标注高风险
        - 精准分类：按照问题类别（启动项、文件关联、COM组件、服务项等）进行分组分析
        - 风险评估：
          * high: 高风险，清理后可能导致系统或软件无法正常运行，需要用户明确确认
          * medium: 中风险，需要用户了解清理的后果，建议备份后操作
          * low: 低风险，一般不会影响系统运行，可放心清理
        
        输出要求：
        请严格按照以下JSON格式返回，不要有任何其他文字说明：
        {
          "summary": "对注册表整体状况的简要总结，200字以内",
          "systemHealthScore": "0-100的数字字符串，如：85",
          "systemHealthLevel": "优秀/良好/一般/较差/危险",
          "analysisInsight": "深入的分析见解，说明发现的主要问题及其可能的影响",
          "optimizationAdvice": "系统优化建议，包括注册表维护、系统性能优化等方面",
          "suggestions": [
            {
              "category": "问题分类，如：startup_entry/file_association/com_component等",
              "categoryLabel": "分类中文名称，如：启动项/文件关联/COM组件等",
              "title": "建议标题，简明扼要",
              "description": "详细说明问题的具体表现和影响",
              "registryPaths": ["相关的注册表路径数组"],
              "issueCount": "此类问题的数量",
              "riskLevel": "low/medium/high",
              "action": "建议采取的操作，如：清理/谨慎清理/建议保留",
              "reason": "为什么给出这个建议的专业解释",
              "priority": "优先级1-10，数字越大越应该优先处理",
              "impact": "清理后对系统的预期影响，如：加快启动速度/减少系统错误/释放内存等",
              "precaution": "注意事项，清理前需要了解的风险和准备工作"
            }
          ]
        }
        
        注意：
        - 建议数量控制在5-10条之间，优先选择问题数量多、风险高、影响大的类别
        - 对于COM组件和服务项等高风险类别，要特别谨慎，给出详细的风险说明
        - 确保registryPaths数组中包含的路径是真实存在的扫描结果中的路径
        - 优先级排序：高风险且问题数量多的 > 中风险影响大的 > 低风险可安全清理的
        """;

    public RegistryAIAnalysisResultDTO aiAnalyzeRegistry() {
        long startTime = System.currentTimeMillis();

        RegistryAnalysisResultDTO analysisResult = analyzeRegistry();

        try {
            String registryDataJson = buildRegistryAnalysisJson(analysisResult);

            String userPrompt = String.format("""
                请分析以下Windows注册表扫描结果：
                
                注册表扫描数据：
                %s
                
                请基于以上数据，提供专业的注册表分析报告和清理建议。
                """, registryDataJson);

            ChatResponse response = callChatApi(AI_REGISTRY_SYSTEM_PROMPT, userPrompt);
            String content = response.getResult().getOutput().getContent();
            Integer tokens = response.getMetadata().getUsage() != null ?
                response.getMetadata().getUsage().getTotalTokens().intValue() : null;

            RegistryAIAnalysisResultDTO result = parseRegistryAIResponse(content, analysisResult);
            result.setModel(modelName);
            result.setTokens(tokens);
            result.setAnalysisDurationMs(System.currentTimeMillis() - startTime);

            log.info("AI注册表分析完成，token消耗: {}", tokens);
            return result;

        } catch (Exception e) {
            log.error("AI分析注册表失败", e);
            if (fallbackEnabled) {
                return fallbackAIRegistryAnalysis(analysisResult, startTime);
            }
            throw new RuntimeException("AI分析失败: " + e.getMessage());
        }
    }

    private String buildRegistryAnalysisJson(RegistryAnalysisResultDTO analysisResult) {
        try {
            Map<String, Object> data = new LinkedHashMap<>();

            Map<String, Object> overview = new LinkedHashMap<>();
            overview.put("totalIssues", analysisResult.getTotalIssues());
            overview.put("analysisDurationMs", analysisResult.getAnalysisDurationMs());
            overview.put("summary", analysisResult.getSummary());
            data.put("overview", overview);

            Map<String, Integer> severityStats = analysisResult.getSeverityStats();
            Map<String, Object> severity = new LinkedHashMap<>();
            severity.put("high", severityStats.getOrDefault("high", 0));
            severity.put("medium", severityStats.getOrDefault("medium", 0));
            severity.put("low", severityStats.getOrDefault("low", 0));
            data.put("severityStats", severity);

            data.put("categoryStats", analysisResult.getCategoryStats());

            Map<String, List<Map<String, Object>>> issuesByCategory = new LinkedHashMap<>();
            for (RegistryIssueDTO issue : analysisResult.getIssues()) {
                issuesByCategory.computeIfAbsent(issue.getCategory(), k -> new ArrayList<>()).add(Map.of(
                    "registryPath", issue.getRegistryPath(),
                    "valueName", issue.getValueName() != null ? issue.getValueName() : "",
                    "description", issue.getDescription(),
                    "issueType", issue.getIssueType(),
                    "issueTypeLabel", issue.getIssueTypeLabel(),
                    "severity", issue.getSeverity(),
                    "reason", issue.getReason()
                ));
            }

            List<Map<String, Object>> categories = new ArrayList<>();
            for (Map.Entry<String, List<Map<String, Object>>> entry : issuesByCategory.entrySet()) {
                String category = entry.getKey();
                List<Map<String, Object>> issues = entry.getValue();
                Map<String, Object> catInfo = new LinkedHashMap<>();
                catInfo.put("category", category);
                catInfo.put("categoryLabel", CATEGORY_LABEL_MAP.getOrDefault(category, category));
                catInfo.put("count", issues.size());
                catInfo.put("samplePaths", issues.stream()
                    .map(m -> (String) m.get("registryPath"))
                    .limit(5)
                    .collect(Collectors.toList()));
                catInfo.put("severityBreakdown", issues.stream()
                    .collect(Collectors.groupingBy(
                        m -> (String) m.get("severity"),
                        Collectors.counting()
                    )));
                categories.add(catInfo);
            }
            data.put("categories", categories);

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
        } catch (Exception e) {
            log.error("构建注册表分析JSON失败", e);
            return "{}";
        }
    }

    private RegistryAIAnalysisResultDTO parseRegistryAIResponse(String aiContent, RegistryAnalysisResultDTO analysisResult) {
        try {
            String jsonContent = extractJson(aiContent);
            JsonNode root = objectMapper.readTree(jsonContent);

            String summary = root.path("summary").asText("");
            String healthScore = root.path("systemHealthScore").asText("70");
            String healthLevel = root.path("systemHealthLevel").asText("一般");
            String insight = root.path("analysisInsight").asText("");
            String optimization = root.path("optimizationAdvice").asText("");

            List<RegistryAISuggestionDTO> suggestions = new ArrayList<>();
            JsonNode suggestionsNode = root.path("suggestions");
            if (suggestionsNode.isArray()) {
                TypeReference<List<RegistryAISuggestionDTO>> typeRef = new TypeReference<List<RegistryAISuggestionDTO>>() {};
                suggestions = objectMapper.convertValue(suggestionsNode, typeRef);
                suggestions.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
            }

            return RegistryAIAnalysisResultDTO.builder()
                    .summary(summary)
                    .systemHealthScore(healthScore)
                    .systemHealthLevel(healthLevel)
                    .totalIssues(analysisResult.getTotalIssues())
                    .highRiskCount(analysisResult.getSeverityStats().getOrDefault("high", 0))
                    .mediumRiskCount(analysisResult.getSeverityStats().getOrDefault("medium", 0))
                    .lowRiskCount(analysisResult.getSeverityStats().getOrDefault("low", 0))
                    .suggestions(suggestions)
                    .analysisInsight(insight)
                    .optimizationAdvice(optimization)
                    .build();

        } catch (Exception e) {
            log.error("解析AI注册表分析响应失败，原始内容: {}", aiContent, e);
            throw new RuntimeException("AI响应解析失败: " + e.getMessage());
        }
    }

    private String extractJson(String content) {
        if (content == null) return "{}";
        content = content.trim();

        int firstBrace = content.indexOf('{');
        int lastBrace = content.lastIndexOf('}');

        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return content.substring(firstBrace, lastBrace + 1);
        }

        return content;
    }

    private ChatResponse callChatApi(String systemPrompt, String userPrompt) {
        List<Message> messages = List.of(
            new SystemMessage(systemPrompt),
            new UserMessage(userPrompt)
        );
        Prompt prompt = new Prompt(messages);
        return chatClient.call(prompt);
    }

    private RegistryAIAnalysisResultDTO fallbackAIRegistryAnalysis(
            RegistryAnalysisResultDTO analysisResult, long startTime) {
        log.warn("降级模式：为注册表分析使用模拟AI分析结果");

        List<RegistryAISuggestionDTO> suggestions = new ArrayList<>();
        Map<String, List<RegistryIssueDTO>> issuesByCategory = analysisResult.getIssues().stream()
            .collect(Collectors.groupingBy(RegistryIssueDTO::getCategory));

        for (Map.Entry<String, List<RegistryIssueDTO>> entry : issuesByCategory.entrySet()) {
            String category = entry.getKey();
            List<RegistryIssueDTO> categoryIssues = entry.getValue();
            int count = categoryIssues.size();
            if (count == 0) continue;

            String categoryLabel = CATEGORY_LABEL_MAP.getOrDefault(category, category);
            String severity = CATEGORY_SEVERITY_MAP.getOrDefault(category, "low");
            int priority = calculateRegistryPriority(category, severity, count);

            suggestions.add(RegistryAISuggestionDTO.builder()
                    .category(category)
                    .categoryLabel(categoryLabel)
                    .title("清理" + categoryLabel + "问题")
                    .description("发现 " + count + " 项" + categoryLabel + "相关问题，" + getCategoryDescription(category))
                    .registryPaths(categoryIssues.stream()
                        .map(RegistryIssueDTO::getRegistryPath)
                        .limit(10)
                        .collect(Collectors.toList()))
                    .issueCount(count)
                    .riskLevel(severity)
                    .action(getActionForSeverity(severity))
                    .reason(getCategoryReason(category))
                    .priority(priority)
                    .impact(getCategoryImpact(category))
                    .precaution(getCategoryPrecaution(category, severity))
                    .build());
        }

        suggestions.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));

        int totalIssues = analysisResult.getTotalIssues();
        int high = analysisResult.getSeverityStats().getOrDefault("high", 0);
        int medium = analysisResult.getSeverityStats().getOrDefault("medium", 0);

        String healthScore = calculateHealthScore(totalIssues, high, medium);
        String healthLevel = getHealthLevel(healthScore);

        StringBuilder summary = new StringBuilder();
        summary.append("共发现 ").append(totalIssues).append(" 个注册表问题，");
        if (high > 0) summary.append("其中高危 ").append(high).append(" 个，");
        if (medium > 0) summary.append("中危 ").append(medium).append(" 个，");
        summary.append("系统健康评分：").append(healthScore).append("分（").append(healthLevel).append("）。");

        String insight = buildFallbackInsight(analysisResult, healthScore);
        String optimization = buildFallbackOptimization(analysisResult);

        return RegistryAIAnalysisResultDTO.builder()
                .summary(summary.toString())
                .systemHealthScore(healthScore)
                .systemHealthLevel(healthLevel)
                .totalIssues(totalIssues)
                .highRiskCount(high)
                .mediumRiskCount(medium)
                .lowRiskCount(analysisResult.getSeverityStats().getOrDefault("low", 0))
                .suggestions(suggestions)
                .analysisInsight(insight)
                .optimizationAdvice(optimization)
                .model("fallback")
                .analysisDurationMs(System.currentTimeMillis() - startTime)
                .build();
    }

    private int calculateRegistryPriority(String category, String severity, int count) {
        int severityScore = switch (severity) {
            case "high" -> 8;
            case "medium" -> 5;
            default -> 2;
        };

        int categoryBonus = switch (category) {
            case "service_entry", "com_component" -> 2;
            case "startup_entry", "dll_reference" -> 1;
            default -> 0;
        };

        int countBonus = count > 10 ? 2 : count > 5 ? 1 : 0;

        return Math.min(severityScore + categoryBonus + countBonus, 10);
    }

    private String getActionForSeverity(String severity) {
        return switch (severity) {
            case "high" -> "谨慎清理，建议先备份";
            case "medium" -> "建议清理，清理前请确认";
            default -> "可安全清理";
        };
    }

    private String getCategoryDescription(String category) {
        return switch (category) {
            case "file_association" -> "这些文件关联指向已不存在的程序，会导致打开文件时出现错误。";
            case "startup_entry" -> "这些启动项指向已卸载的程序，会拖慢系统启动速度。";
            case "uninstall_info" -> "这些是已卸载软件的残留信息，会使程序列表显得杂乱。";
            case "com_component" -> "这些COM组件指向不存在的DLL文件，可能导致程序运行异常。";
            case "service_entry" -> "这些系统服务指向不存在的程序，可能导致系统启动错误。";
            case "dll_reference" -> "这些DLL引用无效或计数为0，会占用系统资源。";
            case "menu_extension" -> "这些右键菜单扩展已失效，会减慢右键菜单响应速度。";
            case "recent_docs" -> "这些最近文档历史记录会暴露您的文件访问痕迹。";
            default -> "这些无效项会占用注册表空间，影响系统性能。";
        };
    }

    private String getCategoryReason(String category) {
        return switch (category) {
            case "file_association" -> "文件关联是系统中用于指定某类文件用什么程序打开的配置。当程序被卸载后，这些关联就变成了无效项，清理后可以修复打开方式菜单。";
            case "startup_entry" -> "启动项是系统启动时自动运行的程序配置。程序卸载后如果没有清理启动项，系统每次启动都会尝试运行不存在的程序，导致启动变慢。";
            case "uninstall_info" -> "卸载信息是在'程序和功能'中显示的已安装程序列表。软件卸载后残留的这些信息会让列表显示不准确。";
            case "com_component" -> "COM组件是Windows中软件组件交互的重要机制。无效的COM组件可能导致软件功能异常、报错甚至崩溃。";
            case "service_entry" -> "系统服务是在后台运行的重要进程。无效的服务项会导致系统启动时尝试加载不存在的服务，产生错误日志。";
            case "dll_reference" -> "共享DLL引用计数用于跟踪有多少程序在使用某个DLL。引用计数为0的DLL可以安全移除，释放磁盘空间。";
            case "menu_extension" -> "右键菜单扩展是当您在文件上点击右键时显示的额外菜单项。失效的扩展会拖慢菜单响应速度。";
            case "recent_docs" -> "最近文档记录保存了您最近打开过的文件列表，便于快速访问。但也会暴露您的使用痕迹，定期清理有助于保护隐私。";
            default -> "这些注册表项已经失去了关联的程序或文件，成为孤立项，清理后可以减小注册表体积，提升系统性能。";
        };
    }

    private String getCategoryImpact(String category) {
        return switch (category) {
            case "file_association" -> "修复文件打开方式，减少'无法找到程序'的错误";
            case "startup_entry" -> "加快系统启动速度，减少开机等待时间";
            case "uninstall_info" -> "使程序列表更整洁，便于管理已安装软件";
            case "com_component" -> "减少程序运行错误，提升系统稳定性";
            case "service_entry" -> "减少系统错误日志，加快系统服务启动";
            case "dll_reference" -> "释放磁盘空间，减少系统加载无效DLL的开销";
            case "menu_extension" -> "加快右键菜单响应速度，提升操作体验";
            case "recent_docs" -> "保护隐私，防止他人看到您最近访问的文件";
            default -> "减小注册表体积，提升系统整体性能";
        };
    }

    private String getCategoryPrecaution(String category, String severity) {
        if ("high".equals(severity)) {
            return "⚠️ 高风险操作！请务必备份注册表后再执行。如果清理后出现问题，可通过备份文件恢复。建议仅在您明确了解后果的情况下清理此项。";
        } else if ("medium".equals(severity)) {
            return "⚠️ 中等风险。建议先备份注册表再操作。如果您不确定某些项是否需要，建议保留。";
        }
        return switch (category) {
            case "recent_docs" -> "清理后最近打开的文件历史会被清除，不影响系统运行。";
            default -> "低风险操作，一般不会影响系统正常运行。但建议重要操作前仍先备份注册表。";
        };
    }

    private String calculateHealthScore(int totalIssues, int high, int medium) {
        int baseScore = 100;
        baseScore -= high * 5;
        baseScore -= medium * 2;
        baseScore -= (totalIssues - high - medium) / 5;
        baseScore = Math.max(0, Math.min(100, baseScore));
        return String.valueOf(baseScore);
    }

    private String getHealthLevel(String scoreStr) {
        int score = Integer.parseInt(scoreStr);
        if (score >= 90) return "优秀";
        if (score >= 75) return "良好";
        if (score >= 60) return "一般";
        if (score >= 40) return "较差";
        return "危险";
    }

    private String buildFallbackInsight(RegistryAnalysisResultDTO analysisResult, String healthScore) {
        int score = Integer.parseInt(healthScore);
        StringBuilder insight = new StringBuilder();

        if (score >= 90) {
            insight.append("您的注册表状态良好，");
        } else if (score >= 75) {
            insight.append("您的注册表状态整体良好，但存在一些可以优化的地方，");
        } else if (score >= 60) {
            insight.append("您的注册表存在较多问题，可能会影响系统性能，");
        } else if (score >= 40) {
            insight.append("您的注册表问题较多，系统性能可能已受到影响，");
        } else {
            insight.append("⚠️ 您的注册表存在严重问题，强烈建议立即清理，");
        }

        int high = analysisResult.getSeverityStats().getOrDefault("high", 0);
        if (high > 0) {
            insight.append("发现 ").append(high).append(" 项高危问题，主要集中在");
            List<String> highCategories = analysisResult.getIssues().stream()
                .filter(i -> "high".equals(i.getSeverity()))
                .map(RegistryIssueDTO::getCategoryLabel)
                .distinct()
                .collect(Collectors.toList());
            insight.append(String.join("、", highCategories)).append("。");
        }

        insight.append("建议按照优先级顺序进行清理，优先处理高危和中风险项。");

        return insight.toString();
    }

    private String buildFallbackOptimization(RegistryAnalysisResultDTO analysisResult) {
        StringBuilder advice = new StringBuilder();

        advice.append("1. 定期清理注册表：建议每月进行一次注册表扫描和清理，保持系统健康。\n");
        advice.append("2. 安装可靠的杀毒软件：恶意软件常常通过注册表实现自启动，保持杀毒软件更新可有效防护。\n");
        advice.append("3. 正确卸载软件：使用软件自带的卸载程序或系统'程序和功能'进行卸载，避免直接删除文件夹。\n");
        advice.append("4. 谨慎使用清理工具：在使用任何注册表清理工具前，务必备份注册表。\n");
        advice.append("5. 减少开机启动项：通过系统配置工具（msconfig）或任务管理器管理启动项，只保留必要的程序。\n");

        int startupCount = (int) analysisResult.getIssues().stream()
            .filter(i -> "startup_entry".equals(i.getCategory()))
            .count();
        if (startupCount > 3) {
            advice.append("\n💡 特别建议：您的启动项问题较多，建议定期检查开机启动程序，只保留杀毒软件、输入法等必要程序。\n");
        }

        int comCount = (int) analysisResult.getIssues().stream()
            .filter(i -> "com_component".equals(i.getCategory()))
            .count();
        if (comCount > 5) {
            advice.append("\n💡 特别建议：您的系统存在较多无效COM组件，这可能是由于软件卸载不彻底造成的。清理后建议重启电脑。\n");
        }

        return advice.toString();
    }
}
