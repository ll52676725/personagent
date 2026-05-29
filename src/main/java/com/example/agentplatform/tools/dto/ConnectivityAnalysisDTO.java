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
public class ConnectivityAnalysisDTO {
    private String sourceIp;
    private String targetIp;
    private String targetHost;
    private boolean isLan;
    private boolean pingable;
    private PingResultDTO pingResult;
    private TracerouteResultDTO tracerouteResult;
    private DnsResultDTO dnsResult;
    private String overallStatus;
    private String diagnosis;
    private List<String> suggestions;
}
