package com.example.agentplatform.agent.article.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleUpdateDTO {
    private String title;
    private String summary;
    private String content;
    private String coverImage;
    private List<String> tags;
    private Integer status;
}