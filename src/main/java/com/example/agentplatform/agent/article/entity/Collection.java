package com.example.agentplatform.agent.article.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "article_collection")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Collection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 256)
    private String title;

    @Column(name = "cover_image", length = 256)
    private String coverImage;

    @Column(columnDefinition = "LONGTEXT")
    private String description;

    @Column(columnDefinition = "JSON")
    private String outlines;

    @Column(name = "article_count", nullable = false)
    @Builder.Default
    private Integer articleCount = 0;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private Integer status = 0;

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