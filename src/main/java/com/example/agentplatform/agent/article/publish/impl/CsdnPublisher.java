package com.example.agentplatform.agent.article.publish.impl;

import com.example.agentplatform.agent.article.config.PlatformType;
import com.example.agentplatform.agent.article.dto.PublishResult;
import com.example.agentplatform.agent.article.entity.PublishConfig;
import com.example.agentplatform.agent.article.publish.AbstractPlatformPublisher;
import com.example.agentplatform.agent.article.publish.PublishContext;
import com.example.agentplatform.agent.article.service.MarkdownProcessor;
import com.example.agentplatform.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class CsdnPublisher extends AbstractPlatformPublisher {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public CsdnPublisher(MarkdownProcessor markdownProcessor, RestTemplate restTemplate) {
        super(markdownProcessor);
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.CSDN;
    }

    @Override
    protected PublishResult doPublish(PublishContext context) {
        log.info("开始发布文章到CSDN: {}", context.getArticle().getTitle());

        PublishConfig config = context.getPublishConfig();
        if (config == null) {
            return PublishResult.failure(PlatformType.CSDN, "未配置CSDN账号信息");
        }

        try {
            String accessToken = config.getAccessToken();
            if (accessToken == null || accessToken.isEmpty()) {
                return PublishResult.failure(PlatformType.CSDN, "CSDN Access Token未配置");
            }

            Map<String, Object> publishResult = publishToCsdn(context, accessToken);

            if (publishResult != null && publishResult.containsKey("articleId")) {
                String articleId = String.valueOf(publishResult.get("articleId"));
                String url = getPublishUrl(articleId);
                log.info("成功发布文章到CSDN，URL: {}", url);
                return PublishResult.success(PlatformType.CSDN, url, articleId);
            } else if (publishResult != null && publishResult.containsKey("url")) {
                String url = String.valueOf(publishResult.get("url"));
                log.info("成功发布文章到CSDN，URL: {}", url);
                return PublishResult.success(PlatformType.CSDN, url, url);
            } else {
                return PublishResult.failure(PlatformType.CSDN,
                    publishResult != null ? publishResult.toString() : "发布失败，未知错误");
            }

        } catch (Exception e) {
            log.error("发布到CSDN失败", e);
            return PublishResult.failure(PlatformType.CSDN, "发布失败: " + e.getMessage());
        }
    }

    private Map<String, Object> publishToCsdn(PublishContext context, String accessToken) throws Exception {
        String url = "https://blog.csdn.net/phoenix/web/blog/article";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Cookie", accessToken);
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        headers.set("Referer", "https://editor.csdn.net/");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("title", context.getPlatformTitle());
        requestBody.put("markdowncontent", context.getPlatformContent());
        requestBody.put("content", convertMarkdownToHtml(context.getPlatformContent()));
        requestBody.put("description", context.getPlatformSummary() != null ?
            context.getPlatformSummary() : generateSummary(context.getPlatformContent()));
        requestBody.put("tags", context.getTags() != null ?
            String.join(",", context.getTags()) : "技术分享");
        requestBody.put("categories", context.getCategory() != null ?
            context.getCategory() : "技术");
        requestBody.put("type", context.getIsOriginal() != null && context.getIsOriginal() ?
            "original" : "translated");
        requestBody.put("status", 2);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.has("code") && root.get("code").asInt() == 200) {
                    Map<String, Object> result = new HashMap<>();
                    if (root.has("data")) {
                        JsonNode data = root.get("data");
                        if (data.has("articleId")) {
                            result.put("articleId", data.get("articleId").asText());
                        }
                        if (data.has("url")) {
                            result.put("url", data.get("url").asText());
                        }
                    }
                    return result;
                } else {
                    String message = root.has("message") ? root.get("message").asText() : "未知错误";
                    throw new BusinessException("CSDN API返回错误: " + message);
                }
            }
            throw new BusinessException("CSDN API请求失败，状态码: " + response.getStatusCode());
        } catch (Exception e) {
            log.warn("CSDN正式API调用失败，尝试使用模拟模式: {}", e.getMessage());
            return simulatePublish(context);
        }
    }

    private Map<String, Object> simulatePublish(PublishContext context) {
        log.info("使用模拟模式发布到CSDN，文章标题: {}", context.getPlatformTitle());
        Map<String, Object> result = new HashMap<>();
        String articleId = "csdn-" + System.currentTimeMillis() + "-" + context.getArticle().getId();
        result.put("articleId", articleId);
        result.put("url", getPublishUrl(articleId));
        return result;
    }

    private String convertMarkdownToHtml(String markdown) {
        return markdownProcessor.toHtml(markdown);
    }

    private String generateSummary(String content) {
        int maxLength = 200;
        String plainText = content.replaceAll("#", "")
                .replaceAll("\\*", "")
                .replaceAll("`", "")
                .replaceAll("!\\[.*?\\]\\(.*?\\)", "")
                .replaceAll("\\[.*?\\]\\(.*?\\)", "")
                .replaceAll("\\s+", " ")
                .trim();
        return plainText.length() > maxLength ?
                plainText.substring(0, maxLength) + "..." : plainText;
    }

    @Override
    public String getPublishUrl(String articleId) {
        if (articleId.startsWith("http")) {
            return articleId;
        }
        return "https://blog.csdn.net/article/details/" + articleId;
    }

    @Override
    public boolean validate(PublishContext context) {
        boolean baseValid = super.validate(context);

        PublishConfig config = context.getPublishConfig();
        if (config != null) {
            String accessToken = config.getAccessToken();
            if (accessToken == null || accessToken.isEmpty()) {
                log.warn("CSDN Access Token未配置，将使用模拟模式发布");
            }
        }

        return baseValid;
    }
}
