package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NetworkInterfaceDTO {
    private String name;
    private String displayName;
    private String ipv4Address;
    private String ipv6Address;
    private String subnetMask;
    private String macAddress;
    private boolean up;
    private boolean loopback;
    private int mtu;
}
