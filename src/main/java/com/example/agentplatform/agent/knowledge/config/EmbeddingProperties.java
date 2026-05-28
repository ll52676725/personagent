package com.example.agentplatform.agent.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "agent.platform.knowledge.embedding")
public class EmbeddingProperties {

    private String provider = "openai";

    private int dimensions = 1024;

    private Map<String, ProviderConfig> providers = new HashMap<>();

    @Data
    public static class ProviderConfig {
        private String baseUrl;
        private String apiKey;
        private String model;
        private int dimensions = 1024;
    }
}
