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
public class AIAnalysisResultDTO {
    private String driveLetter;
    private String summary;
    private long totalReclaimableSpace;
    private List<AICleanupSuggestionDTO> suggestions;
    private String analysisInsight;
    private String model;
    private Integer tokens;
    private long analysisDurationMs;
}
