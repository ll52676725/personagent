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
public class KnowledgeImportDTO {
    @NotBlank(message = "URL不能为空")
    private String url;

    private Long baseId;

    private String title;

    private String category;

    private String tags;
}
