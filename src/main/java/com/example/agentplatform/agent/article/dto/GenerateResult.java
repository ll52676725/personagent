package com.example.agentplatform.agent.article.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateResult {
    private String type;
    private List<String> items;
    private String content;
    private String model;
    private Integer tokens;
    
    public static GenerateResult fromItems(String type, List<String> items) {
        return GenerateResult.builder()
                .type(type)
                .items(items)
                .build();
    }
    
    public static GenerateResult fromContent(String type, String content) {
        return GenerateResult.builder()
                .type(type)
                .content(content)
                .build();
    }
}