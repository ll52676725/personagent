package com.example.agentplatform.agent.article.controller;

import com.example.agentplatform.agent.article.dto.*;
import com.example.agentplatform.agent.article.entity.Article;
import com.example.agentplatform.agent.article.entity.Collection;
import com.example.agentplatform.agent.article.service.CollectionService;
import com.example.agentplatform.common.dto.Result;
import com.example.agentplatform.common.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/agent-article/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    @PostMapping
    public ResponseEntity<Result<Collection>> createCollection(@Valid @RequestBody CollectionCreateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        Collection collection = collectionService.createCollection(userId, dto);
        return ResponseEntity.ok(Result.success("合集创建成功", collection));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<Collection>> getCollection(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        Collection collection = collectionService.getCollection(userId, id);
        return ResponseEntity.ok(Result.success(collection));
    }

    @GetMapping
    public ResponseEntity<Result<List<Collection>>> listCollections() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Collection> collections = collectionService.listCollections(userId);
        return ResponseEntity.ok(Result.success(collections));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Result<Collection>> updateCollection(
            @PathVariable Long id,
            @RequestBody CollectionUpdateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        Collection collection = collectionService.updateCollection(userId, id, dto);
        return ResponseEntity.ok(Result.success("合集更新成功", collection));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Result<Void>> deleteCollection(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        collectionService.deleteCollection(userId, id);
        return ResponseEntity.ok(Result.success("合集删除成功", null));
    }

    @PostMapping("/{id}/generate-outlines")
    public ResponseEntity<Result<CollectionOutlineDTO>> generateCollectionOutlines(
            @PathVariable Long id,
            @RequestBody(required = false) GenerateCollectionOutlineDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();

        GenerateCollectionOutlineDTO request = dto != null ? dto : new GenerateCollectionOutlineDTO();
        request.setCollectionId(id);

        CollectionOutlineDTO outlines = collectionService.generateCollectionOutlines(userId, request);
        return ResponseEntity.ok(Result.success("大纲生成成功", outlines));
    }

    @PostMapping("/{id}/outlines")
    public ResponseEntity<Result<Collection>> saveCollectionOutlines(
            @PathVariable Long id,
            @RequestBody CollectionOutlineDTO outlines) {
        Long userId = SecurityUtils.getCurrentUserId();
        Collection collection = collectionService.saveCollectionOutlines(userId, id, outlines);
        return ResponseEntity.ok(Result.success("大纲保存成功", collection));
    }

    @PostMapping("/{id}/articles")
    public ResponseEntity<Result<Article>> generateArticleFromOutline(
            @PathVariable Long id,
            @Valid @RequestBody GenerateArticleFromOutlineDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        dto.setCollectionId(id);
        Article article = collectionService.generateArticleFromOutline(userId, dto);
        return ResponseEntity.ok(Result.success("文章生成成功", article));
    }

    @PostMapping("/{id}/articles/all")
    public ResponseEntity<Result<Map<String, Object>>> generateAllArticles(
            @PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Article> articles = collectionService.generateAllArticlesFromCollection(userId, id);

        Map<String, Object> result = new HashMap<>();
        result.put("collectionId", id);
        result.put("articleCount", articles.size());
        result.put("articles", articles);

        return ResponseEntity.ok(Result.success("批量文章生成成功", result));
    }
}