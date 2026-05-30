package com.example.agentplatform.agent.rules.controller;

import com.example.agentplatform.agent.rules.dto.ConflictResolveDTO;
import com.example.agentplatform.agent.rules.dto.RulePullResultDTO;
import com.example.agentplatform.agent.rules.entity.RulePullLog;
import com.example.agentplatform.agent.rules.service.ConflictResolutionService;
import com.example.agentplatform.common.dto.Result;
import com.example.agentplatform.common.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 冲突处理控制器
 * <p>提供文件冲突的检测、列表查询和解决功能
 * <p>当规则拉取检测到本地文件与模板内容不一致时，需要进行冲突处理
 * 
 * @author System
 * @since 2025-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agent-rules/conflicts")
@RequiredArgsConstructor
public class ConflictController {

    private final ConflictResolutionService conflictResolutionService;

    /**
     * 获取待处理的冲突列表
     * <p>返回当前用户所有存在冲突的拉取记录，按时间倒序排列
     *
     * @return 冲突日志列表
     */
    @GetMapping
    public ResponseEntity<Result<List<RulePullLog>>> listConflicts() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<RulePullLog> conflicts = conflictResolutionService.listConflictLogs(userId);
        return ResponseEntity.ok(Result.success(conflicts));
    }

    /**
     * 获取单个冲突详情
     * <p>包括本地内容、远程内容、差异对比等详细信息
     *
     * @param id 冲突日志ID
     * @return 冲突详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<RulePullLog>> getConflict(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        RulePullLog conflict = conflictResolutionService.getConflictLog(userId, id);
        return ResponseEntity.ok(Result.success(conflict));
    }

    /**
     * 解决冲突
     * <p>根据选择的策略处理文件冲突
     * <p>处理策略：
     * - overwrite: 直接覆盖本地文件
     * - keep_local: 保留本地文件，不做修改
     * - merge: 智能合并（可提供手动合并后的内容）
     * - rename: 将新内容重命名保存，保留原文件
     *
     * @param dto 冲突解决请求
     * @return 处理结果
     */
    @PostMapping("/resolve")
    public ResponseEntity<Result<RulePullResultDTO>> resolveConflict(@Valid @RequestBody ConflictResolveDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("用户 {} 解决冲突，日志ID: {}, 策略: {}", userId, dto.getLogId(), dto.getResolutionStrategy());
        
        RulePullResultDTO result = conflictResolutionService.resolveConflict(userId, dto);
        
        if ("SUCCESS".equals(result.getStatus())) {
            return ResponseEntity.ok(Result.success("冲突已解决", result));
        } else {
            return ResponseEntity.ok(Result.success(result.getMessage(), result));
        }
    }
}
