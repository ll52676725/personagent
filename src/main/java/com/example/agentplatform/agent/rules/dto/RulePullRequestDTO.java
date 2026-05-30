package com.example.agentplatform.agent.rules.dto;

import lombok.Data;

/**
 * 规则拉取请求DTO
 * 
 * @author System
 * @since 2025-01-01
 */
@Data
public class RulePullRequestDTO {

    private Long templateId;

    private Long configId;

    private String conflictStrategy;

    private Boolean previewOnly;
}
