package com.example.agentplatform.tools.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * JSON格式化请求DTO
 * 用于接收前端传递的JSON格式化请求参数
 */
@Data
public class JsonFormatRequestDTO {

    /**
     * 需要格式化的JSON字符串，不能为空
     */
    @NotBlank(message = "JSON内容不能为空")
    private String content;

    /**
     * 缩进空格数，默认为2
     * 可选值：2, 4, 8
     */
    private Integer indentSize = 2;

    /**
     * 是否对键名进行排序，默认为false
     */
    private Boolean sortKeys = false;
}
