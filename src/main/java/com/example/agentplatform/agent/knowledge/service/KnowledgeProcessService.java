package com.example.agentplatform.agent.knowledge.service;

public interface KnowledgeProcessService {
    void processAndEmbedKnowledge(Long knowledgeId);
    void reprocessKnowledge(Long knowledgeId);
}
