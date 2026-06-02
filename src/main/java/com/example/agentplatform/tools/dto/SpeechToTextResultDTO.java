package com.example.agentplatform.tools.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SpeechToTextResultDTO {

    private String text;

    private String language;

    private String detectedLanguage;

    private Double duration;

    private String model;

    private String originalFileName;

    private Long originalSize;

    private String audioFormat;

    private Long durationMs;
}
