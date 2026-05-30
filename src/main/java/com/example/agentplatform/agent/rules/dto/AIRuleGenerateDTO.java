package com.example.agentplatform.agent.rules.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI规则生成请求DTO
 * 
 * @author System
 * @since 2025-01-01
 */
@Data
public class AIRuleGenerateDTO {

    @NotBlank(message = "规则分类不能为空")
    private String category;

    private String targetTool;

    @NotBlank(message = "规则描述不能为空")
    private String description;

    private String projectType;

    private String codingLanguage;

    private String additionalRequirements;

    private String fileName;

    private Boolean saveAsTemplate;
}
