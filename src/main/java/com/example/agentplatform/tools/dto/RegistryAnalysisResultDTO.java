package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistryAnalysisResultDTO {
    private String summary;
    private int totalIssues;
    private long analysisDurationMs;
    private Map<String, Integer> categoryStats;
    private Map<String, Integer> severityStats;
    private List<RegistryIssueDTO> issues;
    private String disclaimer;
}
