package com.example.agentplatform.agent.rules.service;

import com.example.agentplatform.agent.rules.dto.RuleConfigCreateDTO;
import com.example.agentplatform.agent.rules.dto.RulePullRequestDTO;
import com.example.agentplatform.agent.rules.dto.RulePullResultDTO;
import com.example.agentplatform.agent.rules.entity.RuleConfig;

import java.util.List;

/**
 * 规则配置服务接口
 * 
 * @author System
 * @since 2025-01-01
 */
public interface RuleConfigService {

    RuleConfig createConfig(Long userId, RuleConfigCreateDTO dto);

    RuleConfig updateConfig(Long userId, Long id, RuleConfigCreateDTO dto);

    void deleteConfig(Long userId, Long id);

    RuleConfig getConfig(Long userId, Long id);

    List<RuleConfig> listConfigs(Long userId);

    List<RuleConfig> listConfigsByCategory(Long userId, String category);

    List<RuleConfig> listConfigsByProject(Long userId, String projectPath);

    RulePullResultDTO pullRules(Long userId, RulePullRequestDTO dto);
}
