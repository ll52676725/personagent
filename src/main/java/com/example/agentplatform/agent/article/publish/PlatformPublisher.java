package com.example.agentplatform.agent.article.publish;

import com.example.agentplatform.agent.article.config.PlatformType;
import com.example.agentplatform.agent.article.dto.PublishResult;

public interface PlatformPublisher {
    PlatformType getPlatformType();
    PublishResult publish(PublishContext context);
    boolean validate(PublishContext context);
    String preprocessContent(PublishContext context);
    String getPublishUrl(String articleId);
}
