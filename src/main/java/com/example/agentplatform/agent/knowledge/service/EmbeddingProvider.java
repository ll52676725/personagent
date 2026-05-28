package com.example.agentplatform.agent.knowledge.service;

import java.util.List;

public interface EmbeddingProvider {
    String getName();
    List<Double> embed(String text);
    List<List<Double>> embedBatch(List<String> texts);
}
