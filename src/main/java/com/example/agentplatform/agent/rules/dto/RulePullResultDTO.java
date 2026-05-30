package com.example.agentplatform.agent.rules.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 规则拉取结果DTO
 * 
 * @author System
 * @since 2025-01-01
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RulePullResultDTO {

    private Long logId;

    private Long configId;

    private String status;

    private Boolean hasConflict;

    private String conflictType;

    private String conflictStrategy;

    private String targetPath;

    private String localContent;

    private String remoteContent;

    private String mergedContent;

    private String diffResult;

    private String message;
}
