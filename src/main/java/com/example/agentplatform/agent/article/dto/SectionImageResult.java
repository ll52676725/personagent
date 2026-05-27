package com.example.agentplatform.agent.article.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SectionImageResult {
    private String sectionTitle;
    private String imageUrl;
    private String caption;
    private String insertPosition;
    private boolean success;
}
