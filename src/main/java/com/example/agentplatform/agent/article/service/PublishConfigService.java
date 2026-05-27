package com.example.agentplatform.agent.article.service;

import com.example.agentplatform.agent.article.dto.PublishConfigDTO;
import com.example.agentplatform.agent.article.entity.PublishConfig;
import com.example.agentplatform.agent.article.repository.PublishConfigRepository;
import com.example.agentplatform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublishConfigService {

    private final PublishConfigRepository publishConfigRepository;

    public List<PublishConfig> listConfigs(Long userId) {
        return publishConfigRepository.findByUserId(userId);
    }

    public List<PublishConfig> listEnabledConfigs(Long userId) {
        return publishConfigRepository.findByUserIdAndEnabledTrue(userId);
    }

    public Optional<PublishConfig> getConfig(Long userId, String platform) {
        return publishConfigRepository.findByUserIdAndPlatform(userId, platform);
    }

    public Optional<PublishConfig> getEnabledConfig(Long userId, String platform) {
        return publishConfigRepository.findByUserIdAndPlatformAndEnabledTrue(userId, platform);
    }

    @Transactional
    public PublishConfig saveConfig(Long userId, PublishConfigDTO dto) {
        if (dto.getPlatform() == null || dto.getPlatform().isEmpty()) {
            throw new BusinessException("平台类型不能为空");
        }

        Optional<PublishConfig> existingOpt = publishConfigRepository
                .findByUserIdAndPlatform(userId, dto.getPlatform());

        PublishConfig config;
        if (existingOpt.isPresent()) {
            config = existingOpt.get();
            updateConfigFields(config, dto);
        } else {
            config = PublishConfig.builder()
                    .userId(userId)
                    .platform(dto.getPlatform())
                    .build();
            updateConfigFields(config, dto);
        }

        return publishConfigRepository.save(config);
    }

    @Transactional
    public void deleteConfig(Long userId, String platform) {
        Optional<PublishConfig> configOpt = publishConfigRepository
                .findByUserIdAndPlatform(userId, platform);

        configOpt.ifPresent(publishConfigRepository::delete);
    }

    @Transactional
    public PublishConfig updateConfigStatus(Long userId, String platform, boolean enabled) {
        PublishConfig config = publishConfigRepository
                .findByUserIdAndPlatform(userId, platform)
                .orElseThrow(() -> new BusinessException("配置不存在"));

        config.setEnabled(enabled);
        return publishConfigRepository.save(config);
    }

    public boolean isConfigEnabled(Long userId, String platform) {
        return publishConfigRepository
                .findByUserIdAndPlatformAndEnabledTrue(userId, platform)
                .isPresent();
    }

    private void updateConfigFields(PublishConfig config, PublishConfigDTO dto) {
        if (dto.getAccountName() != null) {
            config.setAccountName(dto.getAccountName());
        }
        if (dto.getApiKey() != null) {
            config.setApiKey(dto.getApiKey());
        }
        if (dto.getApiSecret() != null) {
            config.setApiSecret(dto.getApiSecret());
        }
        if (dto.getAccessToken() != null) {
            config.setAccessToken(dto.getAccessToken());
        }
        if (dto.getRefreshToken() != null) {
            config.setRefreshToken(dto.getRefreshToken());
        }
        if (dto.getExpiresAt() != null) {
            config.setExpiresAt(dto.getExpiresAt());
        }
        if (dto.getConfig() != null) {
            config.setConfig(dto.getConfig());
        }
        if (dto.getEnabled() != null) {
            config.setEnabled(dto.getEnabled());
        }
    }
}
