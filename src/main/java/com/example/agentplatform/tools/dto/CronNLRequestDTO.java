package com.example.agentplatform.tools.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Cron自然语言请求DTO
 * 用于接收前端传递的自然语言转Cron表达式请求参数
 */
@Data
public class CronNLRequestDTO {

    /**
     * 自然语言描述，不能为空
     */
    @NotBlank(message = "自然语言描述不能为空")
    private String naturalLanguage;
}
