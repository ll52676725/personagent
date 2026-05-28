package com.example.agentplatform.tools.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ImageConvertRequestDTO {

    @NotBlank(message = "目标格式不能为空")
    private String targetFormat;

    private Float quality;

    private Integer width;

    private Integer height;
}
