package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PingResultDTO {
    private String target;
    private boolean reachable;
    private String ipAddress;
    private long pingTimeMs;
    private Integer ttl;
    private int packetsSent;
    private int packetsReceived;
    private double packetLossRate;
    private String errorMessage;
}
