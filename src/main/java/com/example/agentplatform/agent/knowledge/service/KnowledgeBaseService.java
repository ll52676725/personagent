package com.example.agentplatform.agent.knowledge.service;

import com.example.agentplatform.agent.knowledge.dto.KnowledgeBaseCreateDTO;
import com.example.agentplatform.agent.knowledge.dto.KnowledgeBaseUpdateDTO;
import com.example.agentplatform.agent.knowledge.entity.KnowledgeBase;

import java.util.List;

public interface KnowledgeBaseService {
    KnowledgeBase createKnowledgeBase(Long userId, KnowledgeBaseCreateDTO dto);
    KnowledgeBase updateKnowledgeBase(Long userId, Long baseId, KnowledgeBaseUpdateDTO dto);
    KnowledgeBase getKnowledgeBase(Long userId, Long baseId);
    List<KnowledgeBase> listKnowledgeBases(Long userId);
    void deleteKnowledgeBase(Long userId, Long baseId);
}
