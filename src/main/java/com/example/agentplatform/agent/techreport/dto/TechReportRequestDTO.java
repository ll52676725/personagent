package com.example.agentplatform.agent.techreport.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechReportRequestDTO {

    @NotBlank(message = "汇报场景不能为空")
    private String scene;

    private String description;

    @NotBlank(message = "汇报受众不能为空")
    private String audience;

    private String reportType;

    private Integer slideCount;

    private List<String> keyPoints;

    private String industry;

    private String companySize;

    private String additionalInfo;
}
