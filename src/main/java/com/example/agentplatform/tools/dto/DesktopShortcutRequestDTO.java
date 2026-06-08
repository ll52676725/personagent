package com.example.agentplatform.tools.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DesktopShortcutRequestDTO {

    @NotBlank(message = "工具ID不能为空")
    private String toolId;

    @NotBlank(message = "工具名称不能为空")
    private String toolName;

    @NotBlank(message = "工具路径不能为空")
    private String toolPath;

    private String baseUrl;

    private Boolean openAsApp = true;
}
