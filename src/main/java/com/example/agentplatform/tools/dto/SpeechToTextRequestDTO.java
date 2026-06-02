package com.example.agentplatform.tools.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SpeechToTextRequestDTO {

    @NotBlank(message = "语言不能为空")
    private String language;

    private String model;

    private String prompt;

    private Double temperature;

    private Boolean verboseJson;
}
