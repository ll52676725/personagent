package com.example.agentplatform.agent.rules.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 规则模板实体类
 * <p>存储预设的AI编码规则模板，支持系统预设、用户自定义、AI生成三种来源
 * <p>规则按分类管理：全局规则、项目规则、编码规范、文档规范、AI工具规则
 * 
 * @author System
 * @since 2025-01-01
 */
@Entity
@Table(name = "rule_template")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(length = 512)
    private String description;

    @Column(name = "category", nullable = false, length = 32)
    private String category;

    @Column(name = "source_type", nullable = false, length = 32)
    private String sourceType;

    @Column(name = "target_tool", length = 32)
    private String targetTool;

    @Column(name = "file_name", nullable = false, length = 128)
    private String fileName;

    @Column(name = "file_path", length = 512)
    private String filePath;

    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "variables", columnDefinition = "JSON")
    private String variables;

    @Column(name = "version", length = 32)
    private String version;

    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private Boolean isSystem = false;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    @Column(name = "use_count", nullable = false)
    @Builder.Default
    private Integer useCount = 0;

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
