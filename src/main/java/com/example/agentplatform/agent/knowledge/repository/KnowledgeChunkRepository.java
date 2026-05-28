package com.example.agentplatform.agent.knowledge.repository;

import com.example.agentplatform.agent.knowledge.entity.KnowledgeChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, Long> {
    List<KnowledgeChunk> findByKnowledgeIdOrderByChunkIndex(Long knowledgeId);
    List<KnowledgeChunk> findByBaseId(Long baseId);
    void deleteByKnowledgeId(Long knowledgeId);
    long countByBaseId(Long baseId);
}
