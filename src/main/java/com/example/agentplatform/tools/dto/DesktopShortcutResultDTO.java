package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DesktopShortcutResultDTO {

    private Boolean success;

    private String shortcutPath;

    private String toolName;

    private String url;

    private String message;
}
