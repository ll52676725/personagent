package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CleanupScriptDTO {
    private String scriptName;
    private String scriptContent;
    private String scriptType;
    private String encoding;
    private int issueCount;
    private String warning;
    private String usageInstructions;
}
