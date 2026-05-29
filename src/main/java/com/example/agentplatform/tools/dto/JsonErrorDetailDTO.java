package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * JSON错误详情DTO
 * 用于描述JSON解析错误的详细信息，包括错误位置、原因和修复建议
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JsonErrorDetailDTO {

    /**
     * 错误类型，如：语法错误、缺少引号、多余逗号等
     */
    private String errorType;

    /**
     * 错误发生的行号（从1开始）
     */
    private Integer lineNumber;

    /**
     * 错误发生的列号（从1开始）
     */
    private Integer columnNumber;

    /**
     * 错误位置附近的原始内容片段
     */
    private String errorContext;

    /**
     * 具体的错误描述信息
     */
    private String message;

    /**
     * 针对该错误的修复建议
     */
    private String suggestion;
}
