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
public class KnowledgeBaseUpdateDTO {
    @Size(max = 128, message = "知识库名称不能超过128个字符")
    private String name;

    @Size(max = 512, message = "描述不能超过512个字符")
    private String description;

    private String icon;
}
