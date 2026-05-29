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
public class DriveAnalysisResultDTO {
    private DriveInfoDTO driveInfo;
    private List<FileTypeStatDTO> fileTypeStats;
    private List<FolderStatDTO> topFolders;
    private List<CleanupSuggestionDTO> cleanupSuggestions;
    private long totalScannedSize;
    private int totalScannedFiles;
    private long scanDurationMs;
}
