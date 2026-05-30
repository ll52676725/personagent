package com.example.agentplatform.agent.rules.service.impl;

import com.example.agentplatform.agent.rules.dto.RuleTemplateCreateDTO;
import com.example.agentplatform.agent.rules.dto.RuleTemplateUpdateDTO;
import com.example.agentplatform.agent.rules.entity.RuleTemplate;
import com.example.agentplatform.agent.rules.repository.RuleTemplateRepository;
import com.example.agentplatform.agent.rules.service.RuleTemplateService;
import com.example.agentplatform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 规则模板服务实现类
 * 
 * @author System
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleTemplateServiceImpl implements RuleTemplateService {

    private final RuleTemplateRepository ruleTemplateRepository;

    @Override
    @Transactional
    public RuleTemplate createTemplate(Long userId, RuleTemplateCreateDTO dto) {
        log.info("【规则模板】用户 {} 创建规则模板: {}", userId, dto.getName());
        
        RuleTemplate template = RuleTemplate.builder()
                .userId(userId)
                .name(dto.getName())
                .description(dto.getDescription())
                .category(dto.getCategory())
                .sourceType(dto.getSourceType())
                .targetTool(dto.getTargetTool())
                .fileName(dto.getFileName())
                .filePath(dto.getFilePath())
                .content(dto.getContent())
                .variables(dto.getVariables())
                .version(dto.getVersion())
                .isPublic(dto.getIsPublic() != null ? dto.getIsPublic() : false)
                .isSystem(false)
                .status(1)
                .build();
        
        return ruleTemplateRepository.save(template);
    }

    @Override
    @Transactional
    public RuleTemplate updateTemplate(Long userId, Long id, RuleTemplateUpdateDTO dto) {
        log.info("【规则模板】用户 {} 更新规则模板: {}", userId, id);
        
        RuleTemplate template = getTemplate(userId, id);
        
        if (Boolean.TRUE.equals(template.getIsSystem())) {
            throw new BusinessException("系统预设模板不允许修改");
        }
        
        if (dto.getName() != null) {
            template.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            template.setDescription(dto.getDescription());
        }
        if (dto.getCategory() != null) {
            template.setCategory(dto.getCategory());
        }
        if (dto.getTargetTool() != null) {
            template.setTargetTool(dto.getTargetTool());
        }
        if (dto.getFileName() != null) {
            template.setFileName(dto.getFileName());
        }
        if (dto.getFilePath() != null) {
            template.setFilePath(dto.getFilePath());
        }
        if (dto.getContent() != null) {
            template.setContent(dto.getContent());
        }
        if (dto.getVariables() != null) {
            template.setVariables(dto.getVariables());
        }
        if (dto.getVersion() != null) {
            template.setVersion(dto.getVersion());
        }
        if (dto.getIsPublic() != null) {
            template.setIsPublic(dto.getIsPublic());
        }
        if (dto.getStatus() != null) {
            template.setStatus(dto.getStatus());
        }
        
        return ruleTemplateRepository.save(template);
    }

    @Override
    @Transactional
    public void deleteTemplate(Long userId, Long id) {
        log.info("【规则模板】用户 {} 删除规则模板: {}", userId, id);
        
        RuleTemplate template = getTemplate(userId, id);
        
        if (Boolean.TRUE.equals(template.getIsSystem())) {
            throw new BusinessException("系统预设模板不允许删除");
        }
        
        ruleTemplateRepository.delete(template);
    }

    @Override
    public RuleTemplate getTemplate(Long userId, Long id) {
        RuleTemplate template = ruleTemplateRepository.findById(id)
                .orElseThrow(() -> new BusinessException("规则模板不存在"));
        
        if (!Boolean.TRUE.equals(template.getIsSystem()) 
                && !Boolean.TRUE.equals(template.getIsPublic())
                && !userId.equals(template.getUserId())) {
            throw new BusinessException("无权限访问该模板");
        }
        
        return template;
    }

    @Override
    public List<RuleTemplate> listTemplates(Long userId) {
        return ruleTemplateRepository.findByUserIdOrIsSystemTrue(userId);
    }

    @Override
    public List<RuleTemplate> listTemplatesByCategory(Long userId, String category) {
        List<RuleTemplate> userTemplates = ruleTemplateRepository.findByUserIdAndCategory(userId, category);
        List<RuleTemplate> systemTemplates = ruleTemplateRepository.findByCategoryAndIsSystemTrue(category);
        userTemplates.addAll(systemTemplates);
        return userTemplates;
    }

    @Override
    public List<RuleTemplate> listSystemTemplates() {
        return ruleTemplateRepository.findByIsSystemTrueAndStatus(1);
    }

    @Override
    public List<RuleTemplate> listPublicTemplates() {
        return ruleTemplateRepository.findByIsPublicTrueAndStatus(1);
    }

    @Override
    @Transactional
    public RuleTemplate copyTemplate(Long userId, Long templateId) {
        log.info("【规则模板】用户 {} 复制模板: {}", userId, templateId);
        
        RuleTemplate source = ruleTemplateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException("源模板不存在"));
        
        if (!Boolean.TRUE.equals(source.getIsSystem()) 
                && !Boolean.TRUE.equals(source.getIsPublic())
                && !userId.equals(source.getUserId())) {
            throw new BusinessException("无权限复制该模板");
        }
        
        RuleTemplate copy = RuleTemplate.builder()
                .userId(userId)
                .name(source.getName() + " (副本)")
                .description(source.getDescription())
                .category(source.getCategory())
                .sourceType("copy")
                .targetTool(source.getTargetTool())
                .fileName(source.getFileName())
                .filePath(source.getFilePath())
                .content(source.getContent())
                .variables(source.getVariables())
                .version(source.getVersion())
                .isPublic(false)
                .isSystem(false)
                .status(1)
                .build();
        
        return ruleTemplateRepository.save(copy);
    }

    @Override
    @Transactional
    public void incrementUseCount(Long templateId) {
        RuleTemplate template = ruleTemplateRepository.findById(templateId).orElse(null);
        if (template != null) {
            template.setUseCount(template.getUseCount() + 1);
            ruleTemplateRepository.save(template);
        }
    }
}
