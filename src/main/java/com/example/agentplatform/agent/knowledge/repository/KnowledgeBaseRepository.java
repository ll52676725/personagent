package com.example.agentplatform.agent.knowledge.repository;

import com.example.agentplatform.agent.knowledge.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {
    List<KnowledgeBase> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<KnowledgeBase> findByIdAndUserId(Long id, Long userId);
    void deleteByIdAndUserId(Long id, Long userId);
}
