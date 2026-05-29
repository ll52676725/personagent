package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArpEntryDTO {
    private String ipAddress;
    private String macAddress;
    private String type;
    private String interfaceName;
}
