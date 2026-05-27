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
public class CollectionOutlineDTO {
    private Long collectionId;

    private String title;

    private String summary;

    private String content;

    private List<OutlineItem> outlines;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OutlineItem {
        private String title;
        private String summary;
        private String keyPoints;
        private Integer order;
    }
}