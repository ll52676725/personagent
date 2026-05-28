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
public class KnowledgeQueryDTO {
    @NotBlank(message = "问题不能为空")
    private String question;

    private Long baseId;

    @Builder.Default
    private Integer topK = 5;

    @Builder.Default
    private Double similarityThreshold = 0.5;
}
