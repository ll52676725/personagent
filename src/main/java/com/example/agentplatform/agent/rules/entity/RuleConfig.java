package com.example.agentplatform.agent.rules.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 规则配置实体类
 * <p>存储实际应用到项目中的规则文件配置
 * <p>记录规则与项目的关联关系，以及冲突处理策略
 * 
 * @author System
 * @since 2025-01-01
 */
@Entity
@Table(name = "rule_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "project_path", length = 512)
    private String projectPath;

    @Column(name = "category", nullable = false, length = 32)
    private String category;

    @Column(name = "target_tool", length = 32)
    private String targetTool;

    @Column(name = "file_name", nullable = false, length = 128)
    private String fileName;

    @Column(name = "target_path", nullable = false, length = 512)
    private String targetPath;

    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "conflict_strategy", length = 32)
    @Builder.Default
    private String conflictStrategy = "ASK";

    @Column(name = "file_hash", length = 64)
    private String fileHash;

    @Column(name = "last_pull_at")
    private LocalDateTime lastPullAt;

    @Column(name = "pull_count", nullable = false)
    @Builder.Default
    private Integer pullCount = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private Integer status = 1;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
