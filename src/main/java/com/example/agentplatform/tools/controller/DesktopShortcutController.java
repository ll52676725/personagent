package com.example.agentplatform.tools.controller;

import com.example.agentplatform.common.dto.Result;
import com.example.agentplatform.tools.dto.DesktopShortcutRequestDTO;
import com.example.agentplatform.tools.dto.DesktopShortcutResultDTO;
import com.example.agentplatform.tools.service.DesktopShortcutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/agent-tools/desktop-shortcut")
@RequiredArgsConstructor
public class DesktopShortcutController {

    private final DesktopShortcutService desktopShortcutService;

    @PostMapping("/create")
    public ResponseEntity<Result<DesktopShortcutResultDTO>> createShortcut(
            @Valid @RequestBody DesktopShortcutRequestDTO request) {
        log.info("收到创建桌面快捷方式请求: toolId={}, toolName={}", request.getToolId(), request.getToolName());
        DesktopShortcutResultDTO result = desktopShortcutService.createShortcut(request);
        if (result.getSuccess()) {
            return ResponseEntity.ok(Result.success("桌面快捷方式创建成功", result));
        } else {
            return ResponseEntity.badRequest().body(Result.<DesktopShortcutResultDTO>builder()
                    .code(400)
                    .message(result.getMessage())
                    .data(result)
                    .timestamp(System.currentTimeMillis())
                    .build());
        }
    }

    @GetMapping("/desktop-path")
    public ResponseEntity<Result<String>> getDesktopPath() {
        String desktopPath = desktopShortcutService.getDesktopPath();
        if (desktopPath != null) {
            return ResponseEntity.ok(Result.success("获取桌面路径成功", desktopPath));
        } else {
            return ResponseEntity.badRequest().body(Result.badRequest("无法定位桌面路径"));
        }
    }
}
