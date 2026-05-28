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
public class ImageFormatInfoDTO {

    private String formatName;

    private List<String> extensions;

    private String mimeType;

    private boolean readable;

    private boolean writable;

    private String description;
}
