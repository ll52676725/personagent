package com.example.agentplatform.agent.knowledge.service;

import com.example.agentplatform.agent.knowledge.dto.KnowledgeQueryDTO;
import com.example.agentplatform.agent.knowledge.dto.KnowledgeQueryResult;
import com.example.agentplatform.agent.knowledge.dto.KnowledgeSearchDTO;
import reactor.core.publisher.Flux;

import java.util.List;

public interface KnowledgeQueryService {
    KnowledgeQueryResult query(KnowledgeQueryDTO dto, Long userId);
    Flux<String> queryStream(KnowledgeQueryDTO dto, Long userId);
    List<KnowledgeQueryResult.SourceReference> search(KnowledgeSearchDTO dto, Long userId);
}
