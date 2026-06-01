package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlErrorDetailDTO {

    private String errorType;

    private Integer lineNumber;

    private Integer columnNumber;

    private String errorContext;

    private String message;

    private String suggestion;
}
