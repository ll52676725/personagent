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
public class PublishDTO {
    private List<String> platforms;
    private List<String> tags;
    private Boolean isOriginal;
}