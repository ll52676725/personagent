package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriveInfoDTO {
    private String driveLetter;
    private String displayName;
    private long totalSpace;
    private long usedSpace;
    private long freeSpace;
    private String fileSystem;
    private String driveType;
}
