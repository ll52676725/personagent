package com.example.agentplatform.agent.article.repository;

import com.example.agentplatform.agent.article.entity.Article;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    Page<Article> findByUserId(Long userId, Pageable pageable);
    Page<Article> findByUserIdAndStatus(Long userId, Integer status, Pageable pageable);
    Optional<Article> findByIdAndUserId(Long id, Long userId);
    List<Article> findByUserIdOrderByCreatedAtDesc(Long userId);
}