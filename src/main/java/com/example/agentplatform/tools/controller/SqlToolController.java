package com.example.agentplatform.tools.controller;

import com.example.agentplatform.common.dto.Result;
import com.example.agentplatform.tools.dto.SqlFormatRequestDTO;
import com.example.agentplatform.tools.dto.SqlFormatResultDTO;
import com.example.agentplatform.tools.service.SqlFormatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/agent-tools/sql")
@RequiredArgsConstructor
public class SqlToolController {

    private final SqlFormatService sqlFormatService;

    @PostMapping("/format")
    public ResponseEntity<Result<SqlFormatResultDTO>> format(@Valid @RequestBody SqlFormatRequestDTO request) {
        log.info("收到SQL格式化请求，内容长度: {}字符，数据库类型: {}", request.getContent().length(), request.getDbType());
        SqlFormatResultDTO result = sqlFormatService.format(request);
        if (result.getSuccess()) {
            return ResponseEntity.ok(Result.success("SQL格式化成功", result));
        } else {
            return ResponseEntity.badRequest().body(Result.<SqlFormatResultDTO>builder()
                    .code(400)
                    .message("SQL格式化失败")
                    .data(result)
                    .timestamp(System.currentTimeMillis())
                    .build());
        }
    }

    @PostMapping("/compact")
    public ResponseEntity<Result<SqlFormatResultDTO>> compact(@Valid @RequestBody SqlFormatRequestDTO request) {
        log.info("收到SQL压缩请求，内容长度: {}字符", request.getContent().length());
        SqlFormatResultDTO result = sqlFormatService.compact(request);
        if (result.getSuccess()) {
            return ResponseEntity.ok(Result.success("SQL压缩成功", result));
        } else {
            return ResponseEntity.badRequest().body(Result.<SqlFormatResultDTO>builder()
                    .code(400)
                    .message("SQL压缩失败")
                    .data(result)
                    .timestamp(System.currentTimeMillis())
                    .build());
        }
    }

    @PostMapping("/validate")
    public ResponseEntity<Result<SqlFormatResultDTO>> validate(@Valid @RequestBody SqlFormatRequestDTO request) {
        log.info("收到SQL校验请求，内容长度: {}字符", request.getContent().length());
        SqlFormatResultDTO result = sqlFormatService.validate(request);
        if (result.getSuccess()) {
            return ResponseEntity.ok(Result.success("SQL校验通过，语法正确", result));
        } else {
            return ResponseEntity.badRequest().body(Result.<SqlFormatResultDTO>builder()
                    .code(400)
                    .message("SQL校验失败，存在语法问题")
                    .data(result)
                    .timestamp(System.currentTimeMillis())
                    .build());
        }
    }

    @PostMapping("/fix")
    public ResponseEntity<Result<SqlFormatResultDTO>> fixWithAI(@Valid @RequestBody SqlFormatRequestDTO request) {
        log.info("收到AI修复SQL请求，内容长度: {}字符", request.getContent().length());
        SqlFormatResultDTO result = sqlFormatService.fixWithAI(request);
        if (result.getSuccess() && (result.getAiFixSuccess() == null || result.getAiFixSuccess())) {
            return ResponseEntity.ok(Result.success("AI修复SQL成功", result));
        } else {
            return ResponseEntity.badRequest().body(Result.<SqlFormatResultDTO>builder()
                    .code(400)
                    .message("AI修复SQL失败")
                    .data(result)
                    .timestamp(System.currentTimeMillis())
                    .build());
        }
    }

    @PostMapping("/optimize")
    public ResponseEntity<Result<SqlFormatResultDTO>> optimizeWithAI(@Valid @RequestBody SqlFormatRequestDTO request) {
        log.info("收到AI优化SQL请求，内容长度: {}字符", request.getContent().length());
        SqlFormatResultDTO result = sqlFormatService.optimizeWithAI(request);
        if (result.getSuccess()) {
            return ResponseEntity.ok(Result.success("AI优化SQL成功", result));
        } else {
            return ResponseEntity.badRequest().body(Result.<SqlFormatResultDTO>builder()
                    .code(400)
                    .message("AI优化SQL失败")
                    .data(result)
                    .timestamp(System.currentTimeMillis())
                    .build());
        }
    }
}
