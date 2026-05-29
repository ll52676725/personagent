package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistryIssueDTO {
    private String id;
    private String category;
    private String categoryLabel;
    private String registryPath;
    private String valueName;
    private String issueType;
    private String issueTypeLabel;
    private String description;
    private String reason;
    private String riskLevel;
    private String severity;
    private boolean selected;
    private String regCommand;
}
