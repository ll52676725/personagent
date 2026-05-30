package com.example.agentplatform.agent.rules.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 规则配置创建DTO
 * 
 * @author System
 * @since 2025-01-01
 */
@Data
public class RuleConfigCreateDTO {

    private Long templateId;

    private String projectPath;

    @NotBlank(message = "规则分类不能为空")
    private String category;

    private String targetTool;

    @NotBlank(message = "文件名不能为空")
    private String fileName;

    @NotBlank(message = "目标路径不能为空")
    private String targetPath;

    private String content;

    private String conflictStrategy;
}
