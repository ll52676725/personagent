package com.example.agentplatform.tools.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SqlFormatRequestDTO {

    @NotBlank(message = "SQL内容不能为空")
    private String content;

    private Integer indentSize = 4;

    private Boolean uppercase = true;

    private String dbType = "MYSQL";

    private String userIntent;

    private Boolean enableAI = true;
}
