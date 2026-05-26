package com.example.agentplatform.agent.article.controller;

import com.example.agentplatform.agent.article.dto.GenerateRequestDTO;
import com.example.agentplatform.agent.article.dto.GenerateResult;
import com.example.agentplatform.agent.article.service.GenerateService;
import com.example.agentplatform.common.security.SecurityUtils;
import com.example.agentplatform.common.dto.Result;
import com.example.agentplatform.common.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/agent-article")
@RequiredArgsConstructor
public class GenerateController {
    
    private final GenerateService generateService;
    
    @PostMapping("/generate/title")
    public ResponseEntity<Result<GenerateResult>> generateTitle(@Valid @RequestBody GenerateRequestDTO request) {
        Long userId = SecurityUtils.getCurrentUserId();
        
        if (request.getTopic() == null || request.getTopic().isBlank()) {
            throw new BusinessException("主题不能为空");
        }
        
        log.info("用户 {} 生成标题，主题: {}", userId, request.getTopic());
        
        GenerateResult result = generateService.generateTitle(request);
        return ResponseEntity.ok(Result.success(result));
    }
    
    @PostMapping(value = "/generate/title/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateTitleStream(@Valid @RequestBody GenerateRequestDTO request) {
        Long userId = SecurityUtils.getCurrentUserId();
        
        if (request.getTopic() == null || request.getTopic().isBlank()) {
            return Flux.error(new BusinessException("主题不能为空"));
        }
        
        log.info("用户 {} 流式生成标题，主题: {}", userId, request.getTopic());
        return generateService.generateTitleStream(request);
    }
    
    @PostMapping("/generate/summary")
    public ResponseEntity<Result<GenerateResult>> generateSummary(@Valid @RequestBody GenerateRequestDTO request) {
        Long userId = SecurityUtils.getCurrentUserId();
        
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BusinessException("文章标题不能为空");
        }
        
        log.info("用户 {} 生成摘要，标题: {}", userId, request.getTitle());
        
        GenerateResult result = generateService.generateSummary(request);
        return ResponseEntity.ok(Result.success(result));
    }
    
    @PostMapping(value = "/generate/summary/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateSummaryStream(@Valid @RequestBody GenerateRequestDTO request) {
        Long userId = SecurityUtils.getCurrentUserId();
        
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            return Flux.error(new BusinessException("文章标题不能为空"));
        }
        
        log.info("用户 {} 流式生成摘要，标题: {}", userId, request.getTitle());
        return generateService.generateSummaryStream(request);
    }
    
    @PostMapping("/generate/content")
    public ResponseEntity<Result<GenerateResult>> generateContent(@Valid @RequestBody GenerateRequestDTO request) {
        Long userId = SecurityUtils.getCurrentUserId();
        
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BusinessException("文章标题不能为空");
        }
        
        log.info("用户 {} 生成正文，标题: {}", userId, request.getTitle());
        
        GenerateResult result = generateService.generateContent(request);
        return ResponseEntity.ok(Result.success(result));
    }
    
    @PostMapping(value = "/generate/content/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateContentStream(@Valid @RequestBody GenerateRequestDTO request) {
        Long userId = SecurityUtils.getCurrentUserId();
        
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            return Flux.error(new BusinessException("文章标题不能为空"));
        }
        
        log.info("用户 {} 流式生成正文，标题: {}", userId, request.getTitle());
        return generateService.generateContentStream(request);
    }
    
    @PostMapping("/generate/outline")
    public ResponseEntity<Result<GenerateResult>> generateOutline(@Valid @RequestBody GenerateRequestDTO request) {
        Long userId = SecurityUtils.getCurrentUserId();
        
        if (request.getTopic() == null || request.getTopic().isBlank()) {
            throw new BusinessException("主题不能为空");
        }
        
        log.info("用户 {} 生成大纲，主题: {}", userId, request.getTopic());
        
        GenerateResult result = generateService.generateOutline(request);
        return ResponseEntity.ok(Result.success(result));
    }
    
    @PostMapping(value = "/generate/outline/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateOutlineStream(@Valid @RequestBody GenerateRequestDTO request) {
        Long userId = SecurityUtils.getCurrentUserId();
        
        if (request.getTopic() == null || request.getTopic().isBlank()) {
            return Flux.error(new BusinessException("主题不能为空"));
        }
        
        log.info("用户 {} 流式生成大纲，主题: {}", userId, request.getTopic());
        return generateService.generateOutlineStream(request);
    }
    
    @PostMapping("/generate/cover-image")
    public ResponseEntity<Result<GenerateResult>> generateCoverImage(@Valid @RequestBody GenerateRequestDTO request) {
        Long userId = SecurityUtils.getCurrentUserId();
        
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BusinessException("文章标题不能为空");
        }
        
        log.info("用户 {} 生成封面图，标题: {}", userId, request.getTitle());
        
        GenerateResult result = generateService.generateCoverImage(request);
        return ResponseEntity.ok(Result.success(result));
    }
}
