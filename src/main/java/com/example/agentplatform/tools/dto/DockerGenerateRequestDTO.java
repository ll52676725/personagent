package com.example.agentplatform.tools.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DockerGenerateRequestDTO {

    @NotBlank(message = "项目路径不能为空")
    private String projectPath;

    private String imageName;

    private String imageTag;

    private Integer port;

    private String jdkVersion;

    private String buildTool;

    private String jvmOpts;

    private String springProfile;

    private Boolean includeDockerCompose = true;
}
