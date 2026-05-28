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
public class MinerUParseResult {

    private String textContent;

    private String markdownContent;

    private String jsonContent;

    private List<MinerUImage> images;

    private List<MinerUTable> tables;

    private List<MinerUFormula> formulas;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MinerUImage {
        private String url;
        private String path;
        private String caption;
        private String alt;
        private Integer pageNum;
        private Float confidence;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MinerUTable {
        private String html;
        private String markdown;
        private String caption;
        private Integer rowCount;
        private Integer colCount;
        private Integer pageNum;
        private List<List<String>> cells;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MinerUFormula {
        private String latex;
        private boolean inline;
        private Integer pageNum;
        private String rawText;
    }
}
