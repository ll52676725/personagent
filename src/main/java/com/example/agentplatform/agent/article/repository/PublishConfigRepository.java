package com.example.agentplatform.agent.article.repository;

import com.example.agentplatform.agent.article.entity.PublishConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PublishConfigRepository extends JpaRepository<PublishConfig, Long> {

    List<PublishConfig> findByUserId(Long userId);

    Optional<PublishConfig> findByUserIdAndPlatform(Long userId, String platform);

    Optional<PublishConfig> findByUserIdAndPlatformAndEnabledTrue(Long userId, String platform);

    List<PublishConfig> findByUserIdAndEnabledTrue(Long userId);

    boolean existsByUserIdAndPlatform(Long userId, String platform);
}
