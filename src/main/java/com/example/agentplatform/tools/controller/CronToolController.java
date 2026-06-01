package com.example.agentplatform.tools.controller;

import com.example.agentplatform.common.dto.Result;
import com.example.agentplatform.tools.dto.*;
import com.example.agentplatform.tools.service.CronService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Cron表达式工具控制器
 * 提供Cron表达式生成、解析、计算下次执行时间和自然语言解析的REST API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agent-tools/cron")
@RequiredArgsConstructor
public class CronToolController {

    /**
     * Cron表达式服务
     */
    private final CronService cronService;

    /**
     * 生成Cron表达式
     * 根据结构化字段（秒、分、时、日、月、周）生成标准Cron表达式
     *
     * @param request 生成请求参数，包含Cron各字段值
     * @return 生成结果，包含生成的Cron表达式
     */
    @PostMapping("/generate")
    public ResponseEntity<Result<CronGenerateResultDTO>> generate(@Valid @RequestBody CronGenerateRequestDTO request) {
        log.info("收到Cron表达式生成请求");
        CronGenerateResultDTO result = cronService.generate(request);
        if (result.getSuccess()) {
            return ResponseEntity.ok(Result.success("Cron表达式生成成功", result));
        } else {
            return ResponseEntity.badRequest().body(Result.<CronGenerateResultDTO>builder()
                    .code(400)
                    .message("Cron表达式生成失败")
                    .data(result)
                    .timestamp(System.currentTimeMillis())
                    .build());
        }
    }

    /**
     * 解析Cron表达式
     * 将Cron表达式解析为人类可读的描述文本
     *
     * @param request 解析请求参数，包含Cron表达式
     * @return 解析结果，包含人类可读的描述
     */
    @PostMapping("/parse")
    public ResponseEntity<Result<CronParseResultDTO>> parse(@Valid @RequestBody CronParseRequestDTO request) {
        log.info("收到Cron表达式解析请求，表达式: {}", request.getCronExpression());
        CronParseResultDTO result = cronService.parse(request);
        if (result.getSuccess()) {
            return ResponseEntity.ok(Result.success("Cron表达式解析成功", result));
        } else {
            return ResponseEntity.badRequest().body(Result.<CronParseResultDTO>builder()
                    .code(400)
                    .message("Cron表达式解析失败")
                    .data(result)
                    .timestamp(System.currentTimeMillis())
                    .build());
        }
    }

    /**
     * 计算下次执行时间
     * 根据Cron表达式计算未来若干次的执行时间点
     *
     * @param request 计算请求参数，包含Cron表达式和计算次数
     * @return 计算结果，包含下次执行时间列表
     */
    @PostMapping("/next-times")
    public ResponseEntity<Result<CronNextTimesResultDTO>> nextTimes(@Valid @RequestBody CronNextTimesRequestDTO request) {
        log.info("收到Cron下次执行时间计算请求，表达式: {}", request.getCronExpression());
        CronNextTimesResultDTO result = cronService.nextExecutionTimes(request);
        if (result.getSuccess()) {
            return ResponseEntity.ok(Result.success("下次执行时间计算成功", result));
        } else {
            return ResponseEntity.badRequest().body(Result.<CronNextTimesResultDTO>builder()
                    .code(400)
                    .message("下次执行时间计算失败")
                    .data(result)
                    .timestamp(System.currentTimeMillis())
                    .build());
        }
    }

    /**
     * 自然语言解析
     * 使用AI将自然语言描述解析为Cron表达式
     *
     * @param request 自然语言解析请求参数，包含自然语言描述文本
     * @return 解析结果，包含生成的Cron表达式和说明
     */
    @PostMapping("/parse-natural-language")
    public ResponseEntity<Result<CronNLResultDTO>> parseNaturalLanguage(@Valid @RequestBody CronNLRequestDTO request) {
        log.info("收到Cron自然语言解析请求，输入: {}", request.getNaturalLanguage());
        CronNLResultDTO result = cronService.parseNaturalLanguage(request);
        if (result.getSuccess()) {
            return ResponseEntity.ok(Result.success("自然语言解析成功", result));
        } else {
            return ResponseEntity.badRequest().body(Result.<CronNLResultDTO>builder()
                    .code(400)
                    .message("自然语言解析失败")
                    .data(result)
                    .timestamp(System.currentTimeMillis())
                    .build());
        }
    }
}
