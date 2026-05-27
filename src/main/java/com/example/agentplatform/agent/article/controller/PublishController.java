package com.example.agentplatform.agent.article.controller;

import com.example.agentplatform.agent.article.dto.PublishDTO;
import com.example.agentplatform.agent.article.dto.PublishResult;
import com.example.agentplatform.agent.article.service.PublishService;
import com.example.agentplatform.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/article/publish")
@RequiredArgsConstructor
public class PublishController {

    private final PublishService publishService;

    @PostMapping("/{articleId}")
    public ResponseEntity<List<PublishResult>> publishArticle(
            @PathVariable Long articleId,
            @RequestBody PublishDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("用户 {} 发布文章 {} 到平台: {}", userId, articleId, dto.getPlatforms());
        List<PublishResult> results = publishService.publishArticle(userId, articleId, dto);
        return ResponseEntity.ok(results);
    }

    @PostMapping("/auto/{articleId}")
    public ResponseEntity<List<PublishResult>> autoPublish(@PathVariable Long articleId) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("用户 {} 一键发布文章 {} 到所有平台", userId, articleId);
        List<PublishResult> results = publishService.autoPublish(userId, articleId);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/preview/{articleId}/{platform}")
    public ResponseEntity<String> previewForPlatform(
            @PathVariable Long articleId,
            @PathVariable String platform) {
        Long userId = SecurityUtils.getCurrentUserId();
        String content = publishService.previewForPlatform(userId, articleId, platform);
        return ResponseEntity.ok(content);
    }

    @GetMapping("/preview/{articleId}")
    public ResponseEntity<Map<String, String>> previewAllPlatforms(@PathVariable Long articleId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Map<String, String> previews = publishService.previewAllPlatforms(userId, articleId);
        return ResponseEntity.ok(previews);
    }

    @GetMapping("/validate/{articleId}/{platform}")
    public ResponseEntity<Boolean> validateForPlatform(
            @PathVariable Long articleId,
            @PathVariable String platform) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean valid = publishService.validateForPlatform(userId, articleId, platform);
        return ResponseEntity.ok(valid);
    }
}
