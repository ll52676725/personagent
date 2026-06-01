package com.example.agentplatform.tools.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 正则表达式生成请求DTO
 * <p>
 * 用于封装通过自然语言描述自动生成正则表达式的请求参数，
 * 支持提供参考测试字符串和分类信息以提升生成质量。
 * </p>
 */
@Data
public class RegexGenerateRequestDTO {

    /**
     * 自然语言描述
     * <p>必填项，用于描述期望匹配的文本规则</p>
     */
    @NotBlank(message = "描述不能为空")
    private String description;

    /**
     * 参考测试字符串
     * <p>可选项，提供后可作为生成正则的参考样例</p>
     */
    private String testString;

    /**
     * 分类
     * <p>可选项，用于指定正则的用途分类，如邮箱、手机号、日期等</p>
     */
    private String category;
}
