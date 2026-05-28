package com.example.agentplatform.agent.knowledge.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeUpdateDTO {
    @Size(max = 256, message = "标题不能超过256个字符")
    private String title;

    private String content;

    private String tags;

    private String category;
}
