package com.example.agentplatform.agent.article.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateCollectionOutlineDTO {
    @NotNull(message = "合集ID不能为空")
    private Long collectionId;

    private String topic;

    private List<String> keywords;

    private Integer articleCount;

    private String style;

    private String model;
}