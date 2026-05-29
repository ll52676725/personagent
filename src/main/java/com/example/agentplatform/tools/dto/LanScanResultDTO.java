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
public class LanScanResultDTO {
    private String subnet;
    private String interfaceName;
    private List<ArpEntryDTO> arpEntries;
    private int totalDevices;
    private long scanDurationMs;
}
