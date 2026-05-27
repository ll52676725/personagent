package com.example.agentplatform.agent.article.service.impl;

import com.example.agentplatform.agent.article.dto.*;
import com.example.agentplatform.agent.article.entity.Article;
import com.example.agentplatform.agent.article.entity.Collection;
import com.example.agentplatform.agent.article.repository.ArticleRepository;
import com.example.agentplatform.agent.article.repository.CollectionRepository;
import com.example.agentplatform.agent.article.service.CollectionService;
import com.example.agentplatform.agent.article.service.GenerateService;
import com.example.agentplatform.common.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionServiceImpl implements CollectionService {

    private final CollectionRepository collectionRepository;
    private final ArticleRepository articleRepository;
    private final GenerateService generateService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Collection createCollection(Long userId, CollectionCreateDTO dto) {
        Collection collection = Collection.builder()
                .userId(userId)
                .title(dto.getTitle())
                .coverImage(dto.getCoverImage())
                .description(dto.getDescription())
                .build();

        Collection saved = collectionRepository.save(collection);
        log.info("用户 {} 创建合集: {}", userId, saved.getTitle());
        return saved;
    }

    @Override
    @Transactional
    public Collection updateCollection(Long userId, Long collectionId, CollectionUpdateDTO dto) {
        Collection collection = collectionRepository.findByIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new BusinessException("合集不存在或无权操作"));

        if (dto.getTitle() != null) {
            collection.setTitle(dto.getTitle());
        }
        if (dto.getCoverImage() != null) {
            collection.setCoverImage(dto.getCoverImage());
        }
        if (dto.getDescription() != null) {
            collection.setDescription(dto.getDescription());
        }
        if (dto.getStatus() != null) {
            collection.setStatus(dto.getStatus());
        }

        Collection updated = collectionRepository.save(collection);
        log.info("用户 {} 更新合集: {}", userId, updated.getId());
        return updated;
    }

    @Override
    public Collection getCollection(Long userId, Long collectionId) {
        return collectionRepository.findByIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new BusinessException("合集不存在或无权操作"));
    }

    @Override
    public List<Collection> listCollections(Long userId) {
        return collectionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional
    public void deleteCollection(Long userId, Long collectionId) {
        Collection collection = collectionRepository.findByIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new BusinessException("合集不存在或无权操作"));

        collectionRepository.delete(collection);
        log.info("用户 {} 删除合集: {}", userId, collectionId);
    }

    @Override
    @Transactional
    public CollectionOutlineDTO generateCollectionOutlines(Long userId, GenerateCollectionOutlineDTO dto) {
        Collection collection = collectionRepository.findByIdAndUserId(dto.getCollectionId(), userId)
                .orElseThrow(() -> new BusinessException("合集不存在或无权操作"));

        String description = collection.getDescription();
        if (description == null || description.isBlank()) {
            throw new BusinessException("合集描述不能为空，请先完善合集描述");
        }

        String topic = dto.getTopic() != null ? dto.getTopic() : collection.getTitle();
        List<String> keywords = dto.getKeywords();
        int articleCount = dto.getArticleCount() != null ? dto.getArticleCount() : 5;

        String systemPrompt = """
                你是一个专业的技术专栏策划师，擅长根据主题描述策划系列文章大纲。
                
                请根据提供的合集描述，策划一个完整的文章专栏大纲，包含多篇文章的标题和要点。
                
                输出要求（严格JSON格式）：
                {
                    "title": "专栏总标题",
                    "summary": "专栏整体介绍",
                    "outlines": [
                        {
                            "title": "文章标题",
                            "summary": "文章摘要",
                            "keyPoints": "文章核心要点，用换行分隔",
                            "order": 1
                        }
                    ]
                }
                """;

        String userPrompt = String.format("""
                请根据以下合集描述策划专栏大纲（生成 %d 篇文章）：
                
                合集标题：%s
                合集描述：%s
                关键词：%s
                
                要求：
                1. 文章之间要有逻辑递进关系
                2. 每篇文章聚焦一个具体主题
                3. 覆盖从入门到进阶的完整学习路径
                """, articleCount, topic, description, keywords != null ? String.join(", ", keywords) : "");

        GenerateResult outlineResult;
        try {
            outlineResult = generateService.generateWithPrompt(systemPrompt, userPrompt, "collection-outline");
        } catch (Exception e) {
            log.error("生成合集大纲失败，使用降级模式", e);
            outlineResult = fallbackGenerateCollectionOutlines(topic, description, articleCount);
        }

        CollectionOutlineDTO outlines = parseCollectionOutlines(outlineResult.getContent(), collection.getId());

        try {
            collection.setOutlines(objectMapper.writeValueAsString(outlines));
            collection.setArticleCount(outlines.getOutlines().size());
            collectionRepository.save(collection);
        } catch (JsonProcessingException e) {
            log.error("保存合集大纲失败", e);
        }

        log.info("用户 {} 为合集 {} 生成了 {} 个文章大纲", userId, collection.getId(), outlines.getOutlines().size());
        return outlines;
    }

    @Override
    @Transactional
    public Collection saveCollectionOutlines(Long userId, Long collectionId, CollectionOutlineDTO outlines) {
        Collection collection = collectionRepository.findByIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new BusinessException("合集不存在或无权操作"));

        try {
            collection.setOutlines(objectMapper.writeValueAsString(outlines));
            collection.setArticleCount(outlines.getOutlines() != null ? outlines.getOutlines().size() : 0);
            return collectionRepository.save(collection);
        } catch (JsonProcessingException e) {
            log.error("保存合集大纲失败", e);
            throw new BusinessException("保存合集大纲失败");
        }
    }

    @Override
    @Transactional
    public Article generateArticleFromOutline(Long userId, GenerateArticleFromOutlineDTO dto) {
        Collection collection = collectionRepository.findByIdAndUserId(dto.getCollectionId(), userId)
                .orElseThrow(() -> new BusinessException("合集不存在或无权操作"));

        if (collection.getOutlines() == null) {
            throw new BusinessException("合集尚未生成大纲，请先生成大纲");
        }

        CollectionOutlineDTO outlines;
        try {
            outlines = objectMapper.readValue(collection.getOutlines(), CollectionOutlineDTO.class);
        } catch (JsonProcessingException e) {
            log.error("解析合集大纲失败", e);
            throw new BusinessException("解析合集大纲失败");
        }

        if (dto.getOutlineIndex() < 0 || dto.getOutlineIndex() >= outlines.getOutlines().size()) {
            throw new BusinessException("大纲序号无效");
        }

        CollectionOutlineDTO.OutlineItem outlineItem = outlines.getOutlines().get(dto.getOutlineIndex());

        String articleTitle = outlineItem.getTitle();
        String articleSummary = outlineItem.getSummary();
        String outlineContent = outlineItem.getKeyPoints();

        GenerateRequestDTO contentRequest = GenerateRequestDTO.builder()
                .title(articleTitle)
                .outline(outlineContent)
                .model(dto.getModel())
                .style(dto.getStyle())
                .build();

        GenerateResult contentResult = generateService.generateContent(contentRequest);

        Article article = Article.builder()
                .userId(userId)
                .title(articleTitle)
                .summary(articleSummary)
                .content(contentResult.getContent())
                .collectionId(collection.getId())
                .status(0)
                .build();

        Article saved = articleRepository.save(article);
        log.info("用户 {} 根据大纲生成文章: {} -> {}", userId, dto.getOutlineIndex(), saved.getTitle());
        return saved;
    }

    @Override
    @Transactional
    public List<Article> generateAllArticlesFromCollection(Long userId, Long collectionId) {
        Collection collection = collectionRepository.findByIdAndUserId(collectionId, userId)
                .orElseThrow(() -> new BusinessException("合集不存在或无权操作"));

        if (collection.getOutlines() == null) {
            throw new BusinessException("合集尚未生成大纲，请先生成大纲");
        }

        CollectionOutlineDTO outlines;
        try {
            outlines = objectMapper.readValue(collection.getOutlines(), CollectionOutlineDTO.class);
        } catch (JsonProcessingException e) {
            log.error("解析合集大纲失败", e);
            throw new BusinessException("解析合集大纲失败");
        }

        List<Article> generatedArticles = new ArrayList<>();
        for (int i = 0; i < outlines.getOutlines().size(); i++) {
            CollectionOutlineDTO.OutlineItem item = outlines.getOutlines().get(i);

            GenerateRequestDTO contentRequest = GenerateRequestDTO.builder()
                    .title(item.getTitle())
                    .outline(item.getKeyPoints())
                    .build();

            GenerateResult contentResult = generateService.generateContent(contentRequest);

            Article article = Article.builder()
                    .userId(userId)
                    .title(item.getTitle())
                    .summary(item.getSummary())
                    .content(contentResult.getContent())
                    .collectionId(collection.getId())
                    .status(0)
                    .build();

            generatedArticles.add(articleRepository.save(article));
            log.info("生成文章 {}: {}", i + 1, article.getTitle());
        }

        log.info("用户 {} 从合集 {} 生成了 {} 篇文章", userId, collectionId, generatedArticles.size());
        return generatedArticles;
    }

    private CollectionOutlineDTO parseCollectionOutlines(String jsonContent, Long collectionId) {
        try {
            CollectionOutlineDTO result = objectMapper.readValue(jsonContent, CollectionOutlineDTO.class);
            if (result.getOutlines() != null && !result.getOutlines().isEmpty()) {
                result.setCollectionId(collectionId);
                return result;
            }
        } catch (JsonProcessingException e) {
            log.warn("解析JSON格式大纲失败，尝试降级解析: {}", jsonContent);
        }
        return parseCollectionOutlinesFallback(jsonContent, collectionId);
    }

    private CollectionOutlineDTO parseCollectionOutlinesFallback(String content, Long collectionId) {
        CollectionOutlineDTO result = new CollectionOutlineDTO();
        result.setCollectionId(collectionId);
        result.setTitle("系列文章专栏");
        result.setSummary("根据合集描述生成的系列文章大纲");

        List<CollectionOutlineDTO.OutlineItem> items = new ArrayList<>();
        String[] lines = content.split("\n");
        int order = 1;

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            line = line.replaceAll("^\\d+[.、)\\s]+", "");
            if (line.length() > 5) {
                CollectionOutlineDTO.OutlineItem item = CollectionOutlineDTO.OutlineItem.builder()
                        .title(line)
                        .summary("关于" + line + "的详细技术文章")
                        .keyPoints("1. 核心概念\n2. 实践应用\n3. 最佳实践")
                        .order(order++)
                        .build();
                items.add(item);
            }
        }

        if (items.isEmpty()) {
            items.add(CollectionOutlineDTO.OutlineItem.builder()
                    .title("技术文章")
                    .summary("技术文章摘要")
                    .keyPoints("核心要点")
                    .order(1)
                    .build());
        }

        result.setOutlines(items);
        return result;
    }

    private GenerateResult fallbackGenerateCollectionOutlines(String topic, String description, int articleCount) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format(
            """
            {
                "title": "%s系列专栏",
                "summary": "深入探讨%s相关技术，从入门到精通的完整学习路径",
                "outlines": [
            """, topic, topic));

        for (int i = 1; i <= articleCount; i++) {
            String title = switch (i) {
                case 1 -> String.format("%s入门指南：从零开始学习%s", topic, topic);
                case 2 -> String.format("深入理解%s核心概念与原理", topic);
                case 3 -> String.format("%s实战：典型应用场景解析", topic);
                case 4 -> String.format("%s进阶：性能优化与最佳实践", topic);
                case 5 -> String.format("%s避坑指南：常见问题与解决方案", topic);
                default -> String.format("%s专题%d：深入探讨", topic, i);
            };

            String keyPoints = String.format("1. %s核心概念\n2. 实践案例分析\n3. 注意事项与最佳实践", topic);
            String summary = String.format("本文深入探讨%s相关内容，帮助读者全面理解并掌握相关技术。", topic);

            sb.append(String.format(
                """
                    {
                        "title": "%s",
                        "summary": "%s",
                        "keyPoints": "%s",
                        "order": %d
                    }%s
                """, title, summary, keyPoints, i, i < articleCount ? "," : ""));
        }

        sb.append("""
                ]
            }""");

        return GenerateResult.fromContent("collection-outline", sb.toString());
    }
}