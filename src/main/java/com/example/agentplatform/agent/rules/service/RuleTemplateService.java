package com.example.agentplatform.agent.rules.service;

import com.example.agentplatform.agent.rules.dto.RuleTemplateCreateDTO;
import com.example.agentplatform.agent.rules.dto.RuleTemplateUpdateDTO;
import com.example.agentplatform.agent.rules.entity.RuleTemplate;

import java.util.List;

/**
 * 规则模板服务接口
 * 
 * @author System
 * @since 2025-01-01
 */
public interface RuleTemplateService {

    RuleTemplate createTemplate(Long userId, RuleTemplateCreateDTO dto);

    RuleTemplate updateTemplate(Long userId, Long id, RuleTemplateUpdateDTO dto);

    void deleteTemplate(Long userId, Long id);

    RuleTemplate getTemplate(Long userId, Long id);

    List<RuleTemplate> listTemplates(Long userId);

    List<RuleTemplate> listTemplatesByCategory(Long userId, String category);

    List<RuleTemplate> listSystemTemplates();

    List<RuleTemplate> listPublicTemplates();

    RuleTemplate copyTemplate(Long userId, Long templateId);

    void incrementUseCount(Long templateId);
}
