package com.example.agentplatform.tools.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PhotoSizeDTO {
    
    private String code;
    
    private String name;
    
    private String widthMm;
    
    private String heightMm;
    
    private Integer widthPx;
    
    private Integer heightPx;
    
    private String description;
    
    private String usage;
}
