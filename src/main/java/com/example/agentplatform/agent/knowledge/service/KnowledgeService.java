package com.example.agentplatform.agent.knowledge.service;

import com.example.agentplatform.agent.knowledge.dto.KnowledgeCreateDTO;
import com.example.agentplatform.agent.knowledge.dto.KnowledgeUpdateDTO;
import com.example.agentplatform.agent.knowledge.entity.Knowledge;

import java.util.List;

public interface KnowledgeService {
    Knowledge createKnowledge(Long userId, Long baseId, KnowledgeCreateDTO dto);
    Knowledge updateKnowledge(Long userId, Long knowledgeId, KnowledgeUpdateDTO dto);
    Knowledge getKnowledge(Long userId, Long knowledgeId);
    List<Knowledge> listKnowledge(Long userId, Long baseId);
    List<Knowledge> listAllKnowledge(Long userId);
    void deleteKnowledge(Long userId, Long knowledgeId);
    void processKnowledgeChunks(Long knowledgeId);
}
