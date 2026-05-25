package com.example.agentplatform.platform.service.impl;

import com.example.agentplatform.platform.entity.AgentPermission;
import com.example.agentplatform.common.exception.BusinessException;
import com.example.agentplatform.platform.repository.AgentPermissionRepository;
import com.example.agentplatform.platform.repository.AgentRepository;
import com.example.agentplatform.platform.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {
    
    private final AgentPermissionRepository permissionRepository;
    private final AgentRepository agentRepository;
    
    @Override
    @Transactional
    public AgentPermission applyForAgent(Long userId, Long agentId, String reason) {
        if (!agentRepository.existsById(agentId)) {
            throw new BusinessException("Agent不存在");
        }
        
        Optional<AgentPermission> existing = permissionRepository
                .findByUserIdAndAgentId(userId, agentId);
        
        if (existing.isPresent()) {
            AgentPermission perm = existing.get();
            if (perm.getStatus() == 0) {
                throw new BusinessException("申请正在审核中");
            } else if (perm.getStatus() == 1) {
                throw new BusinessException("您已有该Agent权限");
            }
        }
        
        AgentPermission permission = AgentPermission.builder()
                .userId(userId)
                .agentId(agentId)
                .status(0)
                .applyReason(reason)
                .build();
        
        AgentPermission saved = permissionRepository.save(permission);
        log.info("用户 {} 申请Agent {} 权限", userId, agentId);
        return saved;
    }
    
    @Override
    public AgentPermission getPermissionStatus(Long userId, Long agentId) {
        return permissionRepository.findByUserIdAndAgentId(userId, agentId)
                .orElse(null);
    }
    
    @Override
    public List<AgentPermission> getUserPermissions(Long userId) {
        return permissionRepository.findByUserId(userId);
    }
    
    @Override
    @Transactional
    public void approvePermission(Long permissionId, Long approverId) {
        AgentPermission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new BusinessException("权限申请不存在"));
        
        if (permission.getStatus() != 0) {
            throw new BusinessException("申请状态不正确");
        }
        
        permission.setStatus(1);
        permission.setApprovedAt(LocalDateTime.now());
        permissionRepository.save(permission);
        log.info("审批员 {} 通过权限申请 {}", approverId, permissionId);
    }
    
    @Override
    @Transactional
    public void rejectPermission(Long permissionId, String reason) {
        AgentPermission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new BusinessException("权限申请不存在"));
        
        if (permission.getStatus() != 0) {
            throw new BusinessException("申请状态不正确");
        }
        
        permission.setStatus(2);
        permission.setRejectReason(reason);
        permission.setApprovedAt(LocalDateTime.now());
        permissionRepository.save(permission);
        log.info("拒绝权限申请 {}, 原因: {}", permissionId, reason);
    }
    
    @Override
    public boolean hasPermission(Long userId, Long agentId) {
        return permissionRepository.existsByUserIdAndAgentIdAndStatus(userId, agentId, 1);
    }
}