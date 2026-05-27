package com.example.agentplatform.agent.article.dto;

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
public class CollectionCreateDTO {
    @NotBlank(message = "合集标题不能为空")
    @Size(max = 256, message = "标题长度不能超过256字符")
    private String title;

    @Size(max = 512, message = "封面图URL长度不能超过512字符")
    private String coverImage;

    private String description;
}