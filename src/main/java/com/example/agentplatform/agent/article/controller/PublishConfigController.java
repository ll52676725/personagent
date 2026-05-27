package com.example.agentplatform.agent.article.controller;

import com.example.agentplatform.agent.article.dto.PublishConfigDTO;
import com.example.agentplatform.agent.article.entity.PublishConfig;
import com.example.agentplatform.agent.article.service.PublishConfigService;
import com.example.agentplatform.common.dto.Result;
import com.example.agentplatform.common.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/agent-article/publish-configs")
@RequiredArgsConstructor
public class PublishConfigController {

    private final PublishConfigService publishConfigService;

    @GetMapping
    public ResponseEntity<Result<List<PublishConfig>>> listConfigs() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<PublishConfig> configs = publishConfigService.listConfigs(userId);
        return ResponseEntity.ok(Result.success(configs));
    }

    @GetMapping("/enabled")
    public ResponseEntity<Result<List<PublishConfig>>> listEnabledConfigs() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<PublishConfig> configs = publishConfigService.listEnabledConfigs(userId);
        return ResponseEntity.ok(Result.success(configs));
    }

    @GetMapping("/{platform}")
    public ResponseEntity<Result<PublishConfig>> getConfig(@PathVariable String platform) {
        Long userId = SecurityUtils.getCurrentUserId();
        return publishConfigService.getConfig(userId, platform)
                .map(config -> ResponseEntity.ok(Result.success(config)))
                .orElse(ResponseEntity.ok(Result.success(null)));
    }

    @PostMapping
    public ResponseEntity<Result<PublishConfig>> saveConfig(
            @Valid @RequestBody PublishConfigDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        PublishConfig config = publishConfigService.saveConfig(userId, dto);
        return ResponseEntity.ok(Result.success("配置保存成功", config));
    }

    @DeleteMapping("/{platform}")
    public ResponseEntity<Result<Void>> deleteConfig(@PathVariable String platform) {
        Long userId = SecurityUtils.getCurrentUserId();
        publishConfigService.deleteConfig(userId, platform);
        return ResponseEntity.ok(Result.success("配置删除成功", null));
    }

    @PutMapping("/{platform}/status")
    public ResponseEntity<Result<PublishConfig>> updateConfigStatus(
            @PathVariable String platform,
            @RequestBody Map<String, Boolean> request) {
        Long userId = SecurityUtils.getCurrentUserId();
        Boolean enabled = request.get("enabled");
        if (enabled == null) {
            enabled = true;
        }
        PublishConfig config = publishConfigService.updateConfigStatus(userId, platform, enabled);
        return ResponseEntity.ok(Result.success("状态更新成功", config));
    }
}
