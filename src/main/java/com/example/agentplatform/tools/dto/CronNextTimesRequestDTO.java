package com.example.agentplatform.tools.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Cron下次执行时间请求DTO
 * 用于查询Cron表达式的未来执行时间
 */
@Data
public class CronNextTimesRequestDTO {

    /**
     * Cron表达式
     */
    @NotBlank(message = "Cron表达式不能为空")
    private String cronExpression;

    /**
     * 返回的执行时间数量，默认5次
     */
    private Integer count = 5;

    /**
     * 是否排除法定节假日
     */
    private Boolean excludeHoliday = false;

    /**
     * 是否使用中国法定节假日（含调休补班）
     */
    private Boolean useChinaHoliday = true;
}
