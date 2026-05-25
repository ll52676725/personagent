package com.example.agentplatform.platform.controller;

import com.example.agentplatform.platform.dto.PermissionApplyDTO;
import com.example.agentplatform.common.dto.Result;
import com.example.agentplatform.platform.entity.Agent;
import com.example.agentplatform.platform.entity.AgentPermission;
import com.example.agentplatform.platform.entity.User;
import com.example.agentplatform.platform.service.AgentService;
import com.example.agentplatform.platform.service.PermissionService;
import com.example.agentplatform.platform.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
public class AgentController {
    
    private final AgentService agentService;
    private final PermissionService permissionService;
    private final UserService userService;
    
    @GetMapping
    public ResponseEntity<Result<List<Agent>>> getAllAgents() {
        List<Agent> agents = agentService.getAllAgents();
        return ResponseEntity.ok(Result.success(agents));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Result<Agent>> getAgentById(@PathVariable Long id) {
        Agent agent = agentService.getAgentById(id);
        return ResponseEntity.ok(Result.success(agent));
    }
    
    @PostMapping("/{id}/apply")
    public ResponseEntity<Result<AgentPermission>> applyForPermission(
            @PathVariable Long id,
            @Valid @RequestBody PermissionApplyDTO dto) {
        User user = userService.getCurrentUser();
        AgentPermission permission = permissionService.applyForAgent(user.getId(), id, dto.getApplyReason());
        return ResponseEntity.ok(Result.success("申请提交成功", permission));
    }
    
    @GetMapping("/{id}/status")
    public ResponseEntity<Result<Map<String, Object>>> getPermissionStatus(@PathVariable Long id) {
        User user = userService.getCurrentUser();
        AgentPermission permission = permissionService.getPermissionStatus(user.getId(), id);
        
        Map<String, Object> result = new HashMap<>();
        if (permission != null) {
            result.put("status", permission.getStatus());
            result.put("applyReason", permission.getApplyReason());
            result.put("createdAt", permission.getCreatedAt());
        } else {
            result.put("status", -1);
            result.put("message", "未申请");
        }
        
        return ResponseEntity.ok(Result.success(result));
    }
}