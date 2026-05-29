package com.example.agentplatform.tools.controller;

import com.example.agentplatform.common.dto.Result;
import com.example.agentplatform.tools.dto.AIAnalysisResultDTO;
import com.example.agentplatform.tools.dto.DriveAnalysisResultDTO;
import com.example.agentplatform.tools.dto.DriveInfoDTO;
import com.example.agentplatform.tools.service.DiskAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/agent-tools/disk")
@RequiredArgsConstructor
public class DiskAnalysisController {

    private final DiskAnalysisService diskAnalysisService;

    @GetMapping("/drives")
    public ResponseEntity<Result<List<DriveInfoDTO>>> getAvailableDrives() {
        List<DriveInfoDTO> drives = diskAnalysisService.getAvailableDrives();
        return ResponseEntity.ok(Result.success(drives));
    }

    @GetMapping("/analyze")
    public ResponseEntity<Result<DriveAnalysisResultDTO>> analyzeDrive(
            @RequestParam String drive,
            @RequestParam(required = false) Integer maxDepth) {
        if (drive == null || drive.length() != 1 || !Character.isLetter(drive.charAt(0))) {
            return ResponseEntity.badRequest().body(Result.badRequest("无效的盘符，请输入单个字母如 C"));
        }
        drive = drive.toUpperCase();

        try {
            DriveAnalysisResultDTO result = diskAnalysisService.analyzeDrive(drive, maxDepth);
            return ResponseEntity.ok(Result.success("磁盘分析完成", result));
        } catch (SecurityException e) {
            log.error("无权限访问盘符: {}", drive, e);
            return ResponseEntity.status(403).body(Result.forbidden("无权限访问 " + drive + ": 盘"));
        } catch (Exception e) {
            log.error("分析盘符失败: {}", drive, e);
            return ResponseEntity.status(500).body(Result.serverError("分析盘符失败: " + e.getMessage()));
        }
    }

    @GetMapping("/ai-analyze")
    public ResponseEntity<Result<AIAnalysisResultDTO>> aiAnalyzeDrive(
            @RequestParam String drive,
            @RequestParam(required = false) Integer maxDepth) {
        if (drive == null || drive.length() != 1 || !Character.isLetter(drive.charAt(0))) {
            return ResponseEntity.badRequest().body(Result.badRequest("无效的盘符，请输入单个字母如 C"));
        }
        drive = drive.toUpperCase();

        try {
            AIAnalysisResultDTO result = diskAnalysisService.aiAnalyzeDrive(drive, maxDepth);
            return ResponseEntity.ok(Result.success("AI磁盘分析完成", result));
        } catch (SecurityException e) {
            log.error("无权限访问盘符: {}", drive, e);
            return ResponseEntity.status(403).body(Result.forbidden("无权限访问 " + drive + ": 盘"));
        } catch (Exception e) {
            log.error("AI分析盘符失败: {}", drive, e);
            return ResponseEntity.status(500).body(Result.serverError("AI分析失败: " + e.getMessage()));
        }
    }
}
