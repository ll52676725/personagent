package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IconDesignRequestDTO {

    private String brandName;

    private String description;

    private String iconType;

    private String iconCategory;

    private String industry;

    private String stylePreference;

    private String colorPreference;
}
