package com.example.agentplatform.agent.article.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "article")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Article {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(nullable = false, length = 256)
    private String title;
    
    @Column(length = 1024)
    private String summary;
    
    @Column(columnDefinition = "LONGTEXT")
    private String content;
    
    @Column(name = "cover_image", length = 256)
    private String coverImage;
    
    @Column(length = 512)
    private String tags;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer status = 0;
    
    @Column(name = "publish_links", columnDefinition = "JSON")
    private String publishLinks;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "published_at")
    private LocalDateTime publishedAt;
    
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