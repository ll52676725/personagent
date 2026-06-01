package com.example.agentplatform.tools.service;

import com.example.agentplatform.tools.dto.CronGenerateRequestDTO;
import com.example.agentplatform.tools.dto.CronGenerateResultDTO;
import com.example.agentplatform.tools.dto.CronNLRequestDTO;
import com.example.agentplatform.tools.dto.CronNLResultDTO;
import com.example.agentplatform.tools.dto.CronNextTimesRequestDTO;
import com.example.agentplatform.tools.dto.CronNextTimesResultDTO;
import com.example.agentplatform.tools.dto.CronParseRequestDTO;
import com.example.agentplatform.tools.dto.CronParseResultDTO;

/**
 * Cron表达式服务接口
 * 定义Cron表达式生成、解析、计算和自然语言处理的核心业务方法
 */
public interface CronService {

    /**
     * 根据Cron字段参数生成Cron表达式
     *
     * @param request 生成请求参数，包含各字段的值
     * @return 生成结果DTO，包含生成的Cron表达式
     */
    CronGenerateResultDTO generate(CronGenerateRequestDTO request);

    /**
     * 解析Cron表达式为人类可读描述
     *
     * @param request 解析请求参数，包含Cron表达式
     * @return 解析结果DTO，包含人类可读的描述
     */
    CronParseResultDTO parse(CronParseRequestDTO request);

    /**
     * 计算Cron表达式的下次执行时间
     *
     * @param request 计算请求参数，包含Cron表达式和计算数量
     * @return 计算结果DTO，包含下次执行时间列表
     */
    CronNextTimesResultDTO nextExecutionTimes(CronNextTimesRequestDTO request);

    /**
     * 使用AI解析自然语言为Cron表达式
     *
     * @param request 自然语言解析请求参数，包含自然语言描述
     * @return 解析结果DTO，包含AI解析出的Cron表达式
     */
    CronNLResultDTO parseNaturalLanguage(CronNLRequestDTO request);
}
