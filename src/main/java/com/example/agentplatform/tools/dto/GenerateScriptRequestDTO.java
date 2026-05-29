package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateScriptRequestDTO {
    private List<String> selectedIssueIds;
    private String scriptType;
    private boolean includeBackup;
}
