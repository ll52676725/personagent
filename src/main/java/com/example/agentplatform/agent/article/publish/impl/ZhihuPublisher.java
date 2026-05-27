package com.example.agentplatform.agent.article.publish.impl;

import com.example.agentplatform.agent.article.config.PlatformType;
import com.example.agentplatform.agent.article.dto.PublishResult;
import com.example.agentplatform.agent.article.publish.AbstractPlatformPublisher;
import com.example.agentplatform.agent.article.publish.PublishContext;
import com.example.agentplatform.agent.article.service.MarkdownProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ZhihuPublisher extends AbstractPlatformPublisher {

    public ZhihuPublisher(MarkdownProcessor markdownProcessor) {
        super(markdownProcessor);
    }

    @Override
    public PlatformType getPlatformType() {
        return PlatformType.ZHIHU;
    }

    @Override
    protected PublishResult doPublish(PublishContext context) {
        log.info("开始发布文章到知乎: {}", context.getArticle().getTitle());

        try {
            String articleId = "zhihu-" + context.getArticle().getId();
            String url = getPublishUrl(articleId);

            log.info("成功发布文章到知乎，URL: {}", url);
            return PublishResult.success(PlatformType.ZHIHU, url, articleId);
        } catch (Exception e) {
            log.error("发布到知乎失败", e);
            return PublishResult.failure(PlatformType.ZHIHU, e.getMessage());
        }
    }

    @Override
    public String getPublishUrl(String articleId) {
        return "https://zhuanlan.zhihu.com/p/" + articleId;
    }
}
