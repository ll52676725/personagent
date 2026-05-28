package com.example.agentplatform.agent.knowledge.config;

import com.example.agentplatform.agent.knowledge.service.EmbeddingProvider;
import com.example.agentplatform.agent.knowledge.service.impl.OpenAiCompatibleEmbeddingProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class EmbeddingConfig {

    private final RestTemplate restTemplate;
    private final EmbeddingProperties embeddingProperties;

    @Bean
    public Map<String, EmbeddingProvider> embeddingProviders() {
        Map<String, EmbeddingProvider> providers = new HashMap<>();

        Map<String, EmbeddingProperties.ProviderConfig> configs = embeddingProperties.getProviders();

        if (configs != null && !configs.isEmpty()) {
            for (Map.Entry<String, EmbeddingProperties.ProviderConfig> entry : configs.entrySet()) {
                String name = entry.getKey();
                EmbeddingProperties.ProviderConfig config = entry.getValue();

                if (config.getBaseUrl() != null && config.getApiKey() != null && config.getModel() != null) {
                    try {
                        EmbeddingProvider provider = new OpenAiCompatibleEmbeddingProvider(restTemplate, name, config);
                        providers.put(name, provider);
                        log.info("已注册 Embedding Provider: {}, baseUrl: {}, model: {}", name, config.getBaseUrl(), config.getModel());
                    } catch (Exception e) {
                        log.warn("注册 Embedding Provider 失败: {}, error: {}", name, e.getMessage());
                    }
                } else {
                    log.warn("Embedding Provider [{}] 配置不完整，跳过", name);
                }
            }
        }

        if (providers.isEmpty()) {
            log.warn("⚠️  未配置任何有效的 Embedding Provider！请在 application.yml 中配置至少一个 provider");
            log.warn("推荐使用智谱AI (https://open.bigmodel.cn/) 或 OpenAI 的 Embedding 服务");
            log.warn("DeepSeek 官方不提供 Embedding 接口，请勿使用 deepseek 作为 embedding provider");
        }

        return providers;
    }
}
