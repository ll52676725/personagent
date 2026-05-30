package com.example.agentplatform.agent.rules.service;

import com.example.agentplatform.agent.rules.dto.ConflictResolveDTO;
import com.example.agentplatform.agent.rules.dto.RulePullResultDTO;
import com.example.agentplatform.agent.rules.entity.RulePullLog;

import java.util.List;

/**
 * 冲突处理服务接口
 * 
 * @author System
 * @since 2025-01-01
 */
public interface ConflictResolutionService {

    RulePullResultDTO detectConflict(String localContent, String remoteContent, String targetPath);

    String generateDiff(String content1, String content2);

    String attemptMerge(String localContent, String remoteContent);

    RulePullResultDTO resolveConflict(Long userId, ConflictResolveDTO dto);

    List<RulePullLog> listConflictLogs(Long userId);

    RulePullLog getConflictLog(Long userId, Long logId);
}
