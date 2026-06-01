package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Cron表达式解析结果DTO
 * 用于返回Cron表达式解析操作的结果，包括各字段解析和错误信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CronParseResultDTO {

    /**
     * 解析是否成功
     */
    private Boolean success;

    /**
     * Cron表达式是否有效
     */
    private Boolean valid;

    /**
     * 人类可读的Cron表达式描述
     */
    private String description;

    /**
     * 各字段解析结果
     * 键为字段名称，值为字段值的描述
     */
    private Map<String, String> fields;

    /**
     * 错误信息列表
     */
    private List<String> errors;
}
