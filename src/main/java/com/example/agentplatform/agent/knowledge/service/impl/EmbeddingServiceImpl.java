package com.example.agentplatform.agent.knowledge.service.impl;

import com.example.agentplatform.agent.knowledge.config.EmbeddingProperties;
import com.example.agentplatform.agent.knowledge.service.EmbeddingProvider;
import com.example.agentplatform.agent.knowledge.service.EmbeddingService;
import com.example.agentplatform.common.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingServiceImpl implements EmbeddingService {

    private final Map<String, EmbeddingProvider> embeddingProviders;
    private final EmbeddingProperties embeddingProperties;

    private EmbeddingProvider primaryProvider;

    @PostConstruct
    public void init() {
        String configuredProvider = embeddingProperties.getProvider();

        if (embeddingProviders.isEmpty()) {
            log.error("========================================================================");
            log.error("❌ 没有可用的 Embedding Provider！请检查 application.yml 配置");
            log.error("========================================================================");
            log.error("DeepSeek 官方不提供 Embedding API，请使用以下方案之一：");
            log.error("");
            log.error("方案 1: 智谱AI (推荐，国内服务快，中文效果好)");
            log.error("  baseUrl: https://open.bigmodel.cn/api/paas/v4");
            log.error("  model: embedding-3");
            log.error("  申请地址: https://open.bigmodel.cn/");
            log.error("");
            log.error("方案 2: OpenAI");
            log.error("  baseUrl: https://api.openai.com/v1");
            log.error("  model: text-embedding-3-small");
            log.error("");
            log.error("方案 3: 通义千问");
            log.error("  baseUrl: https://dashscope.aliyuncs.com/compatible-mode/v1");
            log.error("  model: text-embedding-v2");
            log.error("========================================================================");
            return;
        }

        if (embeddingProviders.containsKey(configuredProvider)) {
            primaryProvider = embeddingProviders.get(configuredProvider);
            log.info("✅ Embedding 服务初始化完成，当前使用 Provider: [{}]", configuredProvider);
        } else {
            Map.Entry<String, EmbeddingProvider> first = embeddingProviders.entrySet().iterator().next();
            primaryProvider = first.getValue();
            log.warn("⚠️  配置的 Provider [{}] 不存在，自动使用第一个可用的 Provider: [{}]", configuredProvider, first.getKey());
        }

        log.info("已加载的 Embedding Provider 列表: {}", embeddingProviders.keySet());
    }

    private void checkProvider() {
        if (primaryProvider == null) {
            throw new BusinessException(
                    "未配置有效的 Embedding 服务。DeepSeek 不提供 Embedding API，" +
                    "请在 application.yml 中配置智谱AI、OpenAI 或通义千问的 Embedding 服务。" +
                    "详情请查看启动日志中的配置说明。"
            );
        }
    }

    @Override
    public List<Double> embed(String text) {
        checkProvider();
        return primaryProvider.embed(text);
    }

    @Override
    public List<List<Double>> embedBatch(List<String> texts) {
        checkProvider();
        return primaryProvider.embedBatch(texts);
    }

    @Override
    public double cosineSimilarity(List<Double> vectorA, List<Double> vectorB) {
        if (vectorA == null || vectorB == null || vectorA.size() != vectorB.size()) {
            return 0.0;
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.size(); i++) {
            dotProduct += vectorA.get(i) * vectorB.get(i);
            normA += vectorA.get(i) * vectorA.get(i);
            normB += vectorB.get(i) * vectorB.get(i);
        }
        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public List<String> getAvailableProviders() {
        return embeddingProviders.keySet().stream().sorted().collect(Collectors.toList());
    }

    public void switchProvider(String providerName) {
        if (!embeddingProviders.containsKey(providerName)) {
            throw new BusinessException("Provider [" + providerName + "] 不存在，可用: " + embeddingProviders.keySet());
        }
        primaryProvider = embeddingProviders.get(providerName);
        log.info("已切换 Embedding Provider 为: [{}]", providerName);
    }
}
