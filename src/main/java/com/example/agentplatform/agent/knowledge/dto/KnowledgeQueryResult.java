package com.example.agentplatform.agent.knowledge.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeQueryResult {
    private String question;
    private String answer;
    private List<SourceReference> sources;
    private String model;
    private Integer tokens;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SourceReference {
        private Long knowledgeId;
        private String title;
        private String content;
        private Double similarity;
    }
}
