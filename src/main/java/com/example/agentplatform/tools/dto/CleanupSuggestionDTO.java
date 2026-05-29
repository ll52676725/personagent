package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CleanupSuggestionDTO {
    private String type;
    private String title;
    private String description;
    private String path;
    private long size;
    private String riskLevel;
    private String action;
}
