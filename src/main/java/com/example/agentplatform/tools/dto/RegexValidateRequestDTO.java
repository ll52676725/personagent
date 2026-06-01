package com.example.agentplatform.tools.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 正则表达式校验请求DTO
 * <p>
 * 用于封装正则表达式校验的请求参数，包括待校验的正则模式、
 * 可选的测试字符串以及正则标志位。
 * </p>
 */
@Data
public class RegexValidateRequestDTO {

    /**
     * 正则表达式模式字符串
     * <p>必填项，不能为空</p>
     */
    @NotBlank(message = "正则表达式不能为空")
    private String pattern;

    /**
     * 待测试的字符串
     * <p>可选项，提供后将返回该字符串与正则的匹配结果</p>
     */
    private String testString;

    /**
     * 正则标志位
     * <p>可选项，对应Java {@link java.util.regex.Pattern}的标志常量，如CASE_INSENSITIVE等</p>
     */
    private Integer flags;
}
