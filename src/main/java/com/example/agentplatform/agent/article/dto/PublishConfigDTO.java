package com.example.agentplatform.agent.article.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublishConfigDTO {

    private String platform;

    private String accountName;

    private String apiKey;

    private String apiSecret;

    private String accessToken;

    private String refreshToken;

    private LocalDateTime expiresAt;

    private String config;

    private Boolean enabled;
}
