package com.example.agentplatform.platform.repository;

import com.example.agentplatform.platform.entity.AgentPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentPermissionRepository extends JpaRepository<AgentPermission, Long> {
    Optional<AgentPermission> findByUserIdAndAgentId(Long userId, Long agentId);
    List<AgentPermission> findByUserId(Long userId);
    List<AgentPermission> findByAgentId(Long agentId);
    List<AgentPermission> findByStatus(Integer status);
    boolean existsByUserIdAndAgentIdAndStatus(Long userId, Long agentId, Integer status);
}