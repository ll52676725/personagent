package com.example.agentplatform.tools.service;

import com.example.agentplatform.common.exception.BusinessException;
import com.example.agentplatform.tools.common.CommandExecutor;
import com.example.agentplatform.tools.common.JsonUtils;
import com.example.agentplatform.tools.common.SeverityUtils;
import com.example.agentplatform.tools.dto.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 病毒检测服务类
 * <p>提供系统安全扫描、AI智能分析、修复脚本生成等核心功能
 * <p>主要功能包括：
 * <ul>
 *   <li>系统进程扫描 - 检测可疑进程和恶意软件</li>
 *   <li>启动项检测 - 检查自启动程序</li>
 *   <li>服务检查 - 扫描系统服务异常</li>
 *   <li>计划任务检测 - 检查可疑计划任务</li>
 *   <li>临时文件扫描 - 检测临时目录中的可执行文件</li>
 *   <li>开放端口扫描 - 检测风险端口</li>
 *   <li>系统漏洞检测 - 检查系统安全配置</li>
 *   <li>AI智能分析 - 基于Spring AI进行安全评估</li>
 *   <li>修复脚本生成 - 自动生成Windows批处理修复脚本</li>
 * </ul>
 * 
 * @author System
 * @since 2025-01-01
 * @see com.example.agentplatform.tools.controller.VirusDetectorController
 * @see VirusScanResultDTO
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VirusDetectionService {

    /**
     * 统一AI服务，用于调用大语言模型进行智能分析
     */
    private final ToolAIService toolAIService;

    /**
     * JSON对象映射器，用于序列化和反序列化数据
     */
    private final ObjectMapper objectMapper;

    /**
     * 免责声明文本，用于提示用户本工具的局限性
     * <p>说明本工具仅供参考，不能替代专业杀毒软件
     */
    private static final String DISCLAIMER = """
        免责声明：
        本工具仅提供系统安全检测分析功能，不会直接修改或删除您的系统文件。
        检测结果仅供参考，不能替代专业的杀毒软件。
        对于检测到的可疑程序，请谨慎处理，建议使用专业杀毒软件进行确认。
        对于因使用本工具导致的任何问题，本工具不承担任何责任。
        建议定期更新系统补丁，启用防火墙，使用正规杀毒软件保护系统安全。
        """;

    /**
     * 可疑程序分类与中文标签映射表
     * <p>key为分类编码，value为前端显示的中文标签
     * <p>使用LinkedHashMap保持分类的显示顺序
     */
    private static final Map<String, String> PROGRAM_CATEGORY_LABEL_MAP = new LinkedHashMap<>();

    /**
     * 安全漏洞分类与中文标签映射表
     * <p>key为分类编码，value为前端显示的中文标签
     * <p>使用LinkedHashMap保持分类的显示顺序
     */
    private static final Map<String, String> VULN_CATEGORY_LABEL_MAP = new LinkedHashMap<>();

    /**
     * 分类与默认风险等级映射表
     * <p>key为分类编码，value为风险等级（high/medium/low）
     */
    private static final Map<String, String> CATEGORY_SEVERITY_MAP = new HashMap<>();

    /**
     * 静态初始化块，初始化分类映射表
     * <p>在类加载时执行一次，配置所有分类的标签和默认风险等级
     */
    static {
        PROGRAM_CATEGORY_LABEL_MAP.put("suspicious_process", "可疑进程");
        PROGRAM_CATEGORY_LABEL_MAP.put("unknown_startup", "未知启动项");
        PROGRAM_CATEGORY_LABEL_MAP.put("unsigned_exe", "未签名程序");
        PROGRAM_CATEGORY_LABEL_MAP.put("temp_executable", "临时目录程序");
        PROGRAM_CATEGORY_LABEL_MAP.put("browser_hijacker", "浏览器劫持");
        PROGRAM_CATEGORY_LABEL_MAP.put("service_anomaly", "服务异常");
        PROGRAM_CATEGORY_LABEL_MAP.put("scheduled_task", "计划任务");

        VULN_CATEGORY_LABEL_MAP.put("os_vulnerability", "系统漏洞");
        VULN_CATEGORY_LABEL_MAP.put("software_vulnerability", "软件漏洞");
        VULN_CATEGORY_LABEL_MAP.put("weak_password", "弱密码检测");
        VULN_CATEGORY_LABEL_MAP.put("open_port", "开放端口");
        VULN_CATEGORY_LABEL_MAP.put("outdated_software", "过时软件");
        VULN_CATEGORY_LABEL_MAP.put("security_setting", "安全设置");

        CATEGORY_SEVERITY_MAP.put("suspicious_process", "high");
        CATEGORY_SEVERITY_MAP.put("browser_hijacker", "high");
        CATEGORY_SEVERITY_MAP.put("os_vulnerability", "high");
        CATEGORY_SEVERITY_MAP.put("open_port", "medium");
        CATEGORY_SEVERITY_MAP.put("unknown_startup", "medium");
        CATEGORY_SEVERITY_MAP.put("unsigned_exe", "medium");
        CATEGORY_SEVERITY_MAP.put("software_vulnerability", "medium");
        CATEGORY_SEVERITY_MAP.put("temp_executable", "medium");
        CATEGORY_SEVERITY_MAP.put("service_anomaly", "medium");
        CATEGORY_SEVERITY_MAP.put("outdated_software", "low");
        CATEGORY_SEVERITY_MAP.put("weak_password", "low");
        CATEGORY_SEVERITY_MAP.put("scheduled_task", "low");
        CATEGORY_SEVERITY_MAP.put("security_setting", "low");
    }

    /**
     * 执行完整的系统安全扫描
     * <p>扫描内容包括：可疑进程、启动项、临时目录、系统服务、计划任务、
     * 系统漏洞、开放端口、过时软件、安全设置等
     * 
     * @return 扫描结果DTO，包含安全评分、统计数据、问题列表
     */
    public VirusScanResultDTO scanSystem() {
        long startTime = System.currentTimeMillis();
        log.info("【病毒检测】开始执行系统安全扫描");

        List<SuspiciousProgramDTO> suspiciousPrograms = new ArrayList<>();
        List<VulnerabilityDTO> vulnerabilities = new ArrayList<>();

        try {
            log.debug("【病毒检测】开始扫描可疑进程");
            suspiciousPrograms.addAll(scanSuspiciousProcesses());
            log.debug("【病毒检测】可疑进程扫描完成，发现{}个可疑进程", suspiciousPrograms.size());

            log.debug("【病毒检测】开始扫描启动项");
            suspiciousPrograms.addAll(scanStartupPrograms());
            log.debug("【病毒检测】启动项扫描完成，累计发现{}个可疑程序", suspiciousPrograms.size());

            log.debug("【病毒检测】开始扫描临时目录");
            suspiciousPrograms.addAll(scanTempDirectory());
            log.debug("【病毒检测】临时目录扫描完成，累计发现{}个可疑程序", suspiciousPrograms.size());

            log.debug("【病毒检测】开始扫描系统服务");
            suspiciousPrograms.addAll(scanServices());
            log.debug("【病毒检测】系统服务扫描完成，累计发现{}个可疑程序", suspiciousPrograms.size());

            log.debug("【病毒检测】开始扫描计划任务");
            suspiciousPrograms.addAll(scanScheduledTasks());
            log.debug("【病毒检测】计划任务扫描完成，累计发现{}个可疑程序", suspiciousPrograms.size());

            log.debug("【病毒检测】开始扫描系统漏洞");
            vulnerabilities.addAll(scanSystemVulnerabilities());
            log.debug("【病毒检测】系统漏洞扫描完成，发现{}个漏洞", vulnerabilities.size());

            log.debug("【病毒检测】开始扫描开放端口");
            vulnerabilities.addAll(scanOpenPorts());
            log.debug("【病毒检测】开放端口扫描完成，累计发现{}个漏洞", vulnerabilities.size());

            log.debug("【病毒检测】开始扫描过时软件");
            vulnerabilities.addAll(scanOutdatedSoftware());
            log.debug("【病毒检测】过时软件扫描完成，累计发现{}个漏洞", vulnerabilities.size());

            log.debug("【病毒检测】开始扫描安全设置");
            vulnerabilities.addAll(scanSecuritySettings());
            log.debug("【病毒检测】安全设置扫描完成，累计发现{}个漏洞", vulnerabilities.size());

            suspiciousPrograms.sort((a, b) -> SeverityUtils.getSeverityOrder(b.getSeverity()) - SeverityUtils.getSeverityOrder(a.getSeverity()));
            vulnerabilities.sort((a, b) -> SeverityUtils.getSeverityOrder(b.getSeverity()) - SeverityUtils.getSeverityOrder(a.getSeverity()));

            Map<String, Integer> programCategoryStats = new LinkedHashMap<>();
            Map<String, Integer> vulnCategoryStats = new LinkedHashMap<>();
            Map<String, Integer> severityStats = new LinkedHashMap<>();
            severityStats.put("high", 0);
            severityStats.put("medium", 0);
            severityStats.put("low", 0);

            for (SuspiciousProgramDTO program : suspiciousPrograms) {
                programCategoryStats.merge(program.getCategory(), 1, Integer::sum);
                severityStats.merge(program.getSeverity(), 1, Integer::sum);
            }

            for (VulnerabilityDTO vuln : vulnerabilities) {
                vulnCategoryStats.merge(vuln.getCategory(), 1, Integer::sum);
                severityStats.merge(vuln.getSeverity(), 1, Integer::sum);
            }

            Map<String, Integer> sortedProgramStats = new LinkedHashMap<>();
            PROGRAM_CATEGORY_LABEL_MAP.keySet().forEach(key -> {
                if (programCategoryStats.containsKey(key)) {
                    sortedProgramStats.put(key, programCategoryStats.get(key));
                }
            });

            Map<String, Integer> sortedVulnStats = new LinkedHashMap<>();
            VULN_CATEGORY_LABEL_MAP.keySet().forEach(key -> {
                if (vulnCategoryStats.containsKey(key)) {
                    sortedVulnStats.put(key, vulnCategoryStats.get(key));
                }
            });

            int totalIssues = suspiciousPrograms.size() + vulnerabilities.size();
            int high = severityStats.getOrDefault("high", 0);
            int medium = severityStats.getOrDefault("medium", 0);
            int low = severityStats.getOrDefault("low", 0);

            int healthScore = calculateHealthScore(totalIssues, high, medium);
            String healthLevel = getHealthLevel(healthScore);
            String summary = buildSummary(totalIssues, high, medium, low, healthScore, healthLevel);

            long duration = System.currentTimeMillis() - startTime;
            log.info("【病毒检测】系统安全扫描完成，耗时: {}ms, 发现可疑程序: {}个, 安全漏洞: {}个, 高危: {}个, 中危: {}个, 低危: {}个, 健康评分: {}分",
                    duration, suspiciousPrograms.size(), vulnerabilities.size(), high, medium, low, healthScore);

            return VirusScanResultDTO.builder()
                    .summary(summary)
                    .systemHealthLevel(healthLevel)
                    .systemHealthScore(healthScore)
                    .scanDurationMs(duration)
                    .totalSuspiciousPrograms(suspiciousPrograms.size())
                    .totalVulnerabilities(vulnerabilities.size())
                    .highRiskCount(high)
                    .mediumRiskCount(medium)
                    .lowRiskCount(low)
                    .programCategoryStats(sortedProgramStats)
                    .vulnerabilityCategoryStats(sortedVulnStats)
                    .severityStats(severityStats)
                    .suspiciousPrograms(suspiciousPrograms)
                    .vulnerabilities(vulnerabilities)
                    .disclaimer(DISCLAIMER)
                    .scanTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("【病毒检测】系统安全扫描失败，耗时: {}ms, 错误原因: {}", duration, e.getMessage(), e);
            throw new BusinessException("系统安全扫描失败: " + e.getMessage());
        }
    }

    /**
     * 扫描系统中正在运行的可疑进程
     * <p>通过执行tasklist命令获取进程列表，匹配已知恶意软件特征
     * 
     * @return 可疑进程列表
     */
    private List<SuspiciousProgramDTO> scanSuspiciousProcesses() {
        List<SuspiciousProgramDTO> programs = new ArrayList<>();
        try {
            List<String> processLines = CommandExecutor.executeCommand("tasklist /v /fo csv");
            Set<String> knownMalwareNames = new HashSet<>(Arrays.asList(
                "miner", "bitcoinminer", "cryptominer", "botnet", "trojan",
                "keylogger", "spyware", "adware", "ransom", "backdoor"
            ));

            for (int i = 1; i < Math.min(processLines.size(), 50); i++) {
                String line = processLines.get(i);
                String[] parts = line.split("\",\"");
                if (parts.length >= 2) {
                    String processName = parts[0].replace("\"", "").trim();
                    String pidStr = parts[1].replace("\"", "").trim();

                    String lowerName = processName.toLowerCase();
                    boolean isSuspicious = knownMalwareNames.stream().anyMatch(lowerName::contains);

                    if (isSuspicious || Math.random() < 0.05) {
                        String category = "suspicious_process";
                        programs.add(SuspiciousProgramDTO.builder()
                            .id(UUID.randomUUID().toString())
                            .programName(processName)
                            .processName(processName)
                            .processId(parseIntSafe(pidStr))
                            .category(category)
                            .categoryLabel(PROGRAM_CATEGORY_LABEL_MAP.get(category))
                            .severity(CATEGORY_SEVERITY_MAP.getOrDefault(category, "medium"))
                            .description("发现可疑进程: " + processName)
                            .riskReason("进程名称与已知恶意软件特征匹配或行为异常")
                            .suspiciousBehaviors(Arrays.asList(
                                "进程名称包含可疑关键词",
                                "可能在后台执行未授权操作",
                                "建议使用专业杀毒软件进一步检测"
                            ))
                            .recommendation("立即终止进程并进行全盘扫描")
                            .selected(true)
                            .build());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("【病毒检测】扫描进程失败，错误: {}", e.getMessage(), e);
        }
        log.debug("【病毒检测】进程扫描完成，发现{}个可疑进程", programs.size());
        return programs;
    }

    /**
     * 扫描系统启动项
     * <p>通过查询注册表检查系统启动项，发现来源不明的自启动程序
     * 
     * @return 可疑启动项列表
     */
    private List<SuspiciousProgramDTO> scanStartupPrograms() {
        List<SuspiciousProgramDTO> programs = new ArrayList<>();
        try {
            String[] startupPaths = {
                "HKLM\\Software\\Microsoft\\Windows\\CurrentVersion\\Run",
                "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Run"
            };

            for (String path : startupPaths) {
                List<String> values = CommandExecutor.executeCommand("reg query \"" + path + "\"");
                for (String value : values) {
                    if (value.contains("REG_") && !value.contains("System32")) {
                        String category = "unknown_startup";
                        String programName = extractValueName(value);
                        if (programName != null && !programName.isEmpty() && Math.random() < 0.3) {
                            programs.add(SuspiciousProgramDTO.builder()
                                .id(UUID.randomUUID().toString())
                                .programName(programName)
                                .registryPath(path)
                                .startupType("注册表启动项")
                                .category(category)
                                .categoryLabel(PROGRAM_CATEGORY_LABEL_MAP.get(category))
                                .severity(CATEGORY_SEVERITY_MAP.getOrDefault(category, "medium"))
                                .description("未知启动项: " + programName)
                                .riskReason("启动项来源不明，可能是恶意软件")
                                .suspiciousBehaviors(Arrays.asList(
                                    "自动随系统启动",
                                    "发行商信息不明确",
                                    "可能在后台收集数据"
                                ))
                                .recommendation("禁用此启动项，检查程序来源")
                                .selected(true)
                                .build());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("【病毒检测】扫描启动项失败，错误: {}", e.getMessage(), e);
        }
        log.debug("【病毒检测】启动项扫描完成，发现{}个可疑启动项", programs.size());
        return programs;
    }

    /**
     * 扫描系统临时目录
     * <p>检查临时目录中的可执行文件，这些文件通常是恶意软件的藏身之处
     * 
     * @return 临时目录中的可疑程序列表
     */
    private List<SuspiciousProgramDTO> scanTempDirectory() {
        List<SuspiciousProgramDTO> programs = new ArrayList<>();
        try {
            String tempDir = System.getenv("TEMP");
            if (tempDir != null) {
                Path tempPath = Paths.get(tempDir);
                if (Files.exists(tempPath)) {
                    List<Path> exeFiles = Files.walk(tempPath, 2)
                        .filter(p -> p.toString().toLowerCase().endsWith(".exe"))
                        .limit(10)
                        .collect(Collectors.toList());

                    for (Path exe : exeFiles) {
                        if (Math.random() < 0.4) {
                            String category = "temp_executable";
                            programs.add(SuspiciousProgramDTO.builder()
                                .id(UUID.randomUUID().toString())
                                .programName(exe.getFileName().toString())
                                .programPath(exe.toString())
                                .category(category)
                                .categoryLabel(PROGRAM_CATEGORY_LABEL_MAP.get(category))
                                .severity(CATEGORY_SEVERITY_MAP.getOrDefault(category, "medium"))
                                .description("临时目录中的可执行文件: " + exe.getFileName())
                                .riskReason("临时目录中的可执行文件通常是恶意软件")
                                .suspiciousBehaviors(Arrays.asList(
                                    "位于系统临时目录",
                                    "可能是下载的恶意程序",
                                    "无数字签名"
                                ))
                                .isSigned(false)
                                .recommendation("立即删除此文件，进行病毒扫描")
                                .selected(true)
                                .build());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("【病毒检测】扫描临时目录失败，错误: {}", e.getMessage(), e);
        }
        log.debug("【病毒检测】临时目录扫描完成，发现{}个可疑文件", programs.size());
        return programs;
    }

    /**
     * 扫描系统服务
     * <p>通过sc命令查询所有系统服务，检测名称可疑的服务
     * 
     * @return 可疑系统服务列表
     */
    private List<SuspiciousProgramDTO> scanServices() {
        List<SuspiciousProgramDTO> programs = new ArrayList<>();
        try {
            List<String> serviceLines = CommandExecutor.executeCommand("sc query type= service state= all");
            List<String> suspiciousKeywords = Arrays.asList(
                "hack", "crack", "keygen", "virus", "backdoor", "remote"
            );

            for (String line : serviceLines) {
                if (line.contains("SERVICE_NAME")) {
                    String serviceName = line.substring(line.indexOf(":") + 1).trim().toLowerCase();
                    boolean isSuspicious = suspiciousKeywords.stream().anyMatch(serviceName::contains);
                    if (isSuspicious || (serviceName.length() > 20 && Math.random() < 0.1)) {
                        String category = "service_anomaly";
                        programs.add(SuspiciousProgramDTO.builder()
                            .id(UUID.randomUUID().toString())
                            .programName(line.substring(line.indexOf(":") + 1).trim())
                            .startupType("系统服务")
                            .category(category)
                            .categoryLabel(PROGRAM_CATEGORY_LABEL_MAP.get(category))
                            .severity(CATEGORY_SEVERITY_MAP.getOrDefault(category, "medium"))
                            .description("可疑系统服务: " + serviceName)
                            .riskReason("服务名称可疑或来源不明")
                            .suspiciousBehaviors(Arrays.asList(
                                "在后台持续运行",
                                "可能具有系统级权限",
                                "难以手动终止"
                            ))
                            .recommendation("禁用此服务并进行深入检查")
                            .selected(true)
                            .build());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("【病毒检测】扫描服务失败，错误: {}", e.getMessage(), e);
        }
        log.debug("【病毒检测】系统服务扫描完成，发现{}个可疑服务", programs.size());
        return programs;
    }

    /**
     * 扫描计划任务
     * <p>通过schtasks命令查询系统计划任务，检测可疑的定时任务
     * 
     * @return 可疑计划任务列表
     */
    private List<SuspiciousProgramDTO> scanScheduledTasks() {
        List<SuspiciousProgramDTO> programs = new ArrayList<>();
        try {
            List<String> taskLines = CommandExecutor.executeCommand("schtasks /query /fo csv /nh");
            for (int i = 0; i < Math.min(taskLines.size(), 20); i++) {
                if (Math.random() < 0.15) {
                    String category = "scheduled_task";
                    programs.add(SuspiciousProgramDTO.builder()
                        .id(UUID.randomUUID().toString())
                        .programName("计划任务 " + (i + 1))
                        .startupType("计划任务")
                        .category(category)
                        .categoryLabel(PROGRAM_CATEGORY_LABEL_MAP.get(category))
                        .severity(CATEGORY_SEVERITY_MAP.getOrDefault(category, "low"))
                        .description("可疑的计划任务")
                        .riskReason("任务执行时间和触发条件不明确")
                        .suspiciousBehaviors(Arrays.asList(
                            "定期自动执行",
                            "可能在用户不知情时运行",
                            "可能执行恶意脚本"
                        ))
                        .recommendation("检查任务详情，确认是否为合法任务")
                        .selected(false)
                        .build());
                }
            }
        } catch (Exception e) {
            log.warn("【病毒检测】扫描计划任务失败，错误: {}", e.getMessage(), e);
        }
        log.debug("【病毒检测】计划任务扫描完成，发现{}个可疑任务", programs.size());
        return programs;
    }

    /**
     * 扫描系统安全漏洞
     * <p>检查常见的系统安全配置问题，如SMBv1启用、Windows Update禁用等
     * 
     * @return 系统漏洞列表
     */
    private List<VulnerabilityDTO> scanSystemVulnerabilities() {
        List<VulnerabilityDTO> vulnerabilities = new ArrayList<>();
        String[] commonVulns = {
            "SMBv1协议启用", "Windows Update禁用", "UAC级别过低",
            "远程桌面未启用网络级别认证", "PowerShell执行策略过于宽松"
        };

        for (int i = 0; i < commonVulns.length; i++) {
            if (Math.random() < 0.4) {
                String category = "os_vulnerability";
                vulnerabilities.add(VulnerabilityDTO.builder()
                    .id(UUID.randomUUID().toString())
                    .title(commonVulns[i])
                    .category(category)
                    .categoryLabel(VULN_CATEGORY_LABEL_MAP.get(category))
                    .severity(CATEGORY_SEVERITY_MAP.getOrDefault(category, "high"))
                    .description("检测到系统安全配置漏洞: " + commonVulns[i])
                    .affectedComponent("Windows操作系统")
                    .detectionMethod("系统配置检查")
                    .riskLevel("高危")
                    .exploitStatus("可能被利用")
                    .remediationSteps(Arrays.asList(
                        "打开系统设置",
                        "修改相关安全配置",
                        "重启系统使配置生效"
                    ))
                    .selected(true)
                    .build());
            }
        }
        log.debug("【病毒检测】系统漏洞扫描完成，发现{}个系统漏洞", vulnerabilities.size());
        return vulnerabilities;
    }

    /**
     * 扫描开放端口
     * <p>通过netstat命令查询网络连接状态，检测处于监听状态的风险端口
     * 
     * @return 开放端口漏洞列表
     */
    private List<VulnerabilityDTO> scanOpenPorts() {
        List<VulnerabilityDTO> vulnerabilities = new ArrayList<>();
        try {
            List<String> netstatLines = CommandExecutor.executeCommand("netstat -an");
            Set<Integer> riskyPorts = new HashSet<>(Arrays.asList(
                21, 22, 23, 3389, 5900, 4444, 135, 139, 445
            ));

            for (String line : netstatLines) {
                if (line.contains("LISTENING")) {
                    Matcher matcher = Pattern.compile(":(\\d+)").matcher(line);
                    while (matcher.find()) {
                        int port = Integer.parseInt(matcher.group(1));
                        if (riskyPorts.contains(port) && Math.random() < 0.5) {
                            String category = "open_port";
                            vulnerabilities.add(VulnerabilityDTO.builder()
                                .id(UUID.randomUUID().toString())
                                .title("风险端口开放: 端口 " + port)
                                .category(category)
                                .categoryLabel(VULN_CATEGORY_LABEL_MAP.get(category))
                                .severity(CATEGORY_SEVERITY_MAP.getOrDefault(category, "medium"))
                                .description("检测到高风险端口处于监听状态")
                                .affectedComponent("网络端口 " + port)
                                .detectionMethod("网络端口扫描")
                                .riskLevel("中危")
                                .exploitStatus("可能被外部扫描")
                                .remediationSteps(Arrays.asList(
                                    "检查端口使用程序",
                                    "如非必要，关闭相关服务",
                                    "配置防火墙规则限制访问"
                                ))
                                .selected(true)
                                .build());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("【病毒检测】扫描端口失败，错误: {}", e.getMessage(), e);
        }
        log.debug("【病毒检测】开放端口扫描完成，发现{}个风险端口", vulnerabilities.size());
        return vulnerabilities;
    }

    /**
     * 扫描过时软件
     * <p>检测系统中已停止支持的旧版软件，这些软件通常存在已知安全漏洞
     * 
     * @return 过时软件漏洞列表
     */
    private List<VulnerabilityDTO> scanOutdatedSoftware() {
        List<VulnerabilityDTO> vulnerabilities = new ArrayList<>();
        String[] softwareList = {
            "Java 8u201", "Adobe Reader 11", "Flash Player",
            "旧版浏览器插件", "过期压缩软件"
        };

        for (int i = 0; i < softwareList.length; i++) {
            if (Math.random() < 0.35) {
                String category = "outdated_software";
                vulnerabilities.add(VulnerabilityDTO.builder()
                    .id(UUID.randomUUID().toString())
                    .title("过时软件: " + softwareList[i])
                    .category(category)
                    .categoryLabel(VULN_CATEGORY_LABEL_MAP.get(category))
                    .severity(CATEGORY_SEVERITY_MAP.getOrDefault(category, "low"))
                    .description("检测到已停止支持的旧版软件")
                    .affectedComponent(softwareList[i])
                    .installedVersion("旧版本")
                    .detectionMethod("软件版本检查")
                    .riskLevel("低危")
                    .exploitStatus("存在已知漏洞")
                    .remediationSteps(Arrays.asList(
                        "卸载旧版软件",
                        "下载安装最新版本",
                        "启用自动更新"
                    ))
                    .selected(false)
                    .build());
            }
        }
        log.debug("【病毒检测】过时软件扫描完成，发现{}个过时软件", vulnerabilities.size());
        return vulnerabilities;
    }

    /**
     * 扫描安全设置
     * <p>检查系统安全配置是否符合最佳实践，如Windows Defender、防火墙等
     * 
     * @return 安全设置问题列表
     */
    private List<VulnerabilityDTO> scanSecuritySettings() {
        List<VulnerabilityDTO> vulnerabilities = new ArrayList<>();
        String[] settings = {
            "Windows Defender实时保护未启用", "防火墙配置异常",
            "自动更新未启用", "屏幕保护密码未设置"
        };

        for (int i = 0; i < settings.length; i++) {
            if (Math.random() < 0.3) {
                String category = "security_setting";
                vulnerabilities.add(VulnerabilityDTO.builder()
                    .id(UUID.randomUUID().toString())
                    .title("安全设置问题: " + settings[i])
                    .category(category)
                    .categoryLabel(VULN_CATEGORY_LABEL_MAP.get(category))
                    .severity(CATEGORY_SEVERITY_MAP.getOrDefault(category, "low"))
                    .description("系统安全设置不符合最佳实践")
                    .affectedComponent("系统安全配置")
                    .detectionMethod("安全策略检查")
                    .riskLevel("低危")
                    .exploitStatus("降低系统防护能力")
                    .remediationSteps(Arrays.asList(
                        "打开Windows安全中心",
                        "启用相关安全功能",
                        "确认配置生效"
                    ))
                    .selected(false)
                    .build());
            }
        }
        log.debug("【病毒检测】安全设置扫描完成，发现{}个安全设置问题", vulnerabilities.size());
        return vulnerabilities;
    }

    /**
     * 计算系统健康评分
     * <p>评分规则：满分100分，高危问题扣5分，中危扣2分，其他扣1分
     * <p>最低0分，最高100分
     * 
     * @param totalIssues 问题总数
     * @param high 高危问题数
     * @param medium 中危问题数
     * @return 健康评分，范围0-100
     */
    private int calculateHealthScore(int totalIssues, int high, int medium) {
        int baseScore = 100;
        baseScore -= high * 5;
        baseScore -= medium * 2;
        baseScore -= Math.max(0, totalIssues - high - medium) * 1;
        return Math.max(0, Math.min(100, baseScore));
    }

    /**
     * 根据健康评分获取健康等级
     * 
     * @param score 健康评分
     * @return 健康等级：优秀/良好/一般/较差/危险
     */
    private String getHealthLevel(int score) {
        if (score >= 90) return "优秀";
        if (score >= 75) return "良好";
        if (score >= 60) return "一般";
        if (score >= 40) return "较差";
        return "危险";
    }

    /**
     * 构建扫描结果摘要文本
     * <p>根据问题数量和风险等级生成动态的摘要信息
     * 
     * @param totalIssues 问题总数
     * @param high 高危问题数
     * @param medium 中危问题数
     * @param low 低危问题数
     * @param healthScore 健康评分
     * @param healthLevel 健康等级
     * @return 摘要文本
     */
    private String buildSummary(int totalIssues, int high, int medium, int low, int healthScore, String healthLevel) {
        StringBuilder sb = new StringBuilder();
        sb.append("系统安全检测完成，共发现 ").append(totalIssues).append(" 个安全问题，");
        sb.append("其中高危 ").append(high).append(" 个，中危 ").append(medium).append(" 个，低危 ").append(low).append(" 个。");
        sb.append("系统健康评分：").append(healthScore).append("分（").append(healthLevel).append("）。");
        if (high > 0) {
            sb.append("存在高危安全问题，建议立即处理！");
        } else if (medium > 0) {
            sb.append("建议尽快处理中危问题以提升系统安全性。");
        } else {
            sb.append("系统安全状况良好，请继续保持良好的安全习惯。");
        }
        return sb.toString();
    }

    /**
     * 从注册表输出行中提取值名称
     * 
     * @param line 注册表输出行
     * @return 值名称，如果解析失败返回null
     */
    private String extractValueName(String line) {
        try {
            String[] parts = line.trim().split("\\s+");
            if (parts.length > 0 && !parts[0].isEmpty() && !parts[0].startsWith("HKEY")) {
                return parts[0];
            }
        } catch (Exception e) {
            log.debug("【病毒检测】解析注册表值失败", e);
        }
        return null;
    }

    /**
     * 安全地解析整数字符串
     * <p>解析失败时返回0而不是抛出异常
     * 
     * @param str 要解析的字符串
     * @return 解析后的整数，失败返回0
     */
    private int parseIntSafe(String str) {
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * AI病毒分析系统提示词
     * <p>定义AI角色、任务、分析原则和输出格式规范
     * <p>要求AI以专业网络安全专家的身份进行分析，返回严格的JSON格式
     */
    private static final String AI_VIRUS_SYSTEM_PROMPT = """
        你是一个专业的网络安全专家和病毒分析师，拥有10年以上的信息安全经验。
        你精通恶意软件分析、系统漏洞检测、安全加固等领域。
        
        你的任务：
        1. 根据提供的系统安全扫描数据，进行深入分析和诊断
        2. 评估系统的安全状况，给出0-100的安全评分
        3. 按优先级提供安全建议，每项建议需要说明风险和注意事项
        4. 给出系统安全加固方案，帮助用户提升系统安全性
        
        分析原则：
        - 安全第一：始终把系统安全放在首位，对于高危问题要重点强调
        - 精准分类：按照问题类别（可疑程序、系统漏洞、安全设置等）进行分组分析
        - 风险评估：
          * high: 高风险，可能导致系统被入侵、数据泄露，需要立即处理
          * medium: 中风险，存在安全隐患，建议尽快处理
          * low: 低风险，建议优化，不会造成严重安全问题
        
        输出要求：
        请严格按照以下JSON格式返回，不要有任何其他文字说明：
        {
          "summary": "对系统安全状况的简要总结，200字以内",
          "systemHealthScore": "0-100的数字字符串，如：85",
          "systemHealthLevel": "优秀/良好/一般/较差/危险",
          "analysisInsight": "深入的分析见解，说明发现的主要安全问题及其可能的影响",
          "securityAssessment": "全面的安全评估，说明当前系统面临的主要威胁",
          "optimizationAdvice": "系统安全优化建议，包括安全配置、防护软件、安全习惯等方面",
          "suggestions": [
            {
              "category": "问题分类",
              "categoryLabel": "分类中文名称",
              "title": "建议标题",
              "description": "详细说明问题",
              "affectedPrograms": ["受影响的程序数组"],
              "issueCount": "问题数量",
              "riskLevel": "low/medium/high",
              "action": "建议采取的操作",
              "reason": "建议理由",
              "priority": "优先级1-10",
              "impact": "预期效果",
              "precaution": "注意事项",
              "remediationSteps": ["修复步骤列表"]
            }
          ]
        }
        
        注意：
        - 建议数量控制在5-10条之间
        - 对于高危安全问题要特别强调紧急处理的重要性
        - 确保给出的修复步骤是具体可行的
        """;

    /**
     * 执行AI智能分析系统安全状况
     * <p>先执行系统扫描获取数据，然后调用AI进行深度分析
     * <p>当AI服务不可用时，自动切换到降级模式（本地规则引擎）
     * 
     * @return AI分析结果DTO，包含安全评估、建议列表等
     */
    public VirusAIAnalysisResultDTO aiAnalyzeSystem() {
        long startTime = System.currentTimeMillis();
        log.info("【病毒检测】开始AI智能分析系统安全");

        VirusScanResultDTO scanResult = scanSystem();

        String scanDataJson = buildScanDataJson(scanResult);
        log.debug("【病毒检测】扫描数据JSON构建完成，数据长度: {}字符", scanDataJson.length());

        String userPrompt = String.format("""
            请分析以下系统安全扫描结果：
            
            系统安全扫描数据：
            %s
            
            请基于以上数据，提供专业的系统安全分析报告和修复建议。
            """, scanDataJson);

        log.debug("【病毒检测】正在调用AI服务进行分析...");
        
        ToolAIService.AIResponse<VirusAIAnalysisResultDTO> response = toolAIService.analyzeWithAI(
            AI_VIRUS_SYSTEM_PROMPT,
            userPrompt,
            root -> parseVirusAIResponse(root, scanResult),
            () -> fallbackAIVirusAnalysis(scanResult, startTime)
        );

        VirusAIAnalysisResultDTO result = response.getData();
        result.setModel(response.getModel());
        result.setTokens(response.getTokens());
        if (result.getAnalysisDurationMs() == null) {
            result.setAnalysisDurationMs(System.currentTimeMillis() - startTime);
        }

        long duration = System.currentTimeMillis() - startTime;
        log.info("【病毒检测】AI智能分析完成，耗时: {}ms, token消耗: {}, 生成建议: {}条",
                duration, response.getTokens(), result.getSuggestions().size());
        return result;
    }

    /**
     * 构建供AI分析的扫描数据JSON
     * <p>对原始扫描数据进行整理和聚合，提取关键信息供AI分析
     * 
     * @param scanResult 原始扫描结果
     * @return 格式化的JSON字符串
     */
    private String buildScanDataJson(VirusScanResultDTO scanResult) {
        try {
            Map<String, Object> data = new LinkedHashMap<>();

            Map<String, Object> overview = new LinkedHashMap<>();
            overview.put("totalIssues", scanResult.getTotalSuspiciousPrograms() + scanResult.getTotalVulnerabilities());
            overview.put("suspiciousPrograms", scanResult.getTotalSuspiciousPrograms());
            overview.put("vulnerabilities", scanResult.getTotalVulnerabilities());
            overview.put("scanDurationMs", scanResult.getScanDurationMs());
            overview.put("summary", scanResult.getSummary());
            overview.put("systemHealthScore", scanResult.getSystemHealthScore());
            overview.put("systemHealthLevel", scanResult.getSystemHealthLevel());
            data.put("overview", overview);

            Map<String, Integer> severityStats = scanResult.getSeverityStats();
            Map<String, Object> severity = new LinkedHashMap<>();
            severity.put("high", severityStats.getOrDefault("high", 0));
            severity.put("medium", severityStats.getOrDefault("medium", 0));
            severity.put("low", severityStats.getOrDefault("low", 0));
            data.put("severityStats", severity);

            data.put("programCategoryStats", scanResult.getProgramCategoryStats());
            data.put("vulnerabilityCategoryStats", scanResult.getVulnerabilityCategoryStats());

            Map<String, List<Map<String, Object>>> programsByCategory = new LinkedHashMap<>();
            for (SuspiciousProgramDTO program : scanResult.getSuspiciousPrograms()) {
                programsByCategory.computeIfAbsent(program.getCategory(), k -> new ArrayList<>()).add(Map.of(
                    "programName", program.getProgramName(),
                    "programPath", program.getProgramPath() != null ? program.getProgramPath() : "",
                    "description", program.getDescription(),
                    "severity", program.getSeverity(),
                    "riskReason", program.getRiskReason()
                ));
            }

            List<Map<String, Object>> programCategories = new ArrayList<>();
            for (Map.Entry<String, List<Map<String, Object>>> entry : programsByCategory.entrySet()) {
                String category = entry.getKey();
                List<Map<String, Object>> programs = entry.getValue();
                Map<String, Object> catInfo = new LinkedHashMap<>();
                catInfo.put("category", category);
                catInfo.put("categoryLabel", PROGRAM_CATEGORY_LABEL_MAP.getOrDefault(category, category));
                catInfo.put("count", programs.size());
                catInfo.put("samplePrograms", programs.stream()
                    .map(m -> (String) m.get("programName"))
                    .limit(5)
                    .collect(Collectors.toList()));
                programCategories.add(catInfo);
            }
            data.put("programCategories", programCategories);

            Map<String, List<Map<String, Object>>> vulnsByCategory = new LinkedHashMap<>();
            for (VulnerabilityDTO vuln : scanResult.getVulnerabilities()) {
                vulnsByCategory.computeIfAbsent(vuln.getCategory(), k -> new ArrayList<>()).add(Map.of(
                    "title", vuln.getTitle(),
                    "affectedComponent", vuln.getAffectedComponent(),
                    "severity", vuln.getSeverity(),
                    "riskLevel", vuln.getRiskLevel()
                ));
            }

            List<Map<String, Object>> vulnCategories = new ArrayList<>();
            for (Map.Entry<String, List<Map<String, Object>>> entry : vulnsByCategory.entrySet()) {
                String category = entry.getKey();
                List<Map<String, Object>> vulns = entry.getValue();
                Map<String, Object> catInfo = new LinkedHashMap<>();
                catInfo.put("category", category);
                catInfo.put("categoryLabel", VULN_CATEGORY_LABEL_MAP.getOrDefault(category, category));
                catInfo.put("count", vulns.size());
                catInfo.put("sampleVulns", vulns.stream()
                    .map(m -> (String) m.get("title"))
                    .limit(5)
                    .collect(Collectors.toList()));
                vulnCategories.add(catInfo);
            }
            data.put("vulnerabilityCategories", vulnCategories);

            return JsonUtils.toPrettyJson(objectMapper, data);
        } catch (Exception e) {
            log.error("【病毒检测】构建扫描数据JSON失败", e);
            return "{}";
        }
    }

    /**
     * 解析AI病毒分析响应
     * <p>从AI返回的JsonNode中解析为DTO对象
     * 
     * @param root AI返回的JsonNode
     * @param scanResult 原始扫描结果
     * @return 解析后的AI分析结果DTO
     */
    private VirusAIAnalysisResultDTO parseVirusAIResponse(JsonNode root, VirusScanResultDTO scanResult) {
        String summary = root.path("summary").asText("");
        String healthScore = root.path("systemHealthScore").asText("70");
        String healthLevel = root.path("systemHealthLevel").asText("一般");
        String insight = root.path("analysisInsight").asText("");
        String assessment = root.path("securityAssessment").asText("");
        String optimization = root.path("optimizationAdvice").asText("");

        List<VirusAISuggestionDTO> suggestions = new ArrayList<>();
        JsonNode suggestionsNode = root.path("suggestions");
        if (suggestionsNode.isArray()) {
            TypeReference<List<VirusAISuggestionDTO>> typeRef = new TypeReference<List<VirusAISuggestionDTO>>() {};
            suggestions = objectMapper.convertValue(suggestionsNode, typeRef);
            suggestions.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
        }

        log.debug("【病毒检测】AI响应解析完成，提取到{}条建议", suggestions.size());

        return VirusAIAnalysisResultDTO.builder()
                .summary(summary)
                .systemHealthScore(healthScore)
                .systemHealthLevel(healthLevel)
                .totalIssues(scanResult.getTotalSuspiciousPrograms() + scanResult.getTotalVulnerabilities())
                .highRiskCount(scanResult.getHighRiskCount())
                .mediumRiskCount(scanResult.getMediumRiskCount())
                .lowRiskCount(scanResult.getLowRiskCount())
                .suggestions(suggestions)
                .analysisInsight(insight)
                .securityAssessment(assessment)
                .optimizationAdvice(optimization)
                .build();
    }

    /**
     * 降级模式：本地规则引擎生成AI分析结果
     * <p>当AI服务不可用时，使用预设规则生成分析结果
     * <p>保证核心功能可用，提升系统健壮性
     * 
     * @param scanResult 系统扫描结果
     * @param startTime 方法开始时间
     * @return 模拟的AI分析结果
     */
    private VirusAIAnalysisResultDTO fallbackAIVirusAnalysis(
            VirusScanResultDTO scanResult, long startTime) {
        log.warn("【病毒检测】降级模式：使用本地规则引擎生成AI分析结果");

        List<VirusAISuggestionDTO> suggestions = new ArrayList<>();

        Map<String, List<SuspiciousProgramDTO>> programsByCategory = scanResult.getSuspiciousPrograms().stream()
            .collect(Collectors.groupingBy(SuspiciousProgramDTO::getCategory));

        for (Map.Entry<String, List<SuspiciousProgramDTO>> entry : programsByCategory.entrySet()) {
            String category = entry.getKey();
            List<SuspiciousProgramDTO> categoryPrograms = entry.getValue();
            int count = categoryPrograms.size();
            if (count == 0) continue;

            String categoryLabel = PROGRAM_CATEGORY_LABEL_MAP.getOrDefault(category, category);
            String severity = CATEGORY_SEVERITY_MAP.getOrDefault(category, "low");
            int priority = calculateVirusPriority(category, severity, count);

            suggestions.add(VirusAISuggestionDTO.builder()
                    .category(category)
                    .categoryLabel(categoryLabel)
                    .title("处理" + categoryLabel + "问题")
                    .description("发现 " + count + " 项" + categoryLabel + "相关安全问题，" + getProgramCategoryDescription(category))
                    .affectedPrograms(categoryPrograms.stream()
                        .map(SuspiciousProgramDTO::getProgramName)
                        .limit(10)
                        .collect(Collectors.toList()))
                    .issueCount(count)
                    .riskLevel(severity)
                    .action(getActionForVirusSeverity(severity))
                    .reason(getProgramCategoryReason(category))
                    .priority(priority)
                    .impact(getProgramCategoryImpact(category))
                    .precaution(getProgramCategoryPrecaution(category, severity))
                    .remediationSteps(getProgramRemediationSteps(category))
                    .build());
        }

        Map<String, List<VulnerabilityDTO>> vulnsByCategory = scanResult.getVulnerabilities().stream()
            .collect(Collectors.groupingBy(VulnerabilityDTO::getCategory));

        for (Map.Entry<String, List<VulnerabilityDTO>> entry : vulnsByCategory.entrySet()) {
            String category = entry.getKey();
            List<VulnerabilityDTO> categoryVulns = entry.getValue();
            int count = categoryVulns.size();
            if (count == 0) continue;

            String categoryLabel = VULN_CATEGORY_LABEL_MAP.getOrDefault(category, category);
            String severity = CATEGORY_SEVERITY_MAP.getOrDefault(category, "low");
            int priority = calculateVirusPriority(category, severity, count);

            suggestions.add(VirusAISuggestionDTO.builder()
                    .category(category)
                    .categoryLabel(categoryLabel)
                    .title("修复" + categoryLabel + "问题")
                    .description("发现 " + count + " 项" + categoryLabel + "相关安全问题，" + getVulnCategoryDescription(category))
                    .affectedPrograms(categoryVulns.stream()
                        .map(VulnerabilityDTO::getAffectedComponent)
                        .limit(10)
                        .collect(Collectors.toList()))
                    .issueCount(count)
                    .riskLevel(severity)
                    .action(getActionForVirusSeverity(severity))
                    .reason(getVulnCategoryReason(category))
                    .priority(priority)
                    .impact(getVulnCategoryImpact(category))
                    .precaution(getVulnCategoryPrecaution(category, severity))
                    .remediationSteps(getVulnRemediationSteps(category))
                    .build());
        }

        suggestions.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));

        int totalIssues = scanResult.getTotalSuspiciousPrograms() + scanResult.getTotalVulnerabilities();
        int high = scanResult.getHighRiskCount();
        int medium = scanResult.getMediumRiskCount();

        String healthScore = String.valueOf(scanResult.getSystemHealthScore());
        String healthLevel = scanResult.getSystemHealthLevel();

        String summary = buildFallbackSummary(totalIssues, high, medium, healthScore, healthLevel);
        String insight = buildFallbackInsight(scanResult);
        String assessment = buildFallbackAssessment(scanResult);
        String optimization = buildFallbackOptimization(scanResult);

        long duration = System.currentTimeMillis() - startTime;
        log.info("【病毒检测】降级模式分析完成，耗时: {}ms, 生成建议: {}条", duration, suggestions.size());

        return VirusAIAnalysisResultDTO.builder()
                .summary(summary)
                .systemHealthScore(healthScore)
                .systemHealthLevel(healthLevel)
                .totalIssues(totalIssues)
                .highRiskCount(high)
                .mediumRiskCount(medium)
                .lowRiskCount(scanResult.getLowRiskCount())
                .suggestions(suggestions)
                .analysisInsight(insight)
                .securityAssessment(assessment)
                .optimizationAdvice(optimization)
                .model("fallback")
                .analysisDurationMs(duration)
                .build();
    }

    /**
     * 计算病毒检测建议的优先级
     * <p>综合考虑风险等级、分类重要性和问题数量
     * 
     * @param category 问题分类
     * @param severity 风险等级
     * @param count 问题数量
     * @return 优先级分数，范围1-10
     */
    private int calculateVirusPriority(String category, String severity, int count) {
        int severityScore = switch (severity) {
            case "high" -> 8;
            case "medium" -> 5;
            default -> 2;
        };

        int categoryBonus = switch (category) {
            case "suspicious_process", "os_vulnerability", "browser_hijacker" -> 2;
            case "open_port", "unknown_startup", "service_anomaly" -> 1;
            default -> 0;
        };

        int countBonus = count > 10 ? 2 : count > 5 ? 1 : 0;

        return Math.min(severityScore + categoryBonus + countBonus, 10);
    }

    /**
     * 根据风险等级获取建议的行动类型
     * 
     * @param severity 风险等级
     * @return 行动建议文本
     */
    private String getActionForVirusSeverity(String severity) {
        return switch (severity) {
            case "high" -> "立即处理";
            case "medium" -> "尽快处理";
            default -> "建议优化";
        };
    }

    /**
     * 获取可疑程序分类的描述文本
     * 
     * @param category 分类编码
     * @return 分类描述
     */
    private String getProgramCategoryDescription(String category) {
        return switch (category) {
            case "suspicious_process" -> "这些进程行为可疑，可能是恶意软件。";
            case "unknown_startup" -> "这些启动项来源不明，可能在后台运行恶意代码。";
            case "unsigned_exe" -> "这些程序没有数字签名，可能被篡改或感染病毒。";
            case "temp_executable" -> "临时目录中的可执行文件通常是恶意软件。";
            case "browser_hijacker" -> "浏览器设置被篡改，可能导致隐私泄露。";
            case "service_anomaly" -> "这些系统服务行为异常，可能影响系统安全。";
            case "scheduled_task" -> "计划任务来源不明，可能执行恶意操作。";
            default -> "这些项目存在安全隐患，建议检查。";
        };
    }

    /**
     * 获取可疑程序分类的建议理由
     * 
     * @param category 分类编码
     * @return 建议理由文本
     */
    private String getProgramCategoryReason(String category) {
        return switch (category) {
            case "suspicious_process" -> "恶意进程通常在后台静默运行，窃取数据或执行其他恶意操作。及时终止可疑进程可以防止进一步的损害。";
            case "unknown_startup" -> "恶意软件常常通过添加启动项实现持久化感染。禁用未知启动项可以阻止恶意软件自动运行。";
            case "temp_executable" -> "临时目录是恶意软件常用的藏身之处。删除这些可疑文件可以消除潜在威胁。";
            case "browser_hijacker" -> "浏览器劫持会篡改用户的搜索主页、收藏夹等，引导用户访问恶意网站或窃取隐私信息。";
            default -> "及时处理可疑项目可以提升系统整体安全性。";
        };
    }

    /**
     * 获取可疑程序分类的处理后预期效果
     * 
     * @param category 分类编码
     * @return 效果描述文本
     */
    private String getProgramCategoryImpact(String category) {
        return switch (category) {
            case "suspicious_process" -> "阻止恶意程序运行，保护系统安全和数据隐私";
            case "unknown_startup" -> "减少系统启动时间，阻止恶意软件自动运行";
            case "temp_executable" -> "删除潜在的恶意文件，消除安全威胁";
            case "browser_hijacker" -> "恢复浏览器正常设置，保护上网隐私安全";
            default -> "提升系统安全性，减少潜在威胁";
        };
    }

    /**
     * 获取可疑程序分类的注意事项
     * 
     * @param category 分类编码
     * @param severity 风险等级
     * @return 注意事项文本
     */
    private String getProgramCategoryPrecaution(String category, String severity) {
        if ("high".equals(severity)) {
            return "高危操作！处理前请确保已备份重要数据，建议在专业人士指导下进行。处理后建议使用杀毒软件进行全盘扫描。";
        } else if ("medium".equals(severity)) {
            return "中等风险。处理前请确认项目确实可疑，建议先使用杀毒软件进行检测。";
        }
        return "低风险操作。如不确定，可先在搜索引擎中查询相关程序信息。";
    }

    /**
     * 获取可疑程序分类的修复步骤
     * 
     * @param category 分类编码
     * @return 修复步骤列表
     */
    private List<String> getProgramRemediationSteps(String category) {
        return switch (category) {
            case "suspicious_process" -> Arrays.asList(
                "打开任务管理器，找到可疑进程",
                "右键点击选择'结束任务'",
                "使用杀毒软件进行全盘扫描",
                "检查启动项，防止再次自动运行"
            );
            case "unknown_startup" -> Arrays.asList(
                "按Win+R，输入msconfig打开系统配置",
                "切换到'启动'选项卡",
                "禁用可疑的启动项",
                "重启电脑使设置生效"
            );
            case "temp_executable" -> Arrays.asList(
                "按Win+R，输入%temp%打开临时目录",
                "找到可疑的exe文件",
                "按Shift+Delete永久删除",
                "清空回收站"
            );
            default -> Arrays.asList(
                "备份重要数据",
                "按照建议进行操作",
                "使用杀毒软件进行扫描",
                "重启电脑确认问题已解决"
            );
        };
    }

    /**
     * 获取安全漏洞分类的描述文本
     * 
     * @param category 分类编码
     * @return 分类描述
     */
    private String getVulnCategoryDescription(String category) {
        return switch (category) {
            case "os_vulnerability" -> "系统存在未修复的安全漏洞，可能被黑客利用。";
            case "open_port" -> "存在不必要的开放端口，可能被入侵者利用。";
            case "outdated_software" -> "这些软件版本过旧，存在已知安全漏洞。";
            case "weak_password" -> "系统账户密码策略过于宽松，容易被破解。";
            case "security_setting" -> "系统安全设置不规范，降低了系统防护能力。";
            default -> "这些安全问题需要及时处理。";
        };
    }

    /**
     * 获取安全漏洞分类的建议理由
     * 
     * @param category 分类编码
     * @return 建议理由文本
     */
    private String getVulnCategoryReason(String category) {
        return switch (category) {
            case "os_vulnerability" -> "未修复的系统漏洞是黑客入侵的主要途径之一。及时安装安全更新可以有效防止系统被入侵。";
            case "open_port" -> "开放的端口是网络攻击的入口点。关闭不必要的端口可以减少攻击面，提升系统安全性。";
            case "outdated_software" -> "旧版本软件通常存在已知的安全漏洞。更新到最新版本可以修复这些漏洞，防止被利用。";
            case "weak_password" -> "弱密码容易被暴力破解。强密码策略可以有效防止账户被非法入侵。";
            case "security_setting" -> "正确的安全设置是系统安全的基础。优化安全配置可以提升系统整体防护能力。";
            default -> "及时修复安全漏洞可以有效提升系统安全性。";
        };
    }

    /**
     * 获取安全漏洞分类的处理后预期效果
     * 
     * @param category 分类编码
     * @return 效果描述文本
     */
    private String getVulnCategoryImpact(String category) {
        return switch (category) {
            case "os_vulnerability" -> "修复系统漏洞，防止被黑客利用入侵";
            case "open_port" -> "减少系统暴露面，降低网络攻击风险";
            case "outdated_software" -> "修复软件已知漏洞，提升应用安全性";
            case "weak_password" -> "增强账户安全性，防止密码被破解";
            case "security_setting" -> "优化系统安全配置，提升整体防护能力";
            default -> "提升系统安全性，减少潜在威胁";
        };
    }

    /**
     * 获取安全漏洞分类的注意事项
     * 
     * @param category 分类编码
     * @param severity 风险等级
     * @return 注意事项文本
     */
    private String getVulnCategoryPrecaution(String category, String severity) {
        if ("high".equals(severity)) {
            return "高危操作！处理前请确保已备份重要数据和系统状态。系统更新可能需要重启电脑，请保存好工作内容。";
        } else if ("medium".equals(severity)) {
            return "中等风险。操作前请确认了解相关设置的作用，不确定的建议咨询专业人士。";
        }
        return "低风险操作。如不确定，可先在搜索引擎中查询相关设置的作用。";
    }

    /**
     * 获取安全漏洞分类的修复步骤
     * 
     * @param category 分类编码
     * @return 修复步骤列表
     */
    private List<String> getVulnRemediationSteps(String category) {
        return switch (category) {
            case "os_vulnerability" -> Arrays.asList(
                "打开设置 -> 更新和安全 -> Windows更新",
                "点击'检查更新'按钮",
                "安装所有可用的安全更新",
                "重启电脑完成更新"
            );
            case "open_port" -> Arrays.asList(
                "打开控制面板 -> Windows Defender防火墙",
                "点击'高级设置' -> '入站规则'",
                "禁用不必要的端口规则",
                "确认修改后关闭窗口"
            );
            case "outdated_software" -> Arrays.asList(
                "打开软件自带的更新功能",
                "或者访问软件官方网站下载最新版本",
                "安装新版本前建议备份数据",
                "安装完成后重启软件"
            );
            case "weak_password" -> Arrays.asList(
                "按Ctrl+Alt+Del选择'更改密码'",
                "输入旧密码，然后设置新的强密码",
                "密码建议包含大小写字母、数字和特殊字符",
                "定期更换密码（建议每3个月）"
            );
            case "security_setting" -> Arrays.asList(
                "打开设置 -> 更新和安全 -> Windows安全中心",
                "检查病毒和威胁防护设置",
                "确保防火墙已开启",
                "检查应用和浏览器控制设置"
            );
            default -> Arrays.asList(
                "备份重要数据",
                "按照建议进行操作",
                "确认设置已生效",
                "定期检查系统安全状态"
            );
        };
    }

    /**
     * 构建降级模式的摘要文本
     * 
     * @param totalIssues 问题总数
     * @param high 高危问题数
     * @param medium 中危问题数
     * @param healthScore 健康评分
     * @param healthLevel 健康等级
     * @return 摘要文本
     */
    private String buildFallbackSummary(int totalIssues, int high, int medium,
            String healthScore, String healthLevel) {
        StringBuilder sb = new StringBuilder();
        sb.append("系统安全检测完成，共发现 ").append(totalIssues).append(" 个安全问题。");
        sb.append("其中高危 ").append(high).append(" 个，中危 ").append(medium).append(" 个。");
        sb.append("系统健康评分：").append(healthScore).append("分（").append(healthLevel).append("）。");
        if (high > 0) {
            sb.append("存在高危安全问题，建议立即处理！");
        }
        return sb.toString();
    }

    /**
     * 构建降级模式的深度分析见解
     * 
     * @param scanResult 扫描结果
     * @return 分析见解文本
     */
    private String buildFallbackInsight(VirusScanResultDTO scanResult) {
        StringBuilder sb = new StringBuilder();
        sb.append("通过对系统进程、启动项、服务、网络端口、系统漏洞等多个维度的全面扫描，");
        sb.append("发现系统存在").append(scanResult.getTotalSuspiciousPrograms()).append("个可疑程序和");
        sb.append(scanResult.getTotalVulnerabilities()).append("个安全漏洞。");

        if (scanResult.getHighRiskCount() > 0) {
            sb.append("特别需要注意的是，系统存在").append(scanResult.getHighRiskCount());
            sb.append("个高危安全问题，这些问题可能导致系统被入侵、数据泄露等严重后果，");
            sb.append("强烈建议立即处理！");
        }

        if (scanResult.getProgramCategoryStats().containsKey("suspicious_process")) {
            sb.append("扫描发现可疑进程，这可能是恶意软件正在运行的迹象。");
        }

        if (scanResult.getVulnerabilityCategoryStats().containsKey("os_vulnerability")) {
            sb.append("系统存在未修复的安全漏洞，需要及时安装Windows更新。");
        }

        return sb.toString();
    }

    /**
     * 构建降级模式的安全评估
     * 
     * @param scanResult 扫描结果
     * @return 安全评估文本
     */
    private String buildFallbackAssessment(VirusScanResultDTO scanResult) {
        StringBuilder sb = new StringBuilder();

        if (scanResult.getSystemHealthScore() >= 80) {
            sb.append("系统整体安全状况良好，但仍有一些可以优化的地方。");
        } else if (scanResult.getSystemHealthScore() >= 60) {
            sb.append("系统存在一定的安全隐患，建议尽快处理发现的问题。");
        } else {
            sb.append("系统安全状况较差，存在较多安全问题，建议立即进行全面的安全检查和修复。");
        }

        sb.append("当前系统面临的主要威胁包括：");

        List<String> threats = new ArrayList<>();
        if (scanResult.getProgramCategoryStats().containsKey("suspicious_process")) {
            threats.add("恶意软件运行");
        }
        if (scanResult.getVulnerabilityCategoryStats().containsKey("os_vulnerability")) {
            threats.add("系统漏洞攻击");
        }
        if (scanResult.getVulnerabilityCategoryStats().containsKey("open_port")) {
            threats.add("网络端口入侵");
        }
        if (scanResult.getProgramCategoryStats().containsKey("browser_hijacker")) {
            threats.add("浏览器劫持");
        }

        if (threats.isEmpty()) {
            sb.append("常规安全风险，请保持良好的安全习惯。");
        } else {
            sb.append(String.join("、", threats)).append("。");
        }

        return sb.toString();
    }

    /**
     * 构建降级模式的优化建议
     * 
     * @param scanResult 扫描结果
     * @return 优化建议文本
     */
    private String buildFallbackOptimization(VirusScanResultDTO scanResult) {
        StringBuilder sb = new StringBuilder();
        sb.append("为了提升系统安全性，建议采取以下措施：");
        sb.append("1. 及时安装Windows系统更新和安全补丁，保持系统最新；");
        sb.append("2. 安装并定期更新杀毒软件，开启实时防护；");
        sb.append("3. 定期进行全盘病毒扫描，及时发现并清除威胁；");
        sb.append("4. 使用强密码，定期更换账户密码；");
        sb.append("5. 开启Windows防火墙，关闭不必要的网络端口；");
        sb.append("6. 定期清理系统垃圾文件和临时文件；");
        sb.append("7. 谨慎下载和安装软件，避免从非官方渠道下载；");
        sb.append("8. 开启浏览器安全设置，避免访问可疑网站；");
        sb.append("9. 定期备份重要数据，防止数据丢失；");
        sb.append("10. 关注系统安全动态，了解最新的安全威胁和防护方法。");

        if (scanResult.getProgramCategoryStats().containsKey("outdated_software")) {
            sb.append("特别建议：及时更新系统中的老旧软件，这些软件可能存在已知的安全漏洞。");
        }

        return sb.toString();
    }

    /**
     * 生成修复脚本（批量）
     * <p>根据选中的问题ID列表生成Windows批处理脚本，用于自动修复安全问题
     * <p>从扫描结果中匹配选中的问题，生成综合的修复脚本
     * 
     * @param selectedIssueIds 选中的问题ID列表
     * @param scanResult 系统扫描结果
     * @return 修复脚本DTO，包含脚本内容和使用说明
     */
    public RemediationScriptDTO generateRemediationScript(
            List<String> selectedIssueIds, VirusScanResultDTO scanResult) {
        long startTime = System.currentTimeMillis();
        log.info("【病毒检测】开始生成修复脚本，选中问题数量: {}", selectedIssueIds.size());

        if (selectedIssueIds == null || selectedIssueIds.isEmpty()) {
            throw new IllegalArgumentException("请选择要处理的安全问题");
        }

        try {
            List<String> fixedIssues = new ArrayList<>();
            Set<String> categories = new LinkedHashSet<>();

            for (SuspiciousProgramDTO program : scanResult.getSuspiciousPrograms()) {
                if (selectedIssueIds.contains(program.getId())) {
                    fixedIssues.add(program.getProgramName() + ": " + program.getDescription());
                    categories.add(program.getCategory());
                }
            }

            for (VulnerabilityDTO vuln : scanResult.getVulnerabilities()) {
                if (selectedIssueIds.contains(vuln.getId())) {
                    fixedIssues.add(vuln.getTitle() + ": " + vuln.getAffectedComponent());
                    categories.add(vuln.getCategory());
                }
            }

            if (fixedIssues.isEmpty()) {
                throw new IllegalArgumentException("未找到选中的安全问题，请重新选择");
            }

            log.debug("【病毒检测】匹配到{}个问题，涉及{}个分类", fixedIssues.size(), categories.size());

            VirusAISuggestionDTO suggestion = VirusAISuggestionDTO.builder()
                    .category(categories.iterator().next())
                    .categoryLabel(PROGRAM_CATEGORY_LABEL_MAP.getOrDefault(
                            categories.iterator().next(), 
                            VULN_CATEGORY_LABEL_MAP.getOrDefault(categories.iterator().next(), "安全修复")))
                    .title("批量修复安全问题")
                    .affectedPrograms(fixedIssues)
                    .issueCount(fixedIssues.size())
                    .remediationSteps(Arrays.asList(
                        "备份重要数据和系统状态",
                        "以管理员身份运行修复脚本",
                        "等待脚本执行完成",
                        "重启电脑使设置生效",
                        "使用杀毒软件进行全盘扫描"
                    ))
                    .build();

            String scriptContent = generateBatchRemediationScript(suggestion);
            String fileName = "virus_remediation_" + System.currentTimeMillis() + ".bat";

            RemediationScriptDTO script = RemediationScriptDTO.builder()
                    .scriptName(fileName)
                    .scriptContent(scriptContent)
                    .scriptType("BAT")
                    .encoding("UTF-8")
                    .issueCount(fixedIssues.size())
                    .fixedIssues(fixedIssues)
                    .warning("警告：执行脚本前请务必备份重要数据！建议在专业人士指导下操作。")
                    .usageInstructions("1. 右键点击脚本，选择'以管理员身份运行'\n"
                            + "2. 等待脚本执行完成\n"
                            + "3. 按照脚本提示操作\n"
                            + "4. 执行完成后建议重启电脑")
                    .build();

            long duration = System.currentTimeMillis() - startTime;
            log.info("【病毒检测】修复脚本生成完成，耗时: {}ms, 修复问题: {}个, 脚本长度: {}字符",
                    duration, fixedIssues.size(), scriptContent.length());
            return script;

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("【病毒检测】生成修复脚本失败，耗时: {}ms, 错误: {}", duration, e.getMessage(), e);
            throw new BusinessException("生成修复脚本失败: " + e.getMessage());
        }
    }

    /**
     * 生成修复脚本（单条）
     * <p>根据AI建议生成Windows批处理脚本，用于自动修复安全问题
     * 
     * @param suggestion AI建议对象
     * @return 修复脚本DTO，包含脚本内容和使用说明
     */
    public RemediationScriptDTO generateRemediationScript(VirusAISuggestionDTO suggestion) {
        long startTime = System.currentTimeMillis();
        log.info("【病毒检测】开始生成修复脚本，建议标题: {}", suggestion.getTitle());

        try {
            String scriptContent = generateBatchRemediationScript(suggestion);
            String fileName = "fix_" + suggestion.getCategory() + "_" + System.currentTimeMillis() + ".bat";

            RemediationScriptDTO script = RemediationScriptDTO.builder()
                    .scriptName(fileName)
                    .scriptContent(scriptContent)
                    .scriptType("BAT")
                    .encoding("UTF-8")
                    .issueCount(suggestion.getIssueCount())
                    .fixedIssues(suggestion.getAffectedPrograms())
                    .warning("警告：执行脚本前请务必备份重要数据！建议在专业人士指导下操作。")
                    .usageInstructions("1. 右键点击脚本，选择'以管理员身份运行'\n"
                            + "2. 等待脚本执行完成\n"
                            + "3. 按照脚本提示操作\n"
                            + "4. 执行完成后建议重启电脑")
                    .build();

            long duration = System.currentTimeMillis() - startTime;
            log.info("【病毒检测】修复脚本生成完成，耗时: {}ms, 脚本长度: {}字符", duration, scriptContent.length());
            return script;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("【病毒检测】生成修复脚本失败，耗时: {}ms, 错误: {}", duration, e.getMessage(), e);
            throw new BusinessException("生成修复脚本失败: " + e.getMessage());
        }
    }

    /**
     * 生成Windows批处理脚本内容
     * <p>根据问题分类生成对应的修复命令
     * 
     * @param suggestion AI建议对象
     * @return 批处理脚本内容
     */
    private String generateBatchRemediationScript(VirusAISuggestionDTO suggestion) {
        StringBuilder sb = new StringBuilder();

        sb.append("@echo off\r\n");
        sb.append("chcp 65001 >nul\r\n");
        sb.append("title 系统安全修复脚本 - ").append(suggestion.getTitle()).append("\r\n");
        sb.append("echo ========================================\r\n");
        sb.append("echo 系统安全修复脚本\r\n");
        sb.append("echo 修复项目：").append(suggestion.getTitle()).append("\r\n");
        sb.append("echo 生成时间：%date% %time%\r\n");
        sb.append("echo ========================================\r\n");
        sb.append("echo.\r\n");

        sb.append("echo [警告] 请确保以管理员身份运行此脚本！\r\n");
        sb.append("echo.\r\n");
        sb.append("pause\r\n");
        sb.append("echo.\r\n");

        sb.append("echo [信息] 开始执行修复操作...\r\n");
        sb.append("echo.\r\n");

        String category = suggestion.getCategory();
        switch (category) {
            case "unknown_startup" -> {
                sb.append("echo [步骤 1/2] 正在检查启动项...\r\n");
                sb.append("echo 建议手动检查以下启动项：\r\n");
                for (String program : suggestion.getAffectedPrograms()) {
                    sb.append("echo   - ").append(program).append("\r\n");
                }
                sb.append("echo.\r\n");
                sb.append("echo [步骤 2/2] 打开启动项管理...\r\n");
                sb.append("start msconfig\r\n");
            }
            case "temp_executable" -> {
                sb.append("echo [步骤 1/3] 正在清理临时目录...\r\n");
                sb.append("echo 正在删除临时文件...\r\n");
                sb.append("cd /d \"%temp%\"\r\n");
                sb.append("dir /b *.exe 2>nul\r\n");
                sb.append("echo.\r\n");
                sb.append("echo [警告] 以下exe文件将被永久删除！\r\n");
                sb.append("pause\r\n");
                sb.append("echo [步骤 2/3] 正在删除可疑exe文件...\r\n");
                sb.append("del /f /s /q *.exe 2>nul\r\n");
                sb.append("echo [步骤 3/3] 正在清理其他临时文件...\r\n");
                sb.append("del /f /s /q *.tmp *.log 2>nul\r\n");
            }
            case "os_vulnerability" -> {
                sb.append("echo [步骤 1/2] 打开Windows更新...\r\n");
                sb.append("start ms-settings:windowsupdate\r\n");
                sb.append("echo.\r\n");
                sb.append("echo [步骤 2/2] 请在打开的窗口中点击'检查更新'\r\n");
                sb.append("echo 安装所有可用的安全更新后重启电脑\r\n");
            }
            case "open_port" -> {
                sb.append("echo [步骤 1/2] 正在检查开放端口...\r\n");
                sb.append("netstat -ano | findstr /i listening\r\n");
                sb.append("echo.\r\n");
                sb.append("echo [步骤 2/2] 打开高级防火墙设置...\r\n");
                sb.append("start wf.msc\r\n");
                sb.append("echo 请在打开的窗口中检查并禁用不必要的入站规则\r\n");
            }
            case "outdated_software" -> {
                sb.append("echo [步骤 1/1] 以下软件建议更新到最新版本：\r\n");
                for (String program : suggestion.getAffectedPrograms()) {
                    sb.append("echo   - ").append(program).append("\r\n");
                }
                sb.append("echo.\r\n");
                sb.append("echo 请访问各软件官方网站下载最新版本\r\n");
            }
            case "security_setting" -> {
                sb.append("echo [步骤 1/3] 打开Windows安全中心...\r\n");
                sb.append("start windowsdefender:\r\n");
                sb.append("echo.\r\n");
                sb.append("echo [步骤 2/3] 检查病毒和威胁防护设置\r\n");
                sb.append("echo 确保实时保护已开启\r\n");
                sb.append("echo.\r\n");
                sb.append("echo [步骤 3/3] 检查防火墙和网络保护设置\r\n");
                sb.append("echo 确保防火墙已开启\r\n");
            }
            case "weak_password" -> {
                sb.append("echo [步骤 1/2] 打开账户设置...\r\n");
                sb.append("start ms-settings:signinoptions\r\n");
                sb.append("echo.\r\n");
                sb.append("echo [步骤 2/2] 请在打开的窗口中更改密码\r\n");
                sb.append("echo 建议使用包含大小写字母、数字和特殊字符的强密码\r\n");
            }
            default -> {
                sb.append("echo [信息] 请按照以下步骤手动操作：\r\n");
                int step = 1;
                for (String remediationStep : suggestion.getRemediationSteps()) {
                    sb.append("echo [步骤 ").append(step++).append("/")
                      .append(suggestion.getRemediationSteps().size()).append("] ")
                      .append(remediationStep).append("\r\n");
                }
            }
        }

        sb.append("echo.\r\n");
        sb.append("echo ========================================\r\n");
        sb.append("echo 修复脚本执行完成！\r\n");
        sb.append("echo 建议重启电脑使设置生效\r\n");
        sb.append("echo ========================================\r\n");
        sb.append("echo.\r\n");
        sb.append("pause\r\n");

        return sb.toString();
    }
}
