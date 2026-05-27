package com.example.agentplatform.agent.article.repository;

import com.example.agentplatform.agent.article.entity.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CollectionRepository extends JpaRepository<Collection, Long> {
    List<Collection> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Collection> findByIdAndUserId(Long id, Long userId);

    void deleteByIdAndUserId(Long id, Long userId);
}