package com.example.agentplatform.tools.controller;

import com.example.agentplatform.common.dto.Result;
import com.example.agentplatform.tools.dto.JsonFormatRequestDTO;
import com.example.agentplatform.tools.dto.JsonFormatResultDTO;
import com.example.agentplatform.tools.service.JsonFormatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * JSON格式化工具控制器
 * 提供JSON格式化、压缩、校验和AI修复的REST API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agent-tools/json")
@RequiredArgsConstructor
public class JsonToolController {

    /**
     * JSON格式化服务
     */
    private final JsonFormatService jsonFormatService;

    /**
     * 格式化JSON字符串
     * 对输入的JSON进行解析和美化格式化，支持缩进调整和键排序
     *
     * @param request 格式化请求参数，包含JSON内容和格式化选项
     * @return 格式化结果，包含格式化后的内容或错误信息
     */
    @PostMapping("/format")
    public ResponseEntity<Result<JsonFormatResultDTO>> format(@Valid @RequestBody JsonFormatRequestDTO request) {
        log.info("收到JSON格式化请求，内容长度: {}字符", request.getContent().length());
        JsonFormatResultDTO result = jsonFormatService.format(request);
        if (result.getSuccess()) {
            return ResponseEntity.ok(Result.success("JSON格式化成功", result));
        } else {
            return ResponseEntity.badRequest().body(Result.<JsonFormatResultDTO>builder()
                    .code(400)
                    .message("JSON格式化失败")
                    .data(result)
                    .timestamp(System.currentTimeMillis())
                    .build());
        }
    }

    /**
     * 压缩JSON字符串
     * 将格式化的JSON字符串压缩为无空格的紧凑格式
     *
     * @param request 压缩请求参数，包含JSON内容
     * @return 压缩结果，包含压缩后的JSON
     */
    @PostMapping("/compact")
    public ResponseEntity<Result<JsonFormatResultDTO>> compact(@Valid @RequestBody JsonFormatRequestDTO request) {
        log.info("收到JSON压缩请求，内容长度: {}字符", request.getContent().length());
        JsonFormatResultDTO result = jsonFormatService.compact(request);
        if (result.getSuccess()) {
            return ResponseEntity.ok(Result.success("JSON压缩成功", result));
        } else {
            return ResponseEntity.badRequest().body(Result.<JsonFormatResultDTO>builder()
                    .code(400)
                    .message("JSON压缩失败")
                    .data(result)
                    .timestamp(System.currentTimeMillis())
                    .build());
        }
    }

    /**
     * 校验JSON语法
     * 检查JSON字符串是否符合语法规范，返回详细的错误信息
     *
     * @param request 校验请求参数，包含JSON内容
     * @return 校验结果，包含错误详情列表
     */
    @PostMapping("/validate")
    public ResponseEntity<Result<JsonFormatResultDTO>> validate(@Valid @RequestBody JsonFormatRequestDTO request) {
        log.info("收到JSON校验请求，内容长度: {}字符", request.getContent().length());
        JsonFormatResultDTO result = jsonFormatService.validate(request);
        if (result.getSuccess()) {
            return ResponseEntity.ok(Result.success("JSON校验通过，格式正确", result));
        } else {
            return ResponseEntity.badRequest().body(Result.<JsonFormatResultDTO>builder()
                    .code(400)
                    .message("JSON校验失败，存在语法错误")
                    .data(result)
                    .timestamp(System.currentTimeMillis())
                    .build());
        }
    }

    /**
     * 使用AI修复JSON格式错误
     * 当JSON存在语法错误时，调用LLM尝试自动修复格式问题
     *
     * @param request 修复请求参数，包含错误的JSON内容
     * @return 修复结果，包含AI修复后的JSON和修复说明
     */
    @PostMapping("/fix")
    public ResponseEntity<Result<JsonFormatResultDTO>> fixWithAI(@Valid @RequestBody JsonFormatRequestDTO request) {
        log.info("收到AI修复JSON请求，内容长度: {}字符", request.getContent().length());
        JsonFormatResultDTO result = jsonFormatService.fixWithAI(request);
        if (result.getSuccess() && result.getAiFixSuccess()) {
            return ResponseEntity.ok(Result.success("AI修复JSON成功", result));
        } else {
            return ResponseEntity.badRequest().body(Result.<JsonFormatResultDTO>builder()
                    .code(400)
                    .message("AI修复JSON失败")
                    .data(result)
                    .timestamp(System.currentTimeMillis())
                    .build());
        }
    }
}
