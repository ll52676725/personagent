package com.example.agentplatform.agent.rules.repository;

import com.example.agentplatform.agent.rules.entity.RuleTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 规则模板数据访问层
 * 
 * @author System
 * @since 2025-01-01
 */
@Repository
public interface RuleTemplateRepository extends JpaRepository<RuleTemplate, Long> {

    List<RuleTemplate> findByUserIdOrIsSystemTrue(Long userId);

    List<RuleTemplate> findByUserIdAndCategory(Long userId, String category);

    List<RuleTemplate> findByCategoryAndIsSystemTrue(String category);

    List<RuleTemplate> findByIsSystemTrueAndStatus(Integer status);

    List<RuleTemplate> findByIsPublicTrueAndStatus(Integer status);

    List<RuleTemplate> findByUserIdAndStatus(Long userId, Integer status);

    List<RuleTemplate> findByTargetTool(String targetTool);
}
