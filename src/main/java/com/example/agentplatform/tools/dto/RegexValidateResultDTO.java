package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 正则表达式校验结果DTO
 * <p>
 * 用于封装正则表达式校验的返回结果，包括校验是否成功、
 * 正则语法是否有效、匹配结果列表、错误信息等。
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegexValidateResultDTO {

    /**
     * 操作是否成功
     */
    private Boolean success;

    /**
     * 正则语法是否有效
     */
    private Boolean valid;

    /**
     * 校验的正则模式
     */
    private String pattern;

    /**
     * 正则描述信息
     */
    private String description;

    /**
     * 匹配结果列表
     */
    private List<MatchResultDTO> matches;

    /**
     * 匹配数量
     */
    private Integer matchCount;

    /**
     * 错误信息列表
     */
    private List<String> errors;

    /**
     * 命名捕获组名称列表
     */
    private List<String> groups;

    /**
     * 匹配结果DTO
     * <p>
     * 封装单次正则匹配的详细信息，包括匹配文本、起止位置和捕获组。
     * </p>
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchResultDTO {
        /**
         * 匹配到的文本
         */
        private String matchedText;
        /**
         * 匹配文本的起始位置（含）
         */
        private Integer startIndex;
        /**
         * 匹配文本的结束位置（不含）
         */
        private Integer endIndex;
        /**
         * 捕获组列表，按顺序排列
         */
        private List<String> groups;
    }
}
