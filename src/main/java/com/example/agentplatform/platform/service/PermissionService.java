package com.example.agentplatform.platform.service;

import com.example.agentplatform.platform.entity.AgentPermission;

import java.util.List;

public interface PermissionService {
    AgentPermission applyForAgent(Long userId, Long agentId, String reason);
    AgentPermission getPermissionStatus(Long userId, Long agentId);
    List<AgentPermission> getUserPermissions(Long userId);
    void approvePermission(Long permissionId, Long approverId);
    void rejectPermission(Long permissionId, String reason);
    boolean hasPermission(Long userId, Long agentId);
}