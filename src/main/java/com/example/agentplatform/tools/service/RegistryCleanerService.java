package com.example.agentplatform.tools.service;

import com.example.agentplatform.tools.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistryCleanerService {

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
}
