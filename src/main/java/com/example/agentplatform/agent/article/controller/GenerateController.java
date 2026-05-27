package com.example.agentplatform.agent.article.controller;

import com.example.agentplatform.agent.article.config.PlatformType;
import com.example.agentplatform.agent.article.dto.GenerateRequestDTO;
import com.example.agentplatform.agent.article.dto.GenerateResult;
import com.example.agentplatform.agent.article.dto.SectionImageGenerateDTO;
import com.example.agentplatform.agent.article.dto.SectionImageResult;
import com.example.agentplatform.agent.article.service.ArticleGenerationService;
import com.example.agentplatform.agent.article.service.GenerateService;
import com.example.agentplatform.agent.article.service.ImageGenerationService;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/agent-article")
@RequiredArgsConstructor
public class GenerateController {
    
    private final GenerateService generateService;
    private final ArticleGenerationService articleGenerationService;
    private final ImageGenerationService imageGenerationService;
    
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

    @PostMapping("/generate/content/platform/{platform}")
    public ResponseEntity<Result<GenerateResult>> generateContentForPlatform(
            @Valid @RequestBody GenerateRequestDTO request,
            @PathVariable String platform) {
        Long userId = SecurityUtils.getCurrentUserId();
        
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BusinessException("文章标题不能为空");
        }
        
        PlatformType platformType = PlatformType.fromCode(platform);
        log.info("用户 {} 为平台 {} 生成正文，标题: {}", userId, platform, request.getTitle());
        
        GenerateResult result = articleGenerationService.generateContentForPlatform(request, platformType);
        return ResponseEntity.ok(Result.success(result));
    }

    @PostMapping(value = "/generate/content/platform/{platform}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateContentStreamForPlatform(
            @Valid @RequestBody GenerateRequestDTO request,
            @PathVariable String platform) {
        Long userId = SecurityUtils.getCurrentUserId();
        
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            return Flux.error(new BusinessException("文章标题不能为空"));
        }
        
        PlatformType platformType = PlatformType.fromCode(platform);
        log.info("用户 {} 为平台 {} 流式生成正文，标题: {}", userId, platform, request.getTitle());
        
        return articleGenerationService.generateContentStreamForPlatform(request, platformType);
    }

    @PostMapping("/generate/full-article")
    public ResponseEntity<Result<GenerateResult>> generateFullArticle(
            @Valid @RequestBody GenerateRequestDTO request,
            @RequestParam(required = false) List<String> platforms) {
        Long userId = SecurityUtils.getCurrentUserId();
        
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new BusinessException("文章标题不能为空");
        }
        
        List<PlatformType> platformTypes = platforms != null ?
                platforms.stream().map(PlatformType::fromCode).toList() :
                List.of(PlatformType.CSDN, PlatformType.TOUTIAO, PlatformType.ZHIHU);
        
        log.info("用户 {} 生成完整文章，标题: {}, 目标平台: {}", userId, request.getTitle(), platforms);
        
        GenerateResult result = articleGenerationService.generateFullArticle(request, platformTypes);
        return ResponseEntity.ok(Result.success(result));
    }

    @GetMapping("/generate/title/platform/{platform}")
    public ResponseEntity<Result<String>> generateTitleForPlatform(
            @RequestParam String topic,
            @RequestParam(required = false) List<String> keywords,
            @PathVariable String platform) {
        Long userId = SecurityUtils.getCurrentUserId();
        
        if (topic == null || topic.isBlank()) {
            throw new BusinessException("主题不能为空");
        }
        
        PlatformType platformType = PlatformType.fromCode(platform);
        log.info("用户 {} 为平台 {} 生成标题，主题: {}", userId, platform, topic);
        
        String title = articleGenerationService.generateTitleForPlatform(topic, keywords, platformType);
        return ResponseEntity.ok(Result.success(title));
    }

    @GetMapping("/generate/summary/platform/{platform}")
    public ResponseEntity<Result<String>> generateSummaryForPlatform(
            @RequestParam String title,
            @RequestParam(required = false) String content,
            @PathVariable String platform) {
        Long userId = SecurityUtils.getCurrentUserId();
        
        if (title == null || title.isBlank()) {
            throw new BusinessException("标题不能为空");
        }
        
        PlatformType platformType = PlatformType.fromCode(platform);
        log.info("用户 {} 为平台 {} 生成摘要，标题: {}", userId, platform, title);
        
        String summary = articleGenerationService.generateSummaryForPlatform(title, content, platformType);
        return ResponseEntity.ok(Result.success(summary));
    }

    @GetMapping("/platforms")
    public ResponseEntity<Result<List<PlatformType>>> getSupportedPlatforms() {
        return ResponseEntity.ok(Result.success(List.of(PlatformType.values())));
    }

    @PostMapping("/generate/section-images")
    public ResponseEntity<Result<Map<String, Object>>> generateSectionImages(
            @Valid @RequestBody SectionImageGenerateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();

        log.info("用户 {} 为文章 '{}' 生成阶段性插图", userId, dto.getArticleTitle());

        PlatformType platformType = dto.getPlatform() != null ?
                PlatformType.fromCode(dto.getPlatform()) : PlatformType.CSDN;

        List<SectionImageResult> images = imageGenerationService.generateSectionImages(
                dto.getContent(), dto.getArticleTitle(), platformType
        );

        Map<String, Object> result = new HashMap<>();
        result.put("images", images);
        result.put("imageCount", images.size());
        result.put("successCount", images.stream().filter(SectionImageResult::isSuccess).count());

        return ResponseEntity.ok(Result.success("阶段性插图生成成功", result));
    }

    @PostMapping("/generate/insert-images")
    public ResponseEntity<Result<Map<String, Object>>> generateAndInsertImages(
            @Valid @RequestBody SectionImageGenerateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();

        log.info("用户 {} 为文章 '{}' 生成并插入插图", userId, dto.getArticleTitle());

        PlatformType platformType = dto.getPlatform() != null ?
                PlatformType.fromCode(dto.getPlatform()) : PlatformType.CSDN;

        String contentWithImages = imageGenerationService.insertImagesIntoMarkdown(
                dto.getContent(), dto.getArticleTitle(), platformType
        );

        List<SectionImageResult> images = imageGenerationService.generateSectionImages(
                dto.getContent(), dto.getArticleTitle(), platformType
        );

        Map<String, Object> result = new HashMap<>();
        result.put("content", contentWithImages);
        result.put("images", images);
        result.put("imageCount", images.size());
        result.put("successCount", images.stream().filter(SectionImageResult::isSuccess).count());

        return ResponseEntity.ok(Result.success("插图生成并插入成功", result));
    }

    @GetMapping("/generate/image-cache")
    public ResponseEntity<Result<Map<String, Object>>> getImageCacheInfo() {
        Long userId = SecurityUtils.getCurrentUserId();

        Map<String, Object> result = new HashMap<>();
        result.put("cacheSize", imageGenerationService.getCacheSize());

        return ResponseEntity.ok(Result.success(result));
    }

    @DeleteMapping("/generate/image-cache")
    public ResponseEntity<Result<Void>> clearImageCache() {
        Long userId = SecurityUtils.getCurrentUserId();

        imageGenerationService.clearCache();

        return ResponseEntity.ok(Result.success("图片缓存已清空", null));
    }
}
