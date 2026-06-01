package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Cron表达式生成结果DTO
 * 用于返回Cron表达式生成操作的结果，包括生成的表达式和可读描述
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CronGenerateResultDTO {

    /**
     * 生成是否成功
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
     * 预设名称，如匹配到预设则返回
     */
    private String presetName;

    /**
     * Cron表达式是否有效
     */
    private Boolean valid;

    /**
     * 警告信息列表，如自动修正通知
     */
    private java.util.List<String> warnings;
}
