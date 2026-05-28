package com.example.agentplatform.tools.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageConvertResultDTO {

    private String originalFormat;

    private String targetFormat;

    private String originalFileName;

    private String convertedFileName;

    private Long originalSize;

    private Long convertedSize;

    private Integer width;

    private Integer height;

    private String mimeType;

    private String imageDataBase64;

    @JsonIgnore
    private byte[] rawImageData;
}
