package com.example.agentplatform.tools.controller;

import com.example.agentplatform.common.dto.Result;
import com.example.agentplatform.tools.dto.*;
import com.example.agentplatform.tools.service.RegistryCleanerService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/agent-tools/registry")
@RequiredArgsConstructor
public class RegistryCleanerController {

    private final RegistryCleanerService registryCleanerService;

    private static final String SESSION_KEY = "registry_analysis_result";

    @GetMapping("/analyze")
    public ResponseEntity<Result<RegistryAnalysisResultDTO>> analyzeRegistry(HttpSession session) {
        try {
            RegistryAnalysisResultDTO result = registryCleanerService.analyzeRegistry();
            session.setAttribute(SESSION_KEY, result.getIssues());
            return ResponseEntity.ok(Result.success("注册表分析完成", result));
        } catch (SecurityException e) {
            log.error("无权限访问注册表", e);
            return ResponseEntity.status(403).body(Result.forbidden("无权限访问注册表，请以管理员身份运行"));
        } catch (Exception e) {
            log.error("分析注册表失败", e);
            return ResponseEntity.status(500).body(Result.serverError("分析注册表失败: " + e.getMessage()));
        }
    }

    @PostMapping("/generate-script")
    public ResponseEntity<Result<CleanupScriptDTO>> generateCleanupScript(
            @RequestBody GenerateScriptRequestDTO request,
            HttpSession session) {
        @SuppressWarnings("unchecked")
        List<RegistryIssueDTO> allIssues = (List<RegistryIssueDTO>) session.getAttribute(SESSION_KEY);

        if (allIssues == null || allIssues.isEmpty()) {
            return ResponseEntity.badRequest().body(Result.badRequest("请先执行注册表分析"));
        }

        if (request.getSelectedIssueIds() == null || request.getSelectedIssueIds().isEmpty()) {
            return ResponseEntity.badRequest().body(Result.badRequest("请选择要清理的注册表项"));
        }

        try {
            CleanupScriptDTO script = registryCleanerService.generateCleanupScript(request, allIssues);
            return ResponseEntity.ok(Result.success("清理脚本生成成功", script));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Result.badRequest(e.getMessage()));
        } catch (Exception e) {
            log.error("生成清理脚本失败", e);
            return ResponseEntity.status(500).body(Result.serverError("生成清理脚本失败: " + e.getMessage()));
        }
    }

    @PostMapping("/download-script")
    public ResponseEntity<byte[]> downloadScript(
            @RequestBody GenerateScriptRequestDTO request,
            HttpSession session) {
        @SuppressWarnings("unchecked")
        List<RegistryIssueDTO> allIssues = (List<RegistryIssueDTO>) session.getAttribute(SESSION_KEY);

        if (allIssues == null || allIssues.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            CleanupScriptDTO script = registryCleanerService.generateCleanupScript(request, allIssues);
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

            return ResponseEntity.ok()
                    .header("Content-Type", "application/octet-stream")
                    .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encodedFileName)
                    .body(content);
        } catch (Exception e) {
            log.error("下载脚本失败", e);
            return ResponseEntity.status(500).build();
        }
    }

    @GetMapping("/ai-analyze")
    public ResponseEntity<Result<RegistryAIAnalysisResultDTO>> aiAnalyzeRegistry() {
        try {
            RegistryAIAnalysisResultDTO result = registryCleanerService.aiAnalyzeRegistry();
            return ResponseEntity.ok(Result.success("AI注册表分析完成", result));
        } catch (SecurityException e) {
            log.error("无权限访问注册表", e);
            return ResponseEntity.status(403).body(Result.forbidden("无权限访问注册表，请以管理员身份运行"));
        } catch (Exception e) {
            log.error("AI分析注册表失败", e);
            return ResponseEntity.status(500).body(Result.serverError("AI分析失败: " + e.getMessage()));
        }
    }
}
