package com.example.agentplatform.agent.article.publish;

import com.example.agentplatform.agent.article.config.PlatformType;
import com.example.agentplatform.agent.article.dto.PublishResult;
import com.example.agentplatform.agent.article.service.MarkdownProcessor;
import com.example.agentplatform.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractPlatformPublisher implements PlatformPublisher {

    protected final MarkdownProcessor markdownProcessor;

    protected AbstractPlatformPublisher(MarkdownProcessor markdownProcessor) {
        this.markdownProcessor = markdownProcessor;
    }

    @Override
    public PublishResult publish(PublishContext context) {
        try {
            if (!validate(context)) {
                return PublishResult.failure(getPlatformType(), "文章不符合平台发布要求");
            }

            String processedContent = preprocessContent(context);
            context.setPlatformContent(processedContent);

            String platformTitle = generatePlatformTitle(context);
            context.setPlatformTitle(platformTitle);

            String platformSummary = generatePlatformSummary(context);
            context.setPlatformSummary(platformSummary);

            return doPublish(context);
        } catch (Exception e) {
            log.error("发布到{}失败", getPlatformType().getName(), e);
            return PublishResult.failure(getPlatformType(), "发布失败: " + e.getMessage());
        }
    }

    @Override
    public boolean validate(PublishContext context) {
        if (context.getArticle() == null) {
            return false;
        }

        String content = context.getArticle().getContent();
        if (content == null || content.isEmpty()) {
            return false;
        }

        int wordCount = markdownProcessor.countWords(content);
        int minWordCount = getPlatformType().getMinWordCount();
        if (wordCount < minWordCount) {
            log.warn("文章字数{}低于平台要求的{}", wordCount, minWordCount);
            throw new BusinessException(String.format(
                "文章字数(%d)低于%s要求的最低字数(%d)",
                wordCount, getPlatformType().getName(), minWordCount
            ));
        }

        int imageCount = markdownProcessor.countImages(content);
        int minImageCount = getPlatformType().getMinImageCount();
        if (imageCount < minImageCount) {
            log.warn("文章图片数{}低于平台要求的{}", imageCount, minImageCount);
            throw new BusinessException(String.format(
                "文章图片数量(%d)低于%s要求的最低数量(%d)",
                imageCount, getPlatformType().getName(), minImageCount
            ));
        }

        return true;
    }

    public String preprocessContent(PublishContext context) {
        String originalContent = context.getArticle().getContent();
        return markdownProcessor.convertForPlatform(originalContent, getPlatformType());
    }

    protected String generatePlatformTitle(PublishContext context) {
        return context.getArticle().getTitle();
    }

    protected String generatePlatformSummary(PublishContext context) {
        return context.getArticle().getSummary();
    }

    protected abstract PublishResult doPublish(PublishContext context);

    @Override
    public String getPublishUrl(String articleId) {
        return "https://" + getPlatformType().getCode() + ".com/article/" + articleId;
    }
}
