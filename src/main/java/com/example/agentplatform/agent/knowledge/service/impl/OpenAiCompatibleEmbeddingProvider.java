package com.example.agentplatform.agent.knowledge.service.impl;

import com.example.agentplatform.agent.knowledge.config.EmbeddingProperties;
import com.example.agentplatform.agent.knowledge.service.EmbeddingProvider;
import com.example.agentplatform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class OpenAiCompatibleEmbeddingProvider implements EmbeddingProvider {

    private final RestTemplate restTemplate;
    private final String providerName;
    private final EmbeddingProperties.ProviderConfig config;

    @Override
    public String getName() {
        return providerName;
    }

    @Override
    public List<Double> embed(String text) {
        try {
            String url = config.getBaseUrl() + "/embeddings";
            Map<String, Object> request = new HashMap<>();
            request.put("model", config.getModel());
            request.put("input", Collections.singletonList(text));

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + config.getApiKey());
            headers.set("Content-Type", "application/json");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            log.debug("调用 Embedding API [{}]: url={}, model={}", providerName, url, config.getModel());

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
                if (data != null && !data.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    List<Double> embedding = (List<Double>) data.get(0).get("embedding");
                    return embedding;
                }
            }
            throw new BusinessException("[" + providerName + "] 嵌入向量生成失败：无效的响应，状态码: " + response.getStatusCode());
        } catch (BusinessException e) {
            throw e;
        } catch (HttpClientErrorException e) {
            log.error("[{}] Embedding API 调用失败，状态码: {}, 响应: {}, url: {}, model: {}",
                    providerName, e.getStatusCode(), e.getResponseBodyAsString(), config.getBaseUrl() + "/embeddings", config.getModel(), e);
            String errorMsg = String.format("[%s] 嵌入向量生成失败：%s %s", providerName, e.getStatusCode(), e.getStatusText());
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                errorMsg += "（请检查 API 地址是否正确。注意：DeepSeek 官方不提供 Embedding 接口！）";
            } else if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                errorMsg += "（请检查 API Key 是否正确）";
            } else if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                errorMsg += "（" + e.getResponseBodyAsString() + "）";
            }
            throw new BusinessException(errorMsg);
        } catch (Exception e) {
            log.error("[{}] 嵌入向量生成失败, url: {}, model: {}", providerName, config.getBaseUrl() + "/embeddings", config.getModel(), e);
            throw new BusinessException("[" + providerName + "] 嵌入向量生成失败：" + e.getMessage());
        }
    }

    @Override
    public List<List<Double>> embedBatch(List<String> texts) {
        try {
            String url = config.getBaseUrl() + "/embeddings";
            Map<String, Object> request = new HashMap<>();
            request.put("model", config.getModel());
            request.put("input", texts);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + config.getApiKey());
            headers.set("Content-Type", "application/json");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            log.debug("调用 Embedding API [{}] 批量: url={}, model={}, count={}", providerName, url, config.getModel(), texts.size());

            ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
                if (data != null && data.size() == texts.size()) {
                    return data.stream()
                            .map(item -> {
                                @SuppressWarnings("unchecked")
                                List<Double> embedding = (List<Double>) item.get("embedding");
                                return embedding;
                            })
                            .collect(Collectors.toList());
                }
            }
            throw new BusinessException("[" + providerName + "] 批量嵌入向量生成失败：无效的响应");
        } catch (Exception e) {
            log.warn("[{}] 批量 Embedding 失败，降级为单条处理: {}", providerName, e.getMessage());
            return texts.stream()
                    .map(this::embed)
                    .collect(Collectors.toList());
        }
    }
}
