package com.example.agentplatform.agent.article.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateArticleFromOutlineDTO {
    @NotNull(message = "合集ID不能为空")
    private Long collectionId;

    @NotNull(message = "大纲序号不能为空")
    private Integer outlineIndex;

    private String model;

    private String style;
}