package com.example.agentplatform.agent.article.publish;

import com.example.agentplatform.agent.article.config.PlatformType;
import com.example.agentplatform.agent.article.entity.Article;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishContext {
    private Article article;
    private String platformContent;
    private String platformTitle;
    private String platformSummary;
    private String coverImage;
    private List<String> tags;
    private Boolean isOriginal;
    private String category;
    private PlatformType targetPlatform;
    private String userId;
    private String token;
}
