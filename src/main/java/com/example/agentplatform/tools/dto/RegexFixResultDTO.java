package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 正则表达式修正结果DTO
 * <p>
 * 用于封装正则表达式修正的返回结果，包括修正前后的正则模式、
 * 修正说明、详细解释、测试匹配结果、使用建议等信息。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegexFixResultDTO {

    /**
     * 操作是否成功
     */
    private Boolean success;

    /**
     * 修正前的原始正则模式
     */
    private String originalPattern;

    /**
     * 修正后的正则模式
     */
    private String fixedPattern;

    /**
     * 修正说明
     * <p>描述本次修正的具体内容和原因</p>
     */
    private String fixDescription;

    /**
     * 详细解释
     * <p>对修正后正则表达式各部分的逐项说明</p>
     */
    private String explanation;

    /**
     * 修正后的正则语法是否有效
     */
    private Boolean valid;

    /**
     * 测试匹配结果列表
     * <p>使用测试字符串对修正后正则的匹配验证结果</p>
     */
    private List<RegexValidateResultDTO.MatchResultDTO> testMatches;

    /**
     * AI模型标识
     * <p>标识本次修正所使用的AI模型</p>
     */
    private String aiModel;

    /**
     * 是否降级
     * <p>当AI模型不可用时是否降级为规则引擎修正</p>
     */
    private Boolean fallback;

    /**
     * 错误信息列表
     */
    private List<String> errors;

    /**
     * 使用建议列表
     * <p>对修正后正则的使用注意事项和最佳实践建议</p>
     */
    private List<String> suggestions;
}
