package com.example.agentplatform.tools.dto;

import lombok.Data;

/**
 * Cron表达式生成请求DTO
 * 用于接收前端传递的Cron表达式各字段参数
 */
@Data
public class CronGenerateRequestDTO {

    /**
     * 秒，默认为"*"
     */
    private String second = "*";

    /**
     * 分，默认为"*"
     */
    private String minute = "*";

    /**
     * 时，默认为"*"
     */
    private String hour = "*";

    /**
     * 日，默认为"*"
     */
    private String day = "*";

    /**
     * 月，默认为"*"
     */
    private String month = "*";

    /**
     * 星期，默认为"?"
     */
    private String weekDay = "?";

    /**
     * 年，可选字段
     */
    private String year;
}
