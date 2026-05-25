package com.example.agentplatform.platform.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "sys_agent")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Agent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 64)
    private String name;
    
    @Column(unique = true, nullable = false, length = 32)
    private String code;
    
    @Column(length = 512)
    private String description;
    
    @Column(name = "module_name", nullable = false, length = 64)
    private String moduleName;
    
    @Column(length = 256)
    private String icon;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer status = 1;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}