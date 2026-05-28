package com.example.agentplatform.agent.knowledge.repository;

import com.example.agentplatform.agent.knowledge.entity.Knowledge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeRepository extends JpaRepository<Knowledge, Long> {
    List<Knowledge> findByBaseIdAndUserIdOrderByCreatedAtDesc(Long baseId, Long userId);
    List<Knowledge> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Knowledge> findByIdAndUserId(Long id, Long userId);
    void deleteByIdAndUserId(Long id, Long userId);
    void deleteByBaseIdAndUserId(Long baseId, Long userId);
    long countByBaseIdAndUserId(Long baseId, Long userId);
}
