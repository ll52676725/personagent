package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileTypeStatDTO {
    private String category;
    private String label;
    private long size;
    private long fileCount;
    private Double percentage;
    private String color;
}
