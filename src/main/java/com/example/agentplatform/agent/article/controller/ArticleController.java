package com.example.agentplatform.agent.article.controller;

import com.example.agentplatform.agent.article.dto.ArticleCreateDTO;
import com.example.agentplatform.agent.article.dto.ArticleUpdateDTO;
import com.example.agentplatform.agent.article.dto.PublishDTO;
import com.example.agentplatform.agent.article.entity.Article;
import com.example.agentplatform.agent.article.service.ArticleService;
import com.example.agentplatform.common.dto.Result;
import com.example.agentplatform.common.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/agent-article/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @PostMapping
    public ResponseEntity<Result<Article>> createArticle(@Valid @RequestBody ArticleCreateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        Article article = articleService.createArticle(userId, dto);
        return ResponseEntity.ok(Result.success("文章创建成功", article));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<Article>> getArticle(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        Article article = articleService.getArticle(userId, id);
        return ResponseEntity.ok(Result.success(article));
    }

    @GetMapping
    public ResponseEntity<Result<List<Article>>> listArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long collectionId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Article> articles = articleService.listArticles(userId, pageable);
        return ResponseEntity.ok(Result.success(articles.getContent()));
    }

    @GetMapping("/collection/{collectionId}")
    public ResponseEntity<Result<List<Article>>> listArticlesByCollection(
            @PathVariable Long collectionId) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Article> articles = articleService.listArticlesByCollectionId(userId, collectionId);
        return ResponseEntity.ok(Result.success(articles));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Result<Article>> updateArticle(
            @PathVariable Long id,
            @RequestBody ArticleUpdateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        Article article = articleService.updateArticle(userId, id, dto);
        return ResponseEntity.ok(Result.success("文章更新成功", article));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Result<Void>> deleteArticle(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        articleService.deleteArticle(userId, id);
        return ResponseEntity.ok(Result.success("文章删除成功", null));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<Result<Map<String, Object>>> publishArticle(
            @PathVariable Long id,
            @RequestBody PublishDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        Article article = articleService.publishArticle(userId, id, dto);

        Map<String, Object> result = new HashMap<>();
        result.put("article_id", article.getId());
        result.put("status", article.getStatus());
        result.put("published_at", article.getPublishedAt());

        return ResponseEntity.ok(Result.success("文章发布成功", result));
    }
}