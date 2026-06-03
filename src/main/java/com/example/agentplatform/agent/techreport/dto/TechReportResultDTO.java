package com.example.agentplatform.agent.techreport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechReportResultDTO {

    private String reportTitle;

    private String executiveSummary;

    private List<ReportSlideDTO> slides;

    private List<String> qaPreparation;

    private List<String> presentationTips;

    private Integer totalSlides;

    private Integer estimatedDurationMinutes;

    private String model;

    private String contentMarkdown;
}
