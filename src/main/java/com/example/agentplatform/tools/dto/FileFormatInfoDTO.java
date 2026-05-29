package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 文件格式信息DTO
 * 用于描述支持的文件格式信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileFormatInfoDTO {

    /**
     * 格式名称
     */
    private String formatName;

    /**
     * 支持的文件扩展名列表
     */
    private List<String> extensions;

    /**
     * MIME类型
     */
    private String mimeType;

    /**
     * 是否可以读取该格式
     */
    private Boolean readable;

    /**
     * 是否可以写入该格式
     */
    private Boolean writable;

    /**
     * 格式描述
     */
    private String description;
}
