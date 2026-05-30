package com.example.agentplatform.agent.rules.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 规则模板创建DTO
 * 
 * @author System
 * @since 2025-01-01
 */
@Data
public class RuleTemplateCreateDTO {

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 128, message = "模板名称不能超过128个字符")
    private String name;

    @Size(max = 512, message = "描述不能超过512个字符")
    private String description;

    @NotBlank(message = "规则分类不能为空")
    private String category;

    @NotBlank(message = "来源类型不能为空")
    private String sourceType;

    private String targetTool;

    @NotBlank(message = "文件名不能为空")
    private String fileName;

    private String filePath;

    private String content;

    private String variables;

    private String version;

    private Boolean isPublic;
}
