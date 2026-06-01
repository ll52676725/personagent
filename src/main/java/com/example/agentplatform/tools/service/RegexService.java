package com.example.agentplatform.tools.service;

import com.example.agentplatform.tools.dto.*;

/**
 * 正则表达式服务接口
 * <p>
 * 提供正则表达式的三大核心能力：
 * <ul>
 *     <li>正则表达式校验 —— 验证给定正则表达式与待匹配文本是否匹配</li>
 *     <li>AI生成 —— 根据自然语言描述，由 AI 自动生成正则表达式</li>
 *     <li>AI修正 —— 对存在问题的正则表达式，由 AI 自动修正并返回修正后的结果</li>
 * </ul>
 */
public interface RegexService {

    /**
     * 校验正则表达式
     * <p>
     * 使用给定的正则表达式对目标文本进行匹配校验，返回是否匹配以及匹配详情。
     *
     * @param request 正则表达式校验请求，包含正则表达式和待校验文本
     * @return 正则表达式校验结果，包含是否匹配及匹配详情
     */
    RegexValidateResultDTO validate(RegexValidateRequestDTO request);

    /**
     * AI生成正则表达式
     * <p>
     * 根据用户提供的自然语言描述，由 AI 自动生成对应的正则表达式。
     *
     * @param request 正则表达式生成请求，包含自然语言描述等参数
     * @return 正则表达式生成结果，包含 AI 生成的正则表达式
     */
    RegexGenerateResultDTO generate(RegexGenerateRequestDTO request);

    /**
     * AI修正正则表达式
     * <p>
     * 对用户提供的存在问题的正则表达式，由 AI 分析错误原因并自动修正，返回修正后的正则表达式。
     *
     * @param request 正则表达式修正请求，包含待修正的正则表达式及相关上下文
     * @return 正则表达式修正结果，包含修正后的正则表达式
     */
    RegexFixResultDTO fix(RegexFixRequestDTO request);
}
