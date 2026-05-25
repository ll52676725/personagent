package com.example.agentplatform.agent.article.controller;

import com.example.agentplatform.agent.article.dto.GenerateRequestDTO;
import com.example.agentplatform.agent.article.dto.GenerateResult;
import com.example.agentplatform.agent.article.service.GenerateService;
import com.example.agentplatform.common.dto.Result;
import com.example.agentplatform.common.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/agent-article/generate")
@RequiredArgsConstructor
public class GenerateController {
    
    private final GenerateService generateService;
    
    @PostMapping("/title")
    public ResponseEntity<Result<GenerateResult>> generateTitle(@Valid @RequestBody GenerateRequestDTO request) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("用户 {} 生成标题，主题: {}", userId, request.getTopic());
        
        GenerateResult result = generateService.generateTitle(request);
        return ResponseEntity.ok(Result.success(result));
    }
    
    @PostMapping("/summary")
    public ResponseEntity<Result<GenerateResult>> generateSummary(@Valid @RequestBody GenerateRequestDTO request) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("用户 {} 生成概要，标题: {}", userId, request.getTitle());
        
        GenerateResult result = generateService.generateSummary(request);
        return ResponseEntity.ok(Result.success(result));
    }
    
    @PostMapping("/content")
    public ResponseEntity<Result<GenerateResult>> generateContent(@Valid @RequestBody GenerateRequestDTO request) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("用户 {} 生成正文，标题: {}", userId, request.getTitle());
        
        GenerateResult result = generateService.generateContent(request);
        return ResponseEntity.ok(Result.success(result));
    }
}