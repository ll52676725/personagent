package com.example.agentplatform.agent.article.service.impl;

import com.example.agentplatform.agent.article.dto.ArticleCreateDTO;
import com.example.agentplatform.agent.article.dto.ArticleUpdateDTO;
import com.example.agentplatform.agent.article.dto.PublishDTO;
import com.example.agentplatform.agent.article.entity.Article;
import com.example.agentplatform.common.exception.BusinessException;
import com.example.agentplatform.agent.article.repository.ArticleRepository;
import com.example.agentplatform.agent.article.service.ArticleService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {
    
    private final ArticleRepository articleRepository;
    private final ObjectMapper objectMapper;
    
    @Override
    @Transactional
    public Article createArticle(Long userId, ArticleCreateDTO dto) {
        Article article = Article.builder()
                .userId(userId)
                .title(dto.getTitle())
                .summary(dto.getSummary())
                .content(dto.getContent())
                .coverImage(dto.getCoverImage())
                .tags(dto.getTags() != null ? String.join(",", dto.getTags()) : null)
                .status(0)
                .build();
        
        Article saved = articleRepository.save(article);
        log.info("用户 {} 创建文章: {}", userId, saved.getTitle());
        return saved;
    }
    
    @Override
    @Transactional
    public Article updateArticle(Long userId, Long articleId, ArticleUpdateDTO dto) {
        Article article = articleRepository.findByIdAndUserId(articleId, userId)
                .orElseThrow(() -> new BusinessException("文章不存在或无权操作"));
        
        if (dto.getTitle() != null) {
            article.setTitle(dto.getTitle());
        }
        if (dto.getSummary() != null) {
            article.setSummary(dto.getSummary());
        }
        if (dto.getContent() != null) {
            article.setContent(dto.getContent());
        }
        if (dto.getCoverImage() != null) {
            article.setCoverImage(dto.getCoverImage());
        }
        if (dto.getTags() != null) {
            article.setTags(String.join(",", dto.getTags()));
        }
        if (dto.getStatus() != null) {
            article.setStatus(dto.getStatus());
        }
        
        Article updated = articleRepository.save(article);
        log.info("用户 {} 更新文章: {}", userId, updated.getId());
        return updated;
    }
    
    @Override
    public Article getArticle(Long userId, Long articleId) {
        return articleRepository.findByIdAndUserId(articleId, userId)
                .orElseThrow(() -> new BusinessException("文章不存在或无权操作"));
    }
    
    @Override
    public Page<Article> listArticles(Long userId, Pageable pageable) {
        return articleRepository.findByUserId(userId, pageable);
    }
    
    @Override
    public List<Article> listArticlesByUserId(Long userId) {
        return articleRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    @Override
    @Transactional
    public void deleteArticle(Long userId, Long articleId) {
        Article article = articleRepository.findByIdAndUserId(articleId, userId)
                .orElseThrow(() -> new BusinessException("文章不存在或无权操作"));
        
        articleRepository.delete(article);
        log.info("用户 {} 删除文章: {}", userId, articleId);
    }
    
    @Override
    @Transactional
    public Article save(Article article) {
        return articleRepository.save(article);
    }

    @Override
    @Transactional
    public Article publishArticle(Long userId, Long articleId, PublishDTO dto) {
        Article article = articleRepository.findByIdAndUserId(articleId, userId)
                .orElseThrow(() -> new BusinessException("文章不存在或无权操作"));
        
        List<Map<String, Object>> results = new ArrayList<>();
        if (dto.getPlatforms() != null) {
            for (String platform : dto.getPlatforms()) {
                Map<String, Object> result = new HashMap<>();
                result.put("platform", platform);
                result.put("success", true);
                result.put("url", "https://example.com/article/" + articleId);
                result.put("message", "发布成功");
                results.add(result);
            }
        }
        
        try {
            article.setPublishLinks(objectMapper.writeValueAsString(results));
        } catch (JsonProcessingException e) {
            log.error("序列化发布结果失败", e);
        }
        
        article.setStatus(1);
        article.setPublishedAt(LocalDateTime.now());
        
        Article updated = articleRepository.save(article);
        log.info("用户 {} 发布文章: {}", userId, articleId);
        return updated;
    }
}