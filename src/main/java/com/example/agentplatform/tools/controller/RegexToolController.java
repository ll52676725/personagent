package com.example.agentplatform.tools.controller;

import com.example.agentplatform.common.dto.Result;
import com.example.agentplatform.tools.dto.*;
import com.example.agentplatform.tools.service.RegexService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 正则表达式工具 REST API 控制器
 * <p>
 * 提供正则表达式相关的操作接口，包括校验、AI生成和AI修正功能。
 * 基础路径：{@code /api/v1/agent-tools/regex}
 * <ul>
 *   <li>{@code POST /validate} - 校验正则表达式与测试字符串是否匹配</li>
 *   <li>{@code POST /generate} - 根据自然语言描述由AI生成正则表达式</li>
 *   <li>{@code POST /fix} - 由AI修正存在问题的正则表达式</li>
 * </ul>
 *
 * @see RegexService
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agent-tools/regex")
@RequiredArgsConstructor
public class RegexToolController {

    private final RegexService regexService;

    /**
     * 校验正则表达式
     * <p>
     * API路径：{@code POST /api/v1/agent-tools/regex/validate}
     * <p>
     * 使用指定的正则表达式模式对测试字符串进行匹配校验，
     * 返回匹配结果及所有匹配项的详细信息。
     *
     * @param request 正则表达式校验请求，包含正则模式和待测试字符串
     * @return 校验成功时返回200及匹配结果；校验失败时返回400及错误信息
     */
    @PostMapping("/validate")
    public ResponseEntity<Result<RegexValidateResultDTO>> validate(@Valid @RequestBody RegexValidateRequestDTO request) {
        log.info("收到正则表达式校验请求，模式: {}", request.getPattern());
        RegexValidateResultDTO result = regexService.validate(request);
        if (result.getSuccess()) {
            return ResponseEntity.ok(Result.success("正则表达式校验成功", result));
        } else {
            return ResponseEntity.badRequest().body(Result.<RegexValidateResultDTO>builder()
                    .code(400)
                    .message("正则表达式校验失败")
                    .data(result)
                    .timestamp(System.currentTimeMillis())
                    .build());
        }
    }

    /**
     * AI生成正则表达式
     * <p>
     * API路径：{@code POST /api/v1/agent-tools/regex/generate}
     * <p>
     * 根据用户提供的自然语言描述，由AI自动生成符合要求的正则表达式，
     * 并附带生成说明和测试用例。
     *
     * @param request 正则表达式生成请求，包含自然语言描述
     * @return 生成成功时返回200及生成的正则表达式；生成失败时返回400及错误信息
     */
    @PostMapping("/generate")
    public ResponseEntity<Result<RegexGenerateResultDTO>> generate(@Valid @RequestBody RegexGenerateRequestDTO request) {
        log.info("收到AI生成正则表达式请求，描述: {}", request.getDescription());
        RegexGenerateResultDTO result = regexService.generate(request);
        if (result.getSuccess()) {
            return ResponseEntity.ok(Result.success("AI生成正则表达式成功", result));
        } else {
            return ResponseEntity.badRequest().body(Result.<RegexGenerateResultDTO>builder()
                    .code(400)
                    .message("AI生成正则表达式失败")
                    .data(result)
                    .timestamp(System.currentTimeMillis())
                    .build());
        }
    }

    /**
     * AI修正正则表达式
     * <p>
     * API路径：{@code POST /api/v1/agent-tools/regex/fix}
     * <p>
     * 对用户提供的存在问题的正则表达式，由AI自动分析并修正，
     * 返回修正后的正则表达式及修正说明。
     *
     * @param request 正则表达式修正请求，包含原始模式和问题描述
     * @return 修正成功时返回200及修正后的正则表达式；修正失败时返回400及错误信息
     */
    @PostMapping("/fix")
    public ResponseEntity<Result<RegexFixResultDTO>> fix(@Valid @RequestBody RegexFixRequestDTO request) {
        log.info("收到AI修正正则表达式请求，模式: {}", request.getPattern());
        RegexFixResultDTO result = regexService.fix(request);
        if (result.getSuccess()) {
            return ResponseEntity.ok(Result.success("AI修正正则表达式成功", result));
        } else {
            return ResponseEntity.badRequest().body(Result.<RegexFixResultDTO>builder()
                    .code(400)
                    .message("AI修正正则表达式失败")
                    .data(result)
                    .timestamp(System.currentTimeMillis())
                    .build());
        }
    }
}
