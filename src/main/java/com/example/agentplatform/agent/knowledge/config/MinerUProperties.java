package com.example.agentplatform.agent.knowledge.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "agent.platform.knowledge.mineru")
public class MinerUProperties {

    private boolean enabled = true;

    private String apiBaseUrl = "https://mineru.net/api";

    private String apiToken;

    private String modelVersion = "vlm";

    private int maxPollingAttempts = 60;

    private int pollingIntervalMs = 3000;

    private int connectTimeoutMs = 30000;

    private int readTimeoutMs = 120000;

    private boolean extractImages = true;

    private boolean extractTables = true;

    private boolean extractFormulas = true;

    private String outputFormat = "markdown";
}
