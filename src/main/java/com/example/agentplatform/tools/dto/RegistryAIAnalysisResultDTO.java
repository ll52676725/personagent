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
public class RegistryAIAnalysisResultDTO {
    private String summary;
    private String systemHealthScore;
    private String systemHealthLevel;
    private int totalIssues;
    private int highRiskCount;
    private int mediumRiskCount;
    private int lowRiskCount;
    private List<RegistryAISuggestionDTO> suggestions;
    private String analysisInsight;
    private String optimizationAdvice;
    private String model;
    private Integer tokens;
    private long analysisDurationMs;
}
