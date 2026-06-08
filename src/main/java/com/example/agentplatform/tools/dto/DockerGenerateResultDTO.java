package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DockerGenerateResultDTO {

    private String projectPath;
    private String projectName;
    private String buildTool;
    private String jdkVersion;
    private String packaging;
    private String mainClass;
    private String dockerfileContent;
    private String dockerignoreContent;
    private String dockerComposeContent;
    private Boolean dockerfileWritten;
    private Boolean dockerignoreWritten;
    private Boolean dockerComposeWritten;
}
