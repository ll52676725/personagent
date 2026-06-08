package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DockerDeployResultDTO {

    private String imageName;
    private String containerId;
    private String containerName;
    private String status;
    private String buildLog;
    private String runLog;
    private Integer mappedPort;
    private Boolean success;
    private String errorMessage;
}
