package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DnsResultDTO {
    private String domain;
    private String resolvedIp;
    private boolean success;
    private String dnsServer;
    private long queryTimeMs;
    private String errorMessage;
}
