package com.example.agentplatform.agent.article.service;

import com.example.agentplatform.agent.article.dto.GenerateRequestDTO;
import com.example.agentplatform.agent.article.dto.GenerateResult;
import reactor.core.publisher.Flux;

public interface GenerateService {
    GenerateResult generateTitle(GenerateRequestDTO request);
    GenerateResult generateSummary(GenerateRequestDTO request);
    GenerateResult generateContent(GenerateRequestDTO request);
    GenerateResult generateOutline(GenerateRequestDTO request);
    GenerateResult generateCoverImage(GenerateRequestDTO request);
    
    Flux<String> generateTitleStream(GenerateRequestDTO request);
    Flux<String> generateSummaryStream(GenerateRequestDTO request);
    Flux<String> generateContentStream(GenerateRequestDTO request);
    Flux<String> generateOutlineStream(GenerateRequestDTO request);
}