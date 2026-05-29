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
public class CurrentIpInfoDTO {
    private String publicIp;
    private String publicIpSource;
    private String hostname;
    private List<NetworkInterfaceDTO> networkInterfaces;
    private String defaultGateway;
    private String dnsServer;
}
