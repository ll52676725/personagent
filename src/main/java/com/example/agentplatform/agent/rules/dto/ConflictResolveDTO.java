package com.example.agentplatform.agent.rules.dto;

import lombok.Data;

/**
 * 冲突解决请求DTO
 * 
 * @author System
 * @since 2025-01-01
 */
@Data
public class ConflictResolveDTO {

    private Long logId;

    private String resolutionStrategy;

    private String mergedContent;

    private String renameSuffix;
}
