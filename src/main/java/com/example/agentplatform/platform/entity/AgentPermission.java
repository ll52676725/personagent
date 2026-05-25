package com.example.agentplatform.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_permission")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentPermission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "agent_id", nullable = false)
    private Long agentId;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer status = 0;
    
    @Column(name = "apply_reason", length = 512)
    private String applyReason;
    
    @Column(name = "reject_reason", length = 512)
    private String rejectReason;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}