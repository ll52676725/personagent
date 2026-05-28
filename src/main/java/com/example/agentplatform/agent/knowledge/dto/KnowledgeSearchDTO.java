package com.example.agentplatform.agent.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeSearchDTO {
    @NotBlank(message = "搜索关键词不能为空")
    private String keyword;

    private Long baseId;

    @Builder.Default
    private Integer topK = 10;
}
