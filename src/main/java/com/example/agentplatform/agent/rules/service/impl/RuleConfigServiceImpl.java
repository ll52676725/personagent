package com.example.agentplatform.agent.rules.service.impl;

import com.example.agentplatform.agent.rules.dto.RuleConfigCreateDTO;
import com.example.agentplatform.agent.rules.dto.RulePullRequestDTO;
import com.example.agentplatform.agent.rules.dto.RulePullResultDTO;
import com.example.agentplatform.agent.rules.entity.RuleConfig;
import com.example.agentplatform.agent.rules.entity.RulePullLog;
import com.example.agentplatform.agent.rules.entity.RuleTemplate;
import com.example.agentplatform.agent.rules.enums.ConflictStrategy;
import com.example.agentplatform.agent.rules.repository.RuleConfigRepository;
import com.example.agentplatform.agent.rules.repository.RulePullLogRepository;
import com.example.agentplatform.agent.rules.repository.RuleTemplateRepository;
import com.example.agentplatform.agent.rules.service.ConflictResolutionService;
import com.example.agentplatform.agent.rules.service.RuleConfigService;
import com.example.agentplatform.agent.rules.service.RuleTemplateService;
import com.example.agentplatform.common.exception.BusinessException;
import com.example.agentplatform.tools.common.FileUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 规则配置服务实现类
 * <p>管理规则配置的CRUD操作和规则拉取核心逻辑
 * 
 * @author System
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleConfigServiceImpl implements RuleConfigService {

    private final RuleConfigRepository ruleConfigRepository;
    private final RuleTemplateRepository ruleTemplateRepository;
    private final RulePullLogRepository rulePullLogRepository;
    private final RuleTemplateService ruleTemplateService;
    private final ConflictResolutionService conflictResolutionService;

    @Override
    @Transactional
    public RuleConfig createConfig(Long userId, RuleConfigCreateDTO dto) {
        log.info("【规则配置】用户 {} 创建规则配置: {}", userId, dto.getFileName());
        
        Optional<RuleConfig> existing = ruleConfigRepository.findByUserIdAndTargetPath(userId, dto.getTargetPath());
        if (existing.isPresent()) {
            throw new BusinessException("该目标路径已存在配置: " + dto.getTargetPath());
        }
        
        String content = dto.getContent();
        if (dto.getTemplateId() != null && (content == null || content.isEmpty())) {
            RuleTemplate template = ruleTemplateService.getTemplate(userId, dto.getTemplateId());
            content = template.getContent();
        }
        
        String fileHash = null;
        if (content != null) {
            fileHash = calculateHash(content);
        }
        
        RuleConfig config = RuleConfig.builder()
                .userId(userId)
                .templateId(dto.getTemplateId())
                .projectPath(dto.getProjectPath())
                .category(dto.getCategory())
                .targetTool(dto.getTargetTool())
                .fileName(dto.getFileName())
                .targetPath(dto.getTargetPath())
                .content(content)
                .conflictStrategy(dto.getConflictStrategy() != null ? dto.getConflictStrategy() : "ASK")
                .fileHash(fileHash)
                .pullCount(0)
                .isActive(true)
                .status(1)
                .build();
        
        return ruleConfigRepository.save(config);
    }

    @Override
    @Transactional
    public RuleConfig updateConfig(Long userId, Long id, RuleConfigCreateDTO dto) {
        log.info("【规则配置】用户 {} 更新规则配置: {}", userId, id);
        
        RuleConfig config = getConfig(userId, id);
        
        if (dto.getTemplateId() != null) {
            config.setTemplateId(dto.getTemplateId());
        }
        if (dto.getProjectPath() != null) {
            config.setProjectPath(dto.getProjectPath());
        }
        if (dto.getCategory() != null) {
            config.setCategory(dto.getCategory());
        }
        if (dto.getTargetTool() != null) {
            config.setTargetTool(dto.getTargetTool());
        }
        if (dto.getFileName() != null) {
            config.setFileName(dto.getFileName());
        }
        if (dto.getTargetPath() != null) {
            config.setTargetPath(dto.getTargetPath());
        }
        if (dto.getContent() != null) {
            config.setContent(dto.getContent());
            config.setFileHash(calculateHash(dto.getContent()));
        }
        if (dto.getConflictStrategy() != null) {
            config.setConflictStrategy(dto.getConflictStrategy());
        }
        
        return ruleConfigRepository.save(config);
    }

    @Override
    @Transactional
    public void deleteConfig(Long userId, Long id) {
        log.info("【规则配置】用户 {} 删除规则配置: {}", userId, id);
        RuleConfig config = getConfig(userId, id);
        ruleConfigRepository.delete(config);
    }

    @Override
    public RuleConfig getConfig(Long userId, Long id) {
        RuleConfig config = ruleConfigRepository.findById(id)
                .orElseThrow(() -> new BusinessException("规则配置不存在"));
        
        if (!userId.equals(config.getUserId())) {
            throw new BusinessException("无权限访问该配置");
        }
        
        return config;
    }

    @Override
    public List<RuleConfig> listConfigs(Long userId) {
        return ruleConfigRepository.findByUserId(userId);
    }

    @Override
    public List<RuleConfig> listConfigsByCategory(Long userId, String category) {
        return ruleConfigRepository.findByUserIdAndCategory(userId, category);
    }

    @Override
    public List<RuleConfig> listConfigsByProject(Long userId, String projectPath) {
        return ruleConfigRepository.findByUserIdAndProjectPath(userId, projectPath);
    }

    @Override
    @Transactional
    public RulePullResultDTO pullRules(Long userId, RulePullRequestDTO dto) {
        log.info("【规则拉取】用户 {} 拉取规则, templateId: {}, configId: {}", 
                userId, dto.getTemplateId(), dto.getConfigId());
        
        RuleConfig config = null;
        RuleTemplate template = null;
        String remoteContent = null;
        String targetPath = null;
        ConflictStrategy strategy = ConflictStrategy.ASK;
        
        if (dto.getConfigId() != null) {
            config = getConfig(userId, dto.getConfigId());
            targetPath = config.getTargetPath();
            strategy = ConflictStrategy.fromCode(dto.getConflictStrategy() != null 
                    ? dto.getConflictStrategy() : config.getConflictStrategy());
            
            if (config.getTemplateId() != null) {
                template = ruleTemplateService.getTemplate(userId, config.getTemplateId());
                remoteContent = template.getContent();
            } else if (config.getContent() != null) {
                remoteContent = config.getContent();
            }
        } else if (dto.getTemplateId() != null) {
            template = ruleTemplateService.getTemplate(userId, dto.getTemplateId());
            remoteContent = template.getContent();
            targetPath = template.getFilePath() != null ? template.getFilePath() 
                    : "./" + template.getFileName();
            strategy = ConflictStrategy.fromCode(dto.getConflictStrategy() != null 
                    ? dto.getConflictStrategy() : "ASK");
        } else {
            throw new BusinessException("必须指定templateId或configId");
        }
        
        if (remoteContent == null || remoteContent.isEmpty()) {
            throw new BusinessException("模板内容为空，无法拉取");
        }
        
        String localContent = null;
        try {
            if (FileUtils.fileExists(targetPath)) {
                localContent = FileUtils.readFile(targetPath);
            }
        } catch (IOException e) {
            log.warn("【规则拉取】读取本地文件失败: {}", e.getMessage());
        }
        
        RulePullResultDTO conflictResult = conflictResolutionService.detectConflict(
                localContent, remoteContent, targetPath);
        
        RulePullLog pullLog = RulePullLog.builder()
                .userId(userId)
                .configId(config != null ? config.getId() : null)
                .templateId(template != null ? template.getId() : null)
                .operationType("PULL")
                .targetPath(targetPath)
                .hasConflict(conflictResult.getHasConflict())
                .conflictType(conflictResult.getConflictType())
                .localContent(localContent)
                .remoteContent(remoteContent)
                .diffResult(conflictResult.getDiffResult())
                .status("PENDING")
                .build();
        
        pullLog = rulePullLogRepository.save(pullLog);
        
        if (Boolean.TRUE.equals(dto.getPreviewOnly())) {
            pullLog.setStatus("PREVIEW");
            rulePullLogRepository.save(pullLog);
            
            return RulePullResultDTO.builder()
                    .logId(pullLog.getId())
                    .configId(config != null ? config.getId() : null)
                    .status("PREVIEW")
                    .hasConflict(conflictResult.getHasConflict())
                    .conflictType(conflictResult.getConflictType())
                    .conflictStrategy(strategy.getCode())
                    .targetPath(targetPath)
                    .localContent(localContent)
                    .remoteContent(remoteContent)
                    .diffResult(conflictResult.getDiffResult())
                    .message("预览模式，未执行实际写入")
                    .build();
        }
        
        if (!conflictResult.getHasConflict()) {
            try {
                writeContentToFile(targetPath, remoteContent);
                updateConfigAfterPull(config, remoteContent);
                if (template != null) {
                    ruleTemplateService.incrementUseCount(template.getId());
                }
                
                pullLog.setStatus("SUCCESS");
                pullLog.setConflictStrategy(strategy.getCode());
                rulePullLogRepository.save(pullLog);
                
                return RulePullResultDTO.builder()
                        .logId(pullLog.getId())
                        .configId(config != null ? config.getId() : null)
                        .status("SUCCESS")
                        .hasConflict(false)
                        .conflictStrategy(strategy.getCode())
                        .targetPath(targetPath)
                        .mergedContent(remoteContent)
                        .message(localContent == null ? "新建规则文件成功" : "文件无变化，已同步")
                        .build();
            } catch (IOException e) {
                log.error("【规则拉取】写入文件失败: {}", e.getMessage(), e);
                pullLog.setStatus("FAILED");
                pullLog.setErrorMessage(e.getMessage());
                rulePullLogRepository.save(pullLog);
                throw new BusinessException("写入文件失败: " + e.getMessage());
            }
        }
        
        if (strategy == ConflictStrategy.ASK) {
            return RulePullResultDTO.builder()
                    .logId(pullLog.getId())
                    .configId(config != null ? config.getId() : null)
                    .status("CONFLICT")
                    .hasConflict(true)
                    .conflictType(conflictResult.getConflictType())
                    .conflictStrategy("ASK")
                    .targetPath(targetPath)
                    .localContent(localContent)
                    .remoteContent(remoteContent)
                    .diffResult(conflictResult.getDiffResult())
                    .message("检测到冲突，请选择处理策略")
                    .build();
        }
        
        return executeConflictStrategy(userId, pullLog, config, template, localContent, remoteContent, strategy);
    }

    private RulePullResultDTO executeConflictStrategy(Long userId, RulePullLog pullLog, 
            RuleConfig config, RuleTemplate template, String localContent, 
            String remoteContent, ConflictStrategy strategy) {
        
        try {
            String targetPath = pullLog.getTargetPath();
            String finalContent = null;
            boolean writeSuccess = false;
            
            switch (strategy) {
                case OVERWRITE:
                    finalContent = remoteContent;
                    writeContentToFile(targetPath, finalContent);
                    writeSuccess = true;
                    break;
                    
                case KEEP_LOCAL:
                    finalContent = localContent;
                    writeSuccess = true;
                    break;
                    
                case MERGE:
                    finalContent = conflictResolutionService.attemptMerge(localContent, remoteContent);
                    if (finalContent != null) {
                        writeContentToFile(targetPath, finalContent);
                        writeSuccess = true;
                        pullLog.setMergeSuccessful(true);
                    } else {
                        pullLog.setStatus("CONFLICT");
                        pullLog.setConflictStrategy(strategy.getCode());
                        pullLog.setMergeSuccessful(false);
                        rulePullLogRepository.save(pullLog);
                        
                        return RulePullResultDTO.builder()
                                .logId(pullLog.getId())
                                .configId(config != null ? config.getId() : null)
                                .status("CONFLICT")
                                .hasConflict(true)
                                .conflictType(pullLog.getConflictType())
                                .conflictStrategy(strategy.getCode())
                                .targetPath(targetPath)
                                .localContent(localContent)
                                .remoteContent(remoteContent)
                                .diffResult(pullLog.getDiffResult())
                                .message("自动合并失败，请手动处理冲突")
                                .build();
                    }
                    break;
                    
                case RENAME:
                    String newPath = generateBackupFileName(targetPath);
                    writeContentToFile(newPath, remoteContent);
                    finalContent = remoteContent;
                    targetPath = newPath;
                    writeSuccess = true;
                    break;
                    
                default:
                    break;
            }
            
            if (writeSuccess) {
                updateConfigAfterPull(config, finalContent);
                if (template != null) {
                    ruleTemplateService.incrementUseCount(template.getId());
                }
                
                pullLog.setStatus("SUCCESS");
                pullLog.setConflictStrategy(strategy.getCode());
                pullLog.setMergedContent(finalContent);
                rulePullLogRepository.save(pullLog);
                
                return RulePullResultDTO.builder()
                        .logId(pullLog.getId())
                        .configId(config != null ? config.getId() : null)
                        .status("SUCCESS")
                        .hasConflict(true)
                        .conflictType(pullLog.getConflictType())
                        .conflictStrategy(strategy.getCode())
                        .targetPath(targetPath)
                        .mergedContent(finalContent)
                        .message("规则拉取成功，策略: " + strategy.getDesc())
                        .build();
            }
            
            pullLog.setStatus("FAILED");
            pullLog.setErrorMessage("未知错误");
            rulePullLogRepository.save(pullLog);
            throw new BusinessException("规则拉取失败");
            
        } catch (IOException e) {
            log.error("【规则拉取】写入文件失败: {}", e.getMessage(), e);
            pullLog.setStatus("FAILED");
            pullLog.setErrorMessage(e.getMessage());
            rulePullLogRepository.save(pullLog);
            throw new BusinessException("写入文件失败: " + e.getMessage());
        }
    }

    private void writeContentToFile(String targetPath, String content) throws IOException {
        java.nio.file.Path path = java.nio.file.Paths.get(targetPath);
        java.nio.file.Path parentDir = path.getParent();
        if (parentDir != null && !java.nio.file.Files.exists(parentDir)) {
            java.nio.file.Files.createDirectories(parentDir);
        }
        FileUtils.writeFile(targetPath, content);
    }

    private void updateConfigAfterPull(RuleConfig config, String content) {
        if (config != null) {
            config.setContent(content);
            config.setFileHash(calculateHash(content));
            config.setLastPullAt(LocalDateTime.now());
            config.setPullCount(config.getPullCount() + 1);
            ruleConfigRepository.save(config);
        }
    }

    private String calculateHash(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private String generateBackupFileName(String originalPath) {
        int dotIndex = originalPath.lastIndexOf('.');
        String timestamp = LocalDateTime.now().toString().replace(":", "-").replace(".", "-");
        if (dotIndex > 0) {
            return originalPath.substring(0, dotIndex) + "." + timestamp + originalPath.substring(dotIndex);
        } else {
            return originalPath + "." + timestamp;
        }
    }
}
