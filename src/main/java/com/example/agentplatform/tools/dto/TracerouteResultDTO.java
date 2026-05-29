package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TracerouteResultDTO {
    private String target;
    private String targetIp;
    private List<TracerouteHopDTO> hops;
    private int totalHops;
    private boolean reachedTarget;
    private Integer blockHop;
    private String blockIp;
    private String blockAnalysis;
    private long durationMs;
    private String errorMessage;
}
