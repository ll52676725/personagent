package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Cron表达式下次执行时间结果DTO
 * 用于返回Cron表达式下次执行时间查询的结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CronNextTimesResultDTO {

    /**
     * 查询是否成功
     */
    private Boolean success;

    /**
     * 查询的Cron表达式
     */
    private String cronExpression;

    /**
     * 下次执行时间列表，ISO格式的日期时间字符串
     */
    private List<String> nextTimes;

    /**
     * 错误信息，失败时返回
     */
    private List<String> errors;

    /**
     * 被排除的节假日执行时间列表
     */
    private List<String> excludedTimes;

    /**
     * 节假日说明信息
     */
    private List<String> holidayInfo;
}
