package com.example.agentplatform.agent.knowledge.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Knowledge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "base_id", nullable = false)
    private Long baseId;

    @Column(nullable = false, length = 256)
    private String title;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "source_type", length = 32)
    @Builder.Default
    private String sourceType = "manual";

    @Column(name = "source_url", length = 512)
    private String sourceUrl;

    @Column(name = "file_name", length = 512)
    private String fileName;

    @Column(name = "file_type", length = 64)
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "markdown_content", columnDefinition = "LONGTEXT")
    private String markdownContent;

    @Column(name = "structured_data", columnDefinition = "LONGTEXT")
    private String structuredData;

    @Column(name = "parse_engine", length = 32)
    private String parseEngine;

    @Column(name = "image_count")
    private Integer imageCount;

    @Column(name = "table_count")
    private Integer tableCount;

    @Column(name = "formula_count")
    private Integer formulaCount;

    @Column(length = 512)
    private String tags;

    @Column(length = 64)
    private String category;

    @Column(name = "chunk_status", nullable = false)
    @Builder.Default
    private Integer chunkStatus = 0;

    @Column(name = "chunk_count", nullable = false)
    @Builder.Default
    private Integer chunkCount = 0;

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
