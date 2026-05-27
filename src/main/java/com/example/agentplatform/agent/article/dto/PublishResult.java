package com.example.agentplatform.agent.article.dto;

import com.example.agentplatform.agent.article.config.PlatformType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishResult {
    private PlatformType platform;
    private boolean success;
    private String articleUrl;
    private String articleId;
    private String errorMessage;
    private LocalDateTime publishedAt;
    private Integer views;
    private Integer likes;
    private Integer comments;

    public static PublishResult success(PlatformType platform, String articleUrl, String articleId) {
        return PublishResult.builder()
                .platform(platform)
                .success(true)
                .articleUrl(articleUrl)
                .articleId(articleId)
                .publishedAt(LocalDateTime.now())
                .build();
    }

    public static PublishResult failure(PlatformType platform, String errorMessage) {
        return PublishResult.builder()
                .platform(platform)
                .success(false)
                .errorMessage(errorMessage)
                .publishedAt(LocalDateTime.now())
                .build();
    }
}
