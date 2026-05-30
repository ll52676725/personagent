package com.example.agentplatform.agent.rules.service;

import com.example.agentplatform.agent.rules.dto.AIRuleGenerateDTO;
import com.example.agentplatform.agent.rules.entity.RuleTemplate;

/**
 * AI规则生成服务接口
 * 
 * @author System
 * @since 2025-01-01
 */
public interface AIRuleGenerationService {

    String generateRules(AIRuleGenerateDTO dto);

    RuleTemplate generateAndSaveTemplate(Long userId, AIRuleGenerateDTO dto);

    String generateRulesByCategory(String category, String targetTool, String description);
}
