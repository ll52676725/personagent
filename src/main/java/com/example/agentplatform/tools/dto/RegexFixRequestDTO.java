package com.example.agentplatform.tools.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 正则表达式修正请求DTO
 * <p>
 * 用于封装正则表达式修正的请求参数，支持提供错误信息、
 * 测试字符串和修正意图以辅助AI进行精准修正。
 * </p>
 */
@Data
public class RegexFixRequestDTO {

    /**
     * 待修正的正则表达式
     * <p>必填项，不能为空</p>
     */
    @NotBlank(message = "正则表达式不能为空")
    private String pattern;

    /**
     * 测试字符串
     * <p>可选项，用于验证修正后的正则是否满足预期匹配</p>
     */
    private String testString;

    /**
     * 错误信息
     * <p>可选项，描述当前正则存在的问题或异常信息</p>
     */
    private String errorMessage;

    /**
     * 修正意图描述
     * <p>可选项，说明期望的修正方向或目标行为</p>
     */
    private String intent;
}
