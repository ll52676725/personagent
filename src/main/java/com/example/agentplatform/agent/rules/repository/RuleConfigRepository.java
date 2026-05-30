package com.example.agentplatform.agent.rules.repository;

import com.example.agentplatform.agent.rules.entity.RuleConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 规则配置数据访问层
 * 
 * @author System
 * @since 2025-01-01
 */
@Repository
public interface RuleConfigRepository extends JpaRepository<RuleConfig, Long> {

    List<RuleConfig> findByUserId(Long userId);

    List<RuleConfig> findByUserIdAndCategory(Long userId, String category);

    List<RuleConfig> findByUserIdAndIsActiveTrue(Long userId);

    Optional<RuleConfig> findByUserIdAndTargetPath(Long userId, String targetPath);

    List<RuleConfig> findByUserIdAndProjectPath(Long userId, String projectPath);
}
