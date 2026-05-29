package com.example.agentplatform.tools.service;

import com.example.agentplatform.tools.dto.JsonFormatRequestDTO;
import com.example.agentplatform.tools.dto.JsonFormatResultDTO;

/**
 * JSON格式化服务接口
 * 定义JSON格式化、校验和修复的核心业务方法
 */
public interface JsonFormatService {

    /**
     * 格式化JSON字符串
     * 对输入的JSON进行解析和美化格式化，支持缩进调整和键排序
     *
     * @param request 格式化请求参数，包含JSON内容和格式化选项
     * @return 格式化结果DTO，包含格式化后的内容或错误信息
     */
    JsonFormatResultDTO format(JsonFormatRequestDTO request);

    /**
     * 压缩JSON字符串
     * 将格式化的JSON字符串压缩为无空格的紧凑格式
     *
     * @param request 格式化请求参数，包含JSON内容
     * @return 压缩结果DTO，包含压缩后的JSON
     */
    JsonFormatResultDTO compact(JsonFormatRequestDTO request);

    /**
     * 校验JSON语法
     * 检查JSON字符串是否符合语法规范，返回详细的错误信息
     *
     * @param request 校验请求参数，包含JSON内容
     * @return 校验结果DTO，包含错误详情列表
     */
    JsonFormatResultDTO validate(JsonFormatRequestDTO request);

    /**
     * 使用AI修复JSON格式错误
     * 当JSON存在语法错误时，调用LLM尝试自动修复格式问题
     *
     * @param request 修复请求参数，包含错误的JSON内容
     * @return 修复结果DTO，包含AI修复后的JSON和修复说明
     */
    JsonFormatResultDTO fixWithAI(JsonFormatRequestDTO request);
}
