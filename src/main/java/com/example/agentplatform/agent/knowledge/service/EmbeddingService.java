package com.example.agentplatform.agent.knowledge.service;

import java.util.List;

public interface EmbeddingService {
    List<Double> embed(String text);
    List<List<Double>> embedBatch(List<String> texts);
    double cosineSimilarity(List<Double> vectorA, List<Double> vectorB);
}
