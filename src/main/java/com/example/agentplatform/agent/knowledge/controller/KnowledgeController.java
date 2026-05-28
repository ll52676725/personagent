package com.example.agentplatform.agent.knowledge.controller;

import com.example.agentplatform.agent.knowledge.dto.KnowledgeCreateDTO;
import com.example.agentplatform.agent.knowledge.dto.KnowledgeImportDTO;
import com.example.agentplatform.agent.knowledge.dto.KnowledgeUpdateDTO;
import com.example.agentplatform.agent.knowledge.entity.Knowledge;
import com.example.agentplatform.agent.knowledge.service.DocumentParserService;
import com.example.agentplatform.agent.knowledge.service.EmbeddingService;
import com.example.agentplatform.agent.knowledge.service.KnowledgeProcessService;
import com.example.agentplatform.agent.knowledge.service.KnowledgeService;
import com.example.agentplatform.agent.knowledge.service.impl.EmbeddingServiceImpl;
import com.example.agentplatform.common.dto.Result;
import com.example.agentplatform.common.exception.BusinessException;
import com.example.agentplatform.common.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 知识条目管理控制器
 * 提供知识条目的 CRUD 操作、URL 导入、文件导入、重新处理等功能
 * 所有接口均需要用户登录认证
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agent-knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;
    private final KnowledgeProcessService knowledgeProcessService;
    private final DocumentParserService documentParserService;
    private final EmbeddingServiceImpl embeddingService;

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    /**
     * 创建知识条目
     * 创建后自动触发异步向量化处理
     *
     * @param baseId 知识库ID
     * @param dto 知识条目创建请求
     * @return 创建的知识条目
     */
    @PostMapping("/bases/{baseId}/items")
    public ResponseEntity<Result<Knowledge>> createKnowledge(
            @PathVariable Long baseId,
            @Valid @RequestBody KnowledgeCreateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        Knowledge knowledge = knowledgeService.createKnowledge(userId, baseId, dto);
        if (knowledge.getContent() != null && !knowledge.getContent().isBlank()) {
            knowledgeProcessService.processAndEmbedKnowledge(knowledge.getId());
        }
        return ResponseEntity.ok(Result.success("知识条目创建成功", knowledge));
    }

    /**
     * 获取指定知识库下的所有知识条目
     * 按创建时间倒序排列
     *
     * @param baseId 知识库ID
     * @return 知识条目列表
     */
    @GetMapping("/bases/{baseId}/items")
    public ResponseEntity<Result<List<Knowledge>>> listKnowledge(@PathVariable Long baseId) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Knowledge> items = knowledgeService.listKnowledge(userId, baseId);
        return ResponseEntity.ok(Result.success(items));
    }

    /**
     * 获取用户所有知识库的知识条目
     * 按创建时间倒序排列
     *
     * @return 全部知识条目列表
     */
    @GetMapping("/items")
    public ResponseEntity<Result<List<Knowledge>>> listAllKnowledge() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Knowledge> items = knowledgeService.listAllKnowledge(userId);
        return ResponseEntity.ok(Result.success(items));
    }

    /**
     * 获取单个知识条目的详细信息
     *
     * @param id 知识条目ID
     * @return 知识条目详情
     */
    @GetMapping("/items/{id}")
    public ResponseEntity<Result<Knowledge>> getKnowledge(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        Knowledge knowledge = knowledgeService.getKnowledge(userId, id);
        return ResponseEntity.ok(Result.success(knowledge));
    }

    /**
     * 更新知识条目
     * 如果内容发生变化，自动触发重新向量化
     *
     * @param id 知识条目ID
     * @param dto 更新内容
     * @return 更新后的知识条目
     */
    @PutMapping("/items/{id}")
    public ResponseEntity<Result<Knowledge>> updateKnowledge(
            @PathVariable Long id,
            @Valid @RequestBody KnowledgeUpdateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        Knowledge knowledge = knowledgeService.updateKnowledge(userId, id, dto);
        if (dto.getContent() != null) {
            knowledgeProcessService.reprocessKnowledge(id);
        }
        return ResponseEntity.ok(Result.success("知识条目更新成功", knowledge));
    }

    /**
     * 删除知识条目
     * 同时删除关联的向量分片数据
     *
     * @param id 知识条目ID
     * @return 空结果
     */
    @DeleteMapping("/items/{id}")
    public ResponseEntity<Result<Void>> deleteKnowledge(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        knowledgeService.deleteKnowledge(userId, id);
        return ResponseEntity.ok(Result.success("知识条目删除成功", null));
    }

    /**
     * 从 URL 导入网页内容
     * 使用 Jsoup 爬取网页正文，智能选择主要内容区域
     * 支持自定义标题、分类和标签
     *
     * @param dto URL 导入请求
     * @return 导入的知识条目
     */
    @PostMapping("/import/url")
    public ResponseEntity<Result<Knowledge>> importFromUrl(@Valid @RequestBody KnowledgeImportDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        try {
            log.info("用户 {} 开始导入 URL: {}", userId, dto.getUrl());

            org.jsoup.nodes.Document doc = Jsoup.connect(dto.getUrl())
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(15000)
                    .get();

            String title = dto.getTitle() != null ? dto.getTitle() : doc.title();

            String content = doc.select(
                    "article, main, .content, .post-body, #article_content, .article, .post-content"
            ).text();

            if (content == null || content.isBlank() || content.length() < 50) {
                content = doc.body().text();
            }

            content = documentParserService.cleanText(content);

            KnowledgeCreateDTO createDTO = KnowledgeCreateDTO.builder()
                    .title(title)
                    .content(content)
                    .sourceType("url")
                    .sourceUrl(dto.getUrl())
                    .category(dto.getCategory())
                    .tags(dto.getTags())
                    .build();

            Knowledge knowledge = knowledgeService.createKnowledge(userId,
                    dto.getBaseId() != null ? dto.getBaseId() : 1L, createDTO);
            if (knowledge.getContent() != null && !knowledge.getContent().isBlank()) {
                knowledgeProcessService.processAndEmbedKnowledge(knowledge.getId());
            }

            log.info("用户 {} URL 导入成功: {}", userId, knowledge.getId());
            return ResponseEntity.ok(Result.success("URL导入成功", knowledge));
        } catch (IOException e) {
            log.error("URL导入失败: {}", dto.getUrl(), e);
            throw new BusinessException("URL导入失败：" + e.getMessage());
        }
    }

    /**
     * 从文件导入知识内容
     * 支持格式：PDF, Word(DOC/DOCX), Excel, PPT, TXT, MD, HTML, RTF 等
     * 使用 Apache Tika 进行文档解析，自动提取文本并清洗
     *
     * @param file 上传的文件
     * @param baseId 目标知识库ID
     * @param category 分类（可选）
     * @param tags 标签（可选，逗号分隔）
     * @return 导入的知识条目
     */
    @PostMapping("/import/file")
    public ResponseEntity<Result<Knowledge>> importFromFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("baseId") Long baseId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tags) {

        Long userId = SecurityUtils.getCurrentUserId();
        String filename = file.getOriginalFilename();

        log.info("用户 {} 开始导入文件: {}, 大小: {} bytes", userId, filename, file.getSize());

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("文件大小超过限制，最大支持 50MB");
        }

        if (!documentParserService.isSupported(filename)) {
            throw new BusinessException("不支持的文件格式，支持格式：PDF, Word, Excel, PPT, TXT, MD, HTML 等");
        }

        try {
            String content = documentParserService.parseFile(file);

            if (content == null || content.isBlank()) {
                throw new BusinessException("无法从文件中提取文本内容，文件可能已损坏或为空");
            }

            String title = filename != null ? filename.replaceAll("\\.[^.]+$", "") : "导入文件";
            String extension = filename != null ? getFileExtension(filename) : "";

            KnowledgeCreateDTO createDTO = KnowledgeCreateDTO.builder()
                    .title(title)
                    .content(content)
                    .sourceType("file")
                    .sourceUrl(filename)
                    .fileName(filename)
                    .fileType(extension)
                    .fileSize(file.getSize())
                    .category(category)
                    .tags(tags)
                    .build();

            Knowledge knowledge = knowledgeService.createKnowledge(userId, baseId, createDTO);
            if (knowledge.getContent() != null && !knowledge.getContent().isBlank()) {
                knowledgeProcessService.processAndEmbedKnowledge(knowledge.getId());
            }

            log.info("用户 {} 文件导入成功: {}, 提取文本长度: {} 字符",
                    userId, knowledge.getId(), content.length());

            return ResponseEntity.ok(Result.success("文件导入成功", knowledge));

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件导入失败: {}", filename, e);
            throw new BusinessException("文件导入失败：" + e.getMessage());
        }
    }

    @PostMapping("/import/files")
    public ResponseEntity<Result<List<Knowledge>>> importFromFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("baseId") Long baseId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tags) {

        Long userId = SecurityUtils.getCurrentUserId();
        log.info("用户 {} 批量导入文件，数量: {}", userId, files.length);

        List<Knowledge> results = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (MultipartFile file : files) {
            String filename = file.getOriginalFilename();

            try {
                if (file.getSize() > MAX_FILE_SIZE) {
                    errors.add(filename + ": 文件大小超过50MB限制");
                    continue;
                }

                if (!documentParserService.isSupported(filename)) {
                    errors.add(filename + ": 不支持的文件格式");
                    continue;
                }

                String content = documentParserService.parseFile(file);

                if (content == null || content.isBlank()) {
                    errors.add(filename + ": 无法提取文本内容");
                    continue;
                }

                String title = filename != null ? filename.replaceAll("\\.[^.]+$", "") : "导入文件";
                String extension = filename != null ? getFileExtension(filename) : "";

                KnowledgeCreateDTO createDTO = KnowledgeCreateDTO.builder()
                        .title(title)
                        .content(content)
                        .sourceType("file")
                        .sourceUrl(filename)
                        .fileName(filename)
                        .fileType(extension)
                        .fileSize(file.getSize())
                        .category(category)
                        .tags(tags)
                        .build();

                Knowledge knowledge = knowledgeService.createKnowledge(userId, baseId, createDTO);
                if (knowledge.getContent() != null && !knowledge.getContent().isBlank()) {
                    knowledgeProcessService.processAndEmbedKnowledge(knowledge.getId());
                }
                results.add(knowledge);

            } catch (Exception e) {
                log.error("批量导入文件失败: {}", filename, e);
                errors.add(filename + ": " + e.getMessage());
            }
        }

        String message = String.format("成功导入 %d 个文件", results.size());
        if (!errors.isEmpty()) {
            message += "，" + errors.size() + " 个文件失败：" + String.join("; ", errors);
        }

        return ResponseEntity.ok(Result.success(message, results));
    }

    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex + 1).toLowerCase();
        }
        return "";
    }

    /**
     * 重新处理知识条目
     * 触发异步的分片和向量化处理
     * 通常用于内容修改后或之前处理失败时
     *
     * @param id 知识条目ID
     * @return 空结果
     */
    @PostMapping("/items/{id}/reprocess")
    public ResponseEntity<Result<Void>> reprocessKnowledge(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        knowledgeService.getKnowledge(userId, id);
        knowledgeProcessService.reprocessKnowledge(id);
        return ResponseEntity.ok(Result.success("重新处理已启动", null));
    }

    @GetMapping("/embedding/providers")
    public ResponseEntity<Result<java.util.Map<String, Object>>> getEmbeddingProviders() {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("available", embeddingService.getAvailableProviders());
        result.put("active", embeddingService.getAvailableProviders().isEmpty() ? "none" : "unknown");
        return ResponseEntity.ok(Result.success(result));
    }

    @PostMapping("/embedding/providers/{name}/switch")
    public ResponseEntity<Result<String>> switchEmbeddingProvider(@PathVariable String name) {
        embeddingService.switchProvider(name);
        return ResponseEntity.ok(Result.success("已切换到 Embedding Provider: " + name, name));
    }

    @PostMapping("/embedding/test")
    public ResponseEntity<Result<java.util.Map<String, Object>>> testEmbedding(@RequestBody java.util.Map<String, String> body) {
        String text = body.getOrDefault("text", "测试文本");
        long start = System.currentTimeMillis();
        java.util.List<Double> embedding = embeddingService.embed(text);
        long cost = System.currentTimeMillis() - start;
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("dimensions", embedding.size());
        result.put("costMs", cost);
        result.put("previewFirst5", embedding.subList(0, Math.min(5, embedding.size())));
        return ResponseEntity.ok(Result.success("Embedding 测试成功", result));
    }
}
