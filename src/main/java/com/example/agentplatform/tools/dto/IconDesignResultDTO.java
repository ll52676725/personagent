package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IconDesignResultDTO {

    private String brandName;
    private String initial;
    private String iconCategory;
    private String graphicShape;
    private String suggestedStyle;
    private String suggestedShape;
    private String primaryColor;
    private String secondaryColor;
    private String bgColor;
    private String textColor;
    private String subText;
    private String designRationale;
    private String[] decorativeElements;
}
