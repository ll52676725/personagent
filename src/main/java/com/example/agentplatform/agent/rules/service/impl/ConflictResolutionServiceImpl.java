package com.example.agentplatform.agent.rules.service.impl;

import com.example.agentplatform.agent.rules.dto.ConflictResolveDTO;
import com.example.agentplatform.agent.rules.dto.RulePullResultDTO;
import com.example.agentplatform.agent.rules.entity.RulePullLog;
import com.example.agentplatform.agent.rules.enums.ConflictStrategy;
import com.example.agentplatform.agent.rules.repository.RulePullLogRepository;
import com.example.agentplatform.agent.rules.service.ConflictResolutionService;
import com.example.agentplatform.common.exception.BusinessException;
import com.example.agentplatform.tools.common.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 冲突处理服务实现类
 * <p>提供文件冲突检测、差异生成、智能合并、冲突解决等核心功能
 * 
 * @author System
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConflictResolutionServiceImpl implements ConflictResolutionService {

    private final RulePullLogRepository rulePullLogRepository;

    @Override
    public RulePullResultDTO detectConflict(String localContent, String remoteContent, String targetPath) {
        log.debug("【冲突检测】检测文件: {}", targetPath);
        
        boolean hasConflict = false;
        String conflictType = null;
        
        if (localContent == null) {
            hasConflict = false;
            conflictType = "NEW_FILE";
        } else if (remoteContent == null) {
            hasConflict = false;
            conflictType = "NO_REMOTE";
        } else if (localContent.equals(remoteContent)) {
            hasConflict = false;
            conflictType = "IDENTICAL";
        } else {
            hasConflict = true;
            conflictType = determineConflictType(localContent, remoteContent);
        }
        
        String diffResult = null;
        if (hasConflict) {
            diffResult = generateDiff(localContent, remoteContent);
        }
        
        return RulePullResultDTO.builder()
                .hasConflict(hasConflict)
                .conflictType(conflictType)
                .targetPath(targetPath)
                .localContent(localContent)
                .remoteContent(remoteContent)
                .diffResult(diffResult)
                .status(hasConflict ? "CONFLICT" : "NO_CONFLICT")
                .message(hasConflict ? "检测到文件冲突" : "无冲突")
                .build();
    }

    @Override
    public String generateDiff(String content1, String content2) {
        if (content1 == null || content2 == null) {
            return "";
        }
        
        StringBuilder diff = new StringBuilder();
        String[] lines1 = content1.split("\n");
        String[] lines2 = content2.split("\n");
        
        diff.append("--- 本地文件\n");
        diff.append("+++ 远程模板\n");
        diff.append("@@ -1,").append(lines1.length).append(" +1,").append(lines2.length).append(" @@\n");
        
        int i = 0, j = 0;
        while (i < lines1.length || j < lines2.length) {
            if (i < lines1.length && j < lines2.length && lines1[i].equals(lines2[j])) {
                diff.append("  ").append(lines1[i]).append("\n");
                i++;
                j++;
            } else {
                boolean foundMatch = false;
                for (int lookAhead = 1; lookAhead < Math.min(5, lines1.length - i); lookAhead++) {
                    for (int lookAhead2 = 1; lookAhead2 < Math.min(5, lines2.length - j); lookAhead2++) {
                        if (i + lookAhead < lines1.length && j + lookAhead2 < lines2.length
                                && lines1[i + lookAhead].equals(lines2[j + lookAhead2])) {
                            for (int k = 0; k < lookAhead; k++) {
                                diff.append("- ").append(lines1[i + k]).append("\n");
                            }
                            for (int k = 0; k < lookAhead2; k++) {
                                diff.append("+ ").append(lines2[j + k]).append("\n");
                            }
                            i += lookAhead;
                            j += lookAhead2;
                            foundMatch = true;
                            break;
                        }
                    }
                    if (foundMatch) break;
                }
                
                if (!foundMatch) {
                    if (i < lines1.length) {
                        diff.append("- ").append(lines1[i]).append("\n");
                        i++;
                    }
                    if (j < lines2.length) {
                        diff.append("+ ").append(lines2[j]).append("\n");
                        j++;
                    }
                }
            }
        }
        
        return diff.toString();
    }

    @Override
    public String attemptMerge(String localContent, String remoteContent) {
        if (localContent == null) {
            return remoteContent;
        }
        if (remoteContent == null) {
            return localContent;
        }
        
        try {
            String[] localLines = localContent.split("\n");
            String[] remoteLines = remoteContent.split("\n");
            
            StringBuilder merged = new StringBuilder();
            int i = 0, j = 0;
            boolean hasMergeConflict = false;
            
            while (i < localLines.length || j < remoteLines.length) {
                if (i < localLines.length && j < remoteLines.length && localLines[i].equals(remoteLines[j])) {
                    merged.append(localLines[i]).append("\n");
                    i++;
                    j++;
                } else if (i < localLines.length && (j >= remoteLines.length 
                        || isSectionHeader(localLines[i]) && !isSectionHeader(remoteLines[j]))) {
                    merged.append(localLines[i]).append("\n");
                    i++;
                } else if (j < remoteLines.length) {
                    merged.append(remoteLines[j]).append("\n");
                    j++;
                }
            }
            
            String result = merged.toString().trim();
            if (result.isEmpty() || hasSignificantDifference(localContent, remoteContent)) {
                hasMergeConflict = true;
            }
            
            if (hasMergeConflict) {
                return null;
            }
            
            return result;
        } catch (Exception e) {
            log.warn("【智能合并】自动合并失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional
    public RulePullResultDTO resolveConflict(Long userId, ConflictResolveDTO dto) {
        log.info("【冲突解决】用户 {} 处理冲突日志: {}, 策略: {}", userId, dto.getLogId(), dto.getResolutionStrategy());
        
        RulePullLog pullLog = rulePullLogRepository.findById(dto.getLogId())
                .orElseThrow(() -> new BusinessException("拉取日志不存在"));
        
        if (!userId.equals(pullLog.getUserId())) {
            throw new BusinessException("无权限处理此冲突");
        }
        
        if (!"PENDING".equals(pullLog.getStatus())) {
            throw new BusinessException("该冲突已处理，状态: " + pullLog.getStatus());
        }
        
        ConflictStrategy strategy = ConflictStrategy.fromCode(dto.getResolutionStrategy());
        String finalContent = null;
        String targetPath = pullLog.getTargetPath();
        
        try {
            switch (strategy) {
                case OVERWRITE:
                    finalContent = pullLog.getRemoteContent();
                    writeFile(targetPath, finalContent);
                    break;
                    
                case KEEP_LOCAL:
                    finalContent = pullLog.getLocalContent();
                    break;
                    
                case MERGE:
                    if (dto.getMergedContent() != null) {
                        finalContent = dto.getMergedContent();
                    } else {
                        finalContent = attemptMerge(pullLog.getLocalContent(), pullLog.getRemoteContent());
                        if (finalContent == null) {
                            throw new BusinessException("自动合并失败，请手动合并后提供mergedContent");
                        }
                    }
                    writeFile(targetPath, finalContent);
                    break;
                    
                case RENAME:
                    String suffix = dto.getRenameSuffix() != null ? dto.getRenameSuffix() : ".new";
                    targetPath = generateNewFileName(pullLog.getTargetPath(), suffix);
                    finalContent = pullLog.getRemoteContent();
                    writeFile(targetPath, finalContent);
                    break;
                    
                case ASK:
                default:
                    pullLog.setStatus("PENDING");
                    rulePullLogRepository.save(pullLog);
                    return RulePullResultDTO.builder()
                            .logId(pullLog.getId())
                            .status("PENDING")
                            .hasConflict(true)
                            .conflictType(pullLog.getConflictType())
                            .conflictStrategy(strategy.getCode())
                            .targetPath(targetPath)
                            .localContent(pullLog.getLocalContent())
                            .remoteContent(pullLog.getRemoteContent())
                            .diffResult(pullLog.getDiffResult())
                            .message("等待用户处理")
                            .build();
            }
            
            pullLog.setConflictStrategy(strategy.getCode());
            pullLog.setMergedContent(finalContent);
            pullLog.setStatus("RESOLVED");
            pullLog.setMergeSuccessful(true);
            rulePullLogRepository.save(pullLog);
            
            return RulePullResultDTO.builder()
                    .logId(pullLog.getId())
                    .configId(pullLog.getConfigId())
                    .status("SUCCESS")
                    .hasConflict(true)
                    .conflictType(pullLog.getConflictType())
                    .conflictStrategy(strategy.getCode())
                    .targetPath(targetPath)
                    .mergedContent(finalContent)
                    .message("冲突已解决，策略: " + strategy.getDesc())
                    .build();
                    
        } catch (IOException e) {
            log.error("【冲突解决】写入文件失败: {}", e.getMessage(), e);
            pullLog.setStatus("FAILED");
            pullLog.setErrorMessage(e.getMessage());
            rulePullLogRepository.save(pullLog);
            throw new BusinessException("写入文件失败: " + e.getMessage());
        }
    }

    @Override
    public List<RulePullLog> listConflictLogs(Long userId) {
        return rulePullLogRepository.findByUserIdAndHasConflictTrueOrderByCreatedAtDesc(userId);
    }

    @Override
    public RulePullLog getConflictLog(Long userId, Long logId) {
        RulePullLog log = rulePullLogRepository.findById(logId)
                .orElseThrow(() -> new BusinessException("日志不存在"));
        
        if (!userId.equals(log.getUserId())) {
            throw new BusinessException("无权限访问此日志");
        }
        
        return log;
    }

    private String determineConflictType(String localContent, String remoteContent) {
        int localLen = localContent.length();
        int remoteLen = remoteContent.length();
        
        if (localLen == 0) return "LOCAL_EMPTY";
        if (remoteLen == 0) return "REMOTE_EMPTY";
        
        double similarity = calculateSimilarity(localContent, remoteContent);
        if (similarity > 0.9) {
            return "MINOR_CHANGES";
        } else if (similarity > 0.5) {
            return "MODERATE_CHANGES";
        } else {
            return "MAJOR_CONFLICT";
        }
    }

    private double calculateSimilarity(String s1, String s2) {
        if (Objects.equals(s1, s2)) return 1.0;
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;
        
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash1 = md.digest(s1.getBytes());
            byte[] hash2 = md.digest(s2.getBytes());
            
            int matches = 0;
            for (int i = 0; i < Math.min(hash1.length, hash2.length); i++) {
                if (hash1[i] == hash2[i]) matches++;
            }
            
            return (double) matches / Math.max(hash1.length, hash2.length);
        } catch (NoSuchAlgorithmException e) {
            return 0.5;
        }
    }

    private boolean isSectionHeader(String line) {
        return line != null && (line.startsWith("#") || line.startsWith("##") || line.startsWith("###")
                || line.startsWith("// ====") || line.startsWith("/* ===="));
    }

    private boolean hasSignificantDifference(String local, String remote) {
        String[] localLines = local.split("\n");
        String[] remoteLines = remote.split("\n");
        
        int diffCount = 0;
        for (String line : localLines) {
            boolean found = false;
            for (String rline : remoteLines) {
                if (line.trim().equals(rline.trim())) {
                    found = true;
                    break;
                }
            }
            if (!found) diffCount++;
        }
        
        return diffCount > localLines.length * 0.3;
    }

    private void writeFile(String targetPath, String content) throws IOException {
        Path path = Paths.get(targetPath);
        Path parentDir = path.getParent();
        if (parentDir != null && !Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }
        FileUtils.writeFile(targetPath, content);
    }

    private String generateNewFileName(String originalPath, String suffix) {
        int dotIndex = originalPath.lastIndexOf('.');
        if (dotIndex > 0) {
            return originalPath.substring(0, dotIndex) + suffix + originalPath.substring(dotIndex);
        } else {
            return originalPath + suffix;
        }
    }
}
