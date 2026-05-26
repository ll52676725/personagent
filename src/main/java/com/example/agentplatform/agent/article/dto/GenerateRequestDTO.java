package com.example.agentplatform.agent.article.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateRequestDTO {
    private String topic;
    
    private List<String> keywords;
    
    private String title;
    
    private String outline;
    
    private String content;
    
    private String model;
    
    private String style;
}