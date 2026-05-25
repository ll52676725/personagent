package com.example.agentplatform.agent.article.service;

import com.example.agentplatform.agent.article.dto.GenerateRequestDTO;
import com.example.agentplatform.agent.article.dto.GenerateResult;

public interface GenerateService {
    GenerateResult generateTitle(GenerateRequestDTO request);
    GenerateResult generateSummary(GenerateRequestDTO request);
    GenerateResult generateContent(GenerateRequestDTO request);
}