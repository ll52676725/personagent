package com.example.agentplatform.tools.controller;

import com.example.agentplatform.common.dto.Result;
import com.example.agentplatform.tools.dto.*;
import com.example.agentplatform.tools.service.VirusDetectionService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * 病毒检测REST API控制器
 * <p>提供系统安全检测、AI智能分析、修复脚本生成等前端访问接口
 * <p>所有接口路径统一前缀：/api/v1/agent-tools/virus
 * <p>主要功能：
 * <ul>
 *   <li>系统安全扫描：扫描可疑进程、启动项、服务、漏洞等</li>
 *   <li>AI智能分析：调用AI对扫描结果进行深度分析，提供专业建议</li>
 *   <li>修复脚本生成：根据选中的问题生成自动修复脚本</li>
 *   <li>脚本下载：支持下载生成的修复脚本文件</li>
 * </ul>
 * 
 * @author System
 * @since 2025-01-01
 * @see VirusDetectionService
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agent-tools/virus")
@RequiredArgsConstructor
public class VirusDetectorController {

    /**
     * 病毒检测核心服务，提供所有业务逻辑实现
     */
    private final VirusDetectionService virusDetectionService;

    /**
     * Session存储键名，用于存储扫描结果供后续接口使用
     */
    private static final String SESSION_KEY = "virus_scan_result";

    /**
     * 执行系统安全扫描
     * <p>GET /api/v1/agent-tools/virus/scan
     * <p>扫描内容包括：可疑进程、启动项、临时目录、系统服务、计划任务、
     * 系统漏洞、开放端口、过期软件、安全设置等9个维度
     * <p>扫描结果会存入Session供后续生成修复脚本使用
     * 
     * @param session HTTP会话对象，用于存储扫描结果
     * @return 扫描结果DTO，包含可疑程序列表、漏洞列表、健康评分等
     */
    @GetMapping("/scan")
    public ResponseEntity<Result<VirusScanResultDTO>> scanSystem(HttpSession session) {
        long startTime = System.currentTimeMillis();
        String sessionId = session.getId();
        log.info("【病毒检测】API: 开始执行系统安全扫描, 会话ID: {}", sessionId);

        try {
            VirusScanResultDTO result = virusDetectionService.scanSystem();
            session.setAttribute(SESSION_KEY, result);

            long duration = System.currentTimeMillis() - startTime;
            log.info("【病毒检测】API: 系统安全扫描完成, 会话ID: {}, 耗时: {}ms, " +
                    "发现可疑程序: {}个, 安全漏洞: {}个, 健康评分: {}分",
                    sessionId, duration, result.getTotalSuspiciousPrograms(),
                    result.getTotalVulnerabilities(), result.getSystemHealthScore());

            return ResponseEntity.ok(Result.success("系统安全扫描完成", result));

        } catch (SecurityException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("【病毒检测】API: 无权限执行系统扫描, 会话ID: {}, 耗时: {}ms, 错误: {}",
                    sessionId, duration, e.getMessage(), e);
            return ResponseEntity.status(403).body(Result.forbidden("无权限执行系统扫描，请以管理员身份运行"));

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("【病毒检测】API: 系统扫描失败, 会话ID: {}, 耗时: {}ms, 错误: {}",
                    sessionId, duration, e.getMessage(), e);
            return ResponseEntity.status(500).body(Result.serverError("系统扫描失败: " + e.getMessage()));
        }
    }

    /**
     * AI智能分析系统安全状况
     * <p>GET /api/v1/agent-tools/virus/ai-analyze
     * <p>先执行完整的系统扫描，然后调用AI对扫描结果进行深度分析，
     * 提供专业的安全评估、问题诊断和修复建议
     * <p>当AI服务不可用时，自动降级为本地规则引擎分析
     * 
     * @return AI分析结果DTO，包含安全评估、建议列表、优化方案等
     */
    @GetMapping("/ai-analyze")
    public ResponseEntity<Result<VirusAIAnalysisResultDTO>> aiAnalyzeSystem() {
        long startTime = System.currentTimeMillis();
        log.info("【病毒检测】API: 开始AI智能分析系统安全");

        try {
            VirusAIAnalysisResultDTO result = virusDetectionService.aiAnalyzeSystem();

            long duration = System.currentTimeMillis() - startTime;
            log.info("【病毒检测】API: AI智能分析完成, 耗时: {}ms, " +
                    "模型: {}, Token消耗: {}, 生成建议: {}条, 健康评分: {}分",
                    duration, result.getModel(), result.getTokens(),
                    result.getSuggestions().size(), result.getSystemHealthScore());

            return ResponseEntity.ok(Result.success("AI系统安全分析完成", result));

        } catch (SecurityException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("【病毒检测】API: 无权限执行系统扫描, 耗时: {}ms, 错误: {}",
                    duration, e.getMessage(), e);
            return ResponseEntity.status(403).body(Result.forbidden("无权限执行系统扫描，请以管理员身份运行"));

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("【病毒检测】API: AI分析系统安全失败, 耗时: {}ms, 错误: {}",
                    duration, e.getMessage(), e);
            return ResponseEntity.status(500).body(Result.serverError("AI分析失败: " + e.getMessage()));
        }
    }

    /**
     * 生成修复脚本
     * <p>POST /api/v1/agent-tools/virus/generate-script
     * <p>根据用户选择的安全问题ID列表，从Session中获取扫描结果，
     * 匹配对应的问题并生成Windows批处理修复脚本
     * 
     * @param request 请求DTO，包含选中的问题ID列表
     * @param session HTTP会话对象，用于获取之前的扫描结果
     * @return 修复脚本DTO，包含脚本内容、使用说明、警告信息等
     */
    @PostMapping("/generate-script")
    public ResponseEntity<Result<RemediationScriptDTO>> generateRemediationScript(
            @RequestBody GenerateRemediationScriptRequestDTO request,
            HttpSession session) {

        long startTime = System.currentTimeMillis();
        String sessionId = session.getId();
        log.info("【病毒检测】API: 开始生成修复脚本, 会话ID: {}, 选中问题数量: {}",
                sessionId, request.getSelectedIssueIds() != null ? request.getSelectedIssueIds().size() : 0);

        VirusScanResultDTO scanResult = (VirusScanResultDTO) session.getAttribute(SESSION_KEY);

        if (scanResult == null) {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("【病毒检测】API: 未找到扫描结果, 会话ID: {}, 耗时: {}ms", sessionId, duration);
            return ResponseEntity.badRequest().body(Result.badRequest("请先执行系统安全扫描"));
        }

        if (request.getSelectedIssueIds() == null || request.getSelectedIssueIds().isEmpty()) {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("【病毒检测】API: 未选择任何问题, 会话ID: {}, 耗时: {}ms", sessionId, duration);
            return ResponseEntity.badRequest().body(Result.badRequest("请选择要处理的安全问题"));
        }

        try {
            RemediationScriptDTO script = virusDetectionService.generateRemediationScript(
                request.getSelectedIssueIds(), scanResult);

            long duration = System.currentTimeMillis() - startTime;
            log.info("【病毒检测】API: 修复脚本生成成功, 会话ID: {}, 耗时: {}ms, " +
                    "修复问题: {}个, 脚本长度: {}字符",
                    sessionId, duration, script.getIssueCount(), script.getScriptContent().length());

            return ResponseEntity.ok(Result.success("修复脚本生成成功", script));

        } catch (IllegalArgumentException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("【病毒检测】API: 生成修复脚本参数错误, 会话ID: {}, 耗时: {}ms, 错误: {}",
                    sessionId, duration, e.getMessage());
            return ResponseEntity.badRequest().body(Result.badRequest(e.getMessage()));

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("【病毒检测】API: 生成修复脚本失败, 会话ID: {}, 耗时: {}ms, 错误: {}",
                    sessionId, duration, e.getMessage(), e);
            return ResponseEntity.status(500).body(Result.serverError("生成修复脚本失败: " + e.getMessage()));
        }
    }

    /**
     * 下载修复脚本文件
     * <p>POST /api/v1/agent-tools/virus/download-script
     * <p>生成修复脚本并以文件形式返回，支持浏览器直接下载
     * <p>脚本文件编码：UTF-8，文件名包含时间戳避免重复
     * 
     * @param request 请求DTO，包含选中的问题ID列表
     * @param session HTTP会话对象，用于获取之前的扫描结果
     * @return 脚本文件字节流，包含Content-Disposition头信息
     */
    @PostMapping("/download-script")
    public ResponseEntity<byte[]> downloadScript(
            @RequestBody GenerateRemediationScriptRequestDTO request,
            HttpSession session) {

        long startTime = System.currentTimeMillis();
        String sessionId = session.getId();
        log.info("【病毒检测】API: 开始下载修复脚本, 会话ID: {}, 选中问题数量: {}",
                sessionId, request.getSelectedIssueIds() != null ? request.getSelectedIssueIds().size() : 0);

        VirusScanResultDTO scanResult = (VirusScanResultDTO) session.getAttribute(SESSION_KEY);

        if (scanResult == null) {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("【病毒检测】API: 下载脚本未找到扫描结果, 会话ID: {}, 耗时: {}ms", sessionId, duration);
            return ResponseEntity.badRequest().build();
        }

        try {
            RemediationScriptDTO script = virusDetectionService.generateRemediationScript(
                request.getSelectedIssueIds(), scanResult);

            byte[] content;
            if ("UTF-16LE".equalsIgnoreCase(script.getEncoding())) {
                byte[] bom = {(byte) 0xFF, (byte) 0xFE};
                byte[] scriptBytes = script.getScriptContent().getBytes(StandardCharsets.UTF_16LE);
                content = new byte[bom.length + scriptBytes.length];
                System.arraycopy(bom, 0, content, 0, bom.length);
                System.arraycopy(scriptBytes, 0, content, bom.length, scriptBytes.length);
            } else {
                content = script.getScriptContent().getBytes(StandardCharsets.UTF_8);
            }

            String fileName = script.getScriptName();
            String encodedFileName = Base64.getEncoder().encodeToString(fileName.getBytes(StandardCharsets.UTF_8));

            long duration = System.currentTimeMillis() - startTime;
            log.info("【病毒检测】API: 修复脚本下载成功, 会话ID: {}, 耗时: {}ms, " +
                    "文件名: {}, 文件大小: {}字节",
                    sessionId, duration, fileName, content.length);

            return ResponseEntity.ok()
                    .header("Content-Type", "application/octet-stream")
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encodedFileName)
                    .body(content);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("【病毒检测】API: 下载脚本失败, 会话ID: {}, 耗时: {}ms, 错误: {}",
                    sessionId, duration, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }
}
