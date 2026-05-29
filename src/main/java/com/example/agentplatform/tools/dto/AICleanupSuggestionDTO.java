package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AICleanupSuggestionDTO {
    private String category;
    private String title;
    private String description;
    private String path;
    private long estimatedSize;
    private String riskLevel;
    private String action;
    private String reason;
    private int priority;
}
