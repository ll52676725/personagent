package com.example.agentplatform.tools.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AudioFormatInfoDTO {

    private String formatName;

    private String extension;

    private String mimeType;

    private String description;

    private Long maxFileSize;
}
