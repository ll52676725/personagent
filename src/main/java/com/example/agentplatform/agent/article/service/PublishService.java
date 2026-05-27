package com.example.agentplatform.agent.article.service;

import com.example.agentplatform.agent.article.config.PlatformType;
import com.example.agentplatform.agent.article.dto.PublishDTO;
import com.example.agentplatform.agent.article.dto.PublishResult;
import com.example.agentplatform.agent.article.entity.Article;
import com.example.agentplatform.agent.article.publish.PlatformPublisher;
import com.example.agentplatform.agent.article.publish.PublishContext;
import com.example.agentplatform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublishService {

    private final List<PlatformPublisher> publishers;
    private final ArticleService articleService;
    private final ImageGenerationService imageGenerationService;

    public List<PublishResult> publishArticle(Long userId, Long articleId, PublishDTO dto) {
        Article article = articleService.getArticle(userId, articleId);

        if (article.getContent() == null || article.getContent().isEmpty()) {
            throw new BusinessException("文章内容为空，无法发布");
        }

        List<PlatformType> platforms = parsePlatforms(dto.getPlatforms());
        List<PublishResult> results = new ArrayList<>();

        for (PlatformType platform : platforms) {
            try {
                PlatformPublisher publisher = getPublisher(platform);
                if (publisher == null) {
                    results.add(PublishResult.failure(platform, "不支持的平台"));
                    continue;
                }

                PublishContext context = buildPublishContext(article, platform, dto);
                PublishResult result = publisher.publish(context);
                results.add(result);

            } catch (Exception e) {
                log.error("发布到{}失败", platform.getName(), e);
                results.add(PublishResult.failure(platform, e.getMessage()));
            }
        }

        updateArticleAfterPublish(article, results);
        return results;
    }

    public List<PublishResult> autoPublish(Long userId, Long articleId) {
        Article article = articleService.getArticle(userId, articleId);
        PublishDTO dto = PublishDTO.builder()
                .platforms(List.of("csdn", "toutiao", "zhihu"))
                .tags(article.getTags() != null ? List.of(article.getTags().split(",")) : List.of())
                .isOriginal(true)
                .build();
        return publishArticle(userId, articleId, dto);
    }

    public String previewForPlatform(Long userId, Long articleId, String platformCode) {
        Article article = articleService.getArticle(userId, articleId);
        PlatformType platform = PlatformType.fromCode(platformCode);

        String content = article.getContent();
        if (content == null || content.isEmpty()) {
            throw new BusinessException("文章内容为空");
        }

        PlatformPublisher publisher = getPublisher(platform);
        if (publisher == null) {
            throw new BusinessException("不支持的平台");
        }

        String contentWithImages = imageGenerationService.insertImagesIntoMarkdown(
            content, article.getTitle(), platform
        );

        PublishContext context = buildPublishContext(article, platform, null);
        context.setPlatformContent(contentWithImages);

        return publisher.preprocessContent(context);
    }

    public Map<String, String> previewAllPlatforms(Long userId, Long articleId) {
        Article article = articleService.getArticle(userId, articleId);
        String content = article.getContent();

        if (content == null || content.isEmpty()) {
            throw new BusinessException("文章内容为空");
        }

        return publishers.stream()
                .collect(Collectors.toMap(
                    publisher -> publisher.getPlatformType().getCode(),
                    publisher -> {
                        try {
                            String contentWithImages = imageGenerationService.insertImagesIntoMarkdown(
                                content, article.getTitle(), publisher.getPlatformType()
                            );
                            PublishContext context = buildPublishContext(article, publisher.getPlatformType(), null);
                            context.setPlatformContent(contentWithImages);
                            return publisher.preprocessContent(context);
                        } catch (Exception e) {
                            return "预览生成失败: " + e.getMessage();
                        }
                    }
                ));
    }

    private List<PlatformType> parsePlatforms(List<String> platformCodes) {
        if (platformCodes == null || platformCodes.isEmpty()) {
            return List.of(PlatformType.CSDN, PlatformType.TOUTIAO, PlatformType.ZHIHU);
        }
        return platformCodes.stream()
                .map(PlatformType::fromCode)
                .collect(Collectors.toList());
    }

    private PlatformPublisher getPublisher(PlatformType platform) {
        for (PlatformPublisher publisher : publishers) {
            if (publisher.getPlatformType() == platform) {
                return publisher;
            }
        }
        return null;
    }

    private PublishContext buildPublishContext(Article article, PlatformType platform, PublishDTO dto) {
        return PublishContext.builder()
                .article(article)
                .platformContent(article.getContent())
                .platformTitle(article.getTitle())
                .platformSummary(article.getSummary())
                .coverImage(article.getCoverImage())
                .tags(dto != null && dto.getTags() != null ? dto.getTags() :
                        (article.getTags() != null ? List.of(article.getTags().split(",")) : List.of()))
                .isOriginal(dto != null && dto.getIsOriginal() != null ? dto.getIsOriginal() : true)
                .targetPlatform(platform)
                .userId(String.valueOf(article.getUserId()))
                .build();
    }

    private void updateArticleAfterPublish(Article article, List<PublishResult> results) {
        boolean allSuccess = results.stream().allMatch(PublishResult::isSuccess);
        article.setStatus(allSuccess ? 1 : 2);

        StringBuilder publishLinks = new StringBuilder("[");
        for (int i = 0; i < results.size(); i++) {
            PublishResult result = results.get(i);
            publishLinks.append(String.format(
                "{\"platform\":\"%s\",\"success\":%s,\"url\":\"%s\"}",
                result.getPlatform().getCode(),
                result.isSuccess(),
                result.getArticleUrl() != null ? result.getArticleUrl() : ""
            ));
            if (i < results.size() - 1) {
                publishLinks.append(",");
            }
        }
        publishLinks.append("]");

        article.setPublishLinks(publishLinks.toString());
        articleService.save(article);
    }

    public boolean validateForPlatform(Long userId, Long articleId, String platformCode) {
        Article article = articleService.getArticle(userId, articleId);
        PlatformType platform = PlatformType.fromCode(platformCode);

        PlatformPublisher publisher = getPublisher(platform);
        if (publisher == null) {
            throw new BusinessException("不支持的平台");
        }

        PublishContext context = buildPublishContext(article, platform, null);
        try {
            return publisher.validate(context);
        } catch (Exception e) {
            log.warn("文章验证失败: {}", e.getMessage());
            return false;
        }
    }
}
