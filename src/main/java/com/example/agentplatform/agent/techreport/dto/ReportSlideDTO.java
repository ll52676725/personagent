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
public class ReportSlideDTO {

    private Integer slideNumber;

    private String title;

    private String type;

    private String content;

    private List<String> keyPoints;

    private String speakerNotes;

    private String visualSuggestion;

    private Integer durationMinutes;
}
