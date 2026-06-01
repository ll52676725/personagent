package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Cron自然语言转表达式结果DTO
 * 用于返回自然语言转Cron表达式操作的结果，包括置信度和AI模型信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CronNLResultDTO {

    /**
     * 转换是否成功
     */
    private Boolean success;

    /**
     * 生成的Cron表达式
     */
    private String cronExpression;

    /**
     * 人类可读的Cron表达式描述
     */
    private String description;

    /**
     * 转换置信度，取值范围0-1
     */
    private Double confidence;

    /**
     * 使用的AI模型名称，可能为空
     */
    private String aiModel;

    /**
     * 是否为降级（非AI）结果
     */
    private Boolean fallback;

    /**
     * 错误信息列表
     */
    private List<String> errors;
}
