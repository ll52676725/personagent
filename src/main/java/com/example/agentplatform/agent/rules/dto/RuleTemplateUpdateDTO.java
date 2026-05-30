package com.example.agentplatform.agent.rules.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 规则模板更新DTO
 * 
 * @author System
 * @since 2025-01-01
 */
@Data
public class RuleTemplateUpdateDTO {

    @Size(max = 128, message = "模板名称不能超过128个字符")
    private String name;

    @Size(max = 512, message = "描述不能超过512个字符")
    private String description;

    private String category;

    private String targetTool;

    private String fileName;

    private String filePath;

    private String content;

    private String variables;

    private String version;

    private Boolean isPublic;

    private Integer status;
}
