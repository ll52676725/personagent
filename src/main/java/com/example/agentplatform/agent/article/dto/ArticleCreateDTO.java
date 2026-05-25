package com.example.agentplatform.agent.article.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleCreateDTO {
    @NotBlank(message = "标题不能为空")
    private String title;
    
    private String summary;
    
    private String content;
    
    private String coverImage;
    
    private List<String> tags;
}