package com.example.agentplatform.tools.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Cron表达式解析请求DTO
 * 用于接收前端传递的Cron表达式解析请求参数
 */
@Data
public class CronParseRequestDTO {

    /**
     * 需要解析的Cron表达式，不能为空
     */
    @NotBlank(message = "Cron表达式不能为空")
    private String cronExpression;
}
