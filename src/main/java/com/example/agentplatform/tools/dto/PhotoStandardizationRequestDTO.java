package com.example.agentplatform.tools.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PhotoStandardizationRequestDTO {
    
    @NotBlank(message = "证件照尺寸不能为空")
    private String photoSize;
    
    @NotBlank(message = "背景颜色不能为空")
    private String backgroundColor;
    
    @NotNull(message = "输出格式不能为空")
    private Boolean jpegOutput;
    
    private Integer quality;
    
    private Boolean autoDetectFace;
    
    private Double headTopMargin;
    
    private Double headBottomMargin;
}
