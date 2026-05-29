package com.example.agentplatform.tools.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoStandardizationResultDTO {
    
    private String originalFileName;
    
    private String convertedFileName;
    
    private Long originalSize;
    
    private Long convertedSize;
    
    private Integer width;
    
    private Integer height;
    
    private String photoSize;
    
    private String backgroundColor;
    
    private String dpi;
    
    private String mimeType;
    
    private String imageDataBase64;
}
