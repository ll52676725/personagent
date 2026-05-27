package com.example.agentplatform.agent.article.service;

import com.example.agentplatform.agent.article.dto.ArticleCreateDTO;
import com.example.agentplatform.agent.article.dto.ArticleUpdateDTO;
import com.example.agentplatform.agent.article.dto.PublishDTO;
import com.example.agentplatform.agent.article.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ArticleService {
    Article createArticle(Long userId, ArticleCreateDTO dto);
    Article updateArticle(Long userId, Long articleId, ArticleUpdateDTO dto);
    Article getArticle(Long userId, Long articleId);
    Page<Article> listArticles(Long userId, Pageable pageable);
    List<Article> listArticlesByUserId(Long userId);
    void deleteArticle(Long userId, Long articleId);
    Article publishArticle(Long userId, Long articleId, PublishDTO dto);
    Article save(Article article);
}