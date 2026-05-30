package com.example.agentplatform.agent.rules.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 规则拉取日志实体类
 * <p>记录每次规则拉取操作的详细信息，包括冲突检测结果和处理过程
 * <p>用于审计追踪和冲突历史追溯
 * 
 * @author System
 * @since 2025-01-01
 */
@Entity
@Table(name = "rule_pull_log")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RulePullLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "config_id")
    private Long configId;

    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "operation_type", nullable = false, length = 32)
    private String operationType;

    @Column(name = "target_path", length = 512)
    private String targetPath;

    @Column(name = "has_conflict", nullable = false)
    @Builder.Default
    private Boolean hasConflict = false;

    @Column(name = "conflict_type", length = 32)
    private String conflictType;

    @Column(name = "conflict_strategy", length = 32)
    private String conflictStrategy;

    @Column(name = "local_content", columnDefinition = "LONGTEXT")
    private String localContent;

    @Column(name = "remote_content", columnDefinition = "LONGTEXT")
    private String remoteContent;

    @Column(name = "merged_content", columnDefinition = "LONGTEXT")
    private String mergedContent;

    @Column(name = "diff_result", columnDefinition = "LONGTEXT")
    private String diffResult;

    @Column(name = "merge_successful")
    private Boolean mergeSuccessful;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    @Column(name = "error_message", length = 1024)
    private String errorMessage;

    @Column(name = "operator", length = 64)
    private String operator;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
