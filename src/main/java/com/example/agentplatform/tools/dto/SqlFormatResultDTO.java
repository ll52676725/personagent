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
public class SqlFormatResultDTO {

    private Boolean success;

    private String formattedSql;

    private String compactSql;

    private List<SqlErrorDetailDTO> errors;

    private String aiFixedSql;

    private String aiFixDescription;

    private Boolean aiFixSuccess;

    private Integer indentSize;

    private Boolean uppercase;

    private String dbType;

    private String sqlType;

    private String statistics;

    private List<String> suggestions;
}
