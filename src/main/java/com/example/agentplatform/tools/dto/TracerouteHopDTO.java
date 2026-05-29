package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TracerouteHopDTO {
    private int hop;
    private String host;
    private String ip;
    private long latency1;
    private long latency2;
    private long latency3;
    private boolean timeout;
}
