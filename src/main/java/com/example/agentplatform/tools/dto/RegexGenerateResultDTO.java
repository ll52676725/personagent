package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 正则表达式生成结果DTO
 * <p>
 * 用于封装正则表达式生成的返回结果，包括生成的正则模式、
 * 详细解释、测试用例、置信度、替代方案等信息。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegexGenerateResultDTO {

    /**
     * 操作是否成功
     */
    private Boolean success;

    /**
     * 生成的正则表达式模式
     */
    private String pattern;

    /**
     * 正则描述信息
     */
    private String description;

    /**
     * 详细解释
     * <p>对生成的正则表达式各部分的逐项说明</p>
     */
    private String explanation;

    /**
     * 测试用例列表
     */
    private List<String> testCases;

    /**
     * 置信度
     * <p>取值范围0-1，表示AI对生成结果的信心程度</p>
     */
    private Double confidence;

    /**
     * AI模型标识
     * <p>标识本次生成所使用的AI模型</p>
     */
    private String aiModel;

    /**
     * 是否降级
     * <p>当AI模型不可用时是否降级为规则引擎生成</p>
     */
    private Boolean fallback;

    /**
     * 错误信息列表
     */
    private List<String> errors;

    /**
     * 替代方案列表
     * <p>当置信度不高时提供的备选正则表达式方案</p>
     */
    private List<AlternativeDTO> alternatives;

    /**
     * 替代方案DTO
     * <p>
     * 封装备选正则表达式方案，包含正则模式和对应描述。
     * </p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlternativeDTO {
        /**
         * 替代正则表达式模式
         */
        private String pattern;
        /**
         * 替代方案的描述
         */
        private String description;
    }
}
