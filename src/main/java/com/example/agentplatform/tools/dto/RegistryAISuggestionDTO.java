package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistryAISuggestionDTO {
    private String category;
    private String categoryLabel;
    private String title;
    private String description;
    private List<String> registryPaths;
    private int issueCount;
    private String riskLevel;
    private String action;
    private String reason;
    private int priority;
    private String impact;
    private String precaution;
}
