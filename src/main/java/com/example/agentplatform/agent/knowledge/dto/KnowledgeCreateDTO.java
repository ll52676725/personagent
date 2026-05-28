package com.example.agentplatform.agent.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeCreateDTO {
    @NotBlank(message = "知识标题不能为空")
    @Size(max = 256, message = "标题不能超过256个字符")
    private String title;

    private String content;

    @Builder.Default
    private String sourceType = "manual";

    private String sourceUrl;

    private String fileName;

    private String fileType;

    private Long fileSize;

    private String tags;

    private String category;
}
