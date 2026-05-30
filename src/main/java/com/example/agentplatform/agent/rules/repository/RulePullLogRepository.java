package com.example.agentplatform.agent.rules.repository;

import com.example.agentplatform.agent.rules.entity.RulePullLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 规则拉取日志数据访问层
 * 
 * @author System
 * @since 2025-01-01
 */
@Repository
public interface RulePullLogRepository extends JpaRepository<RulePullLog, Long> {

    List<RulePullLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<RulePullLog> findByConfigIdOrderByCreatedAtDesc(Long configId);

    List<RulePullLog> findByUserIdAndHasConflictTrueOrderByCreatedAtDesc(Long userId);

    List<RulePullLog> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);
}
