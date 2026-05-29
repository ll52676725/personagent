package com.example.agentplatform.tools.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件格式转换结果DTO
 * 用于返回文件转换后的结果信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileConvertResultDTO {

    /**
     * 原始文件格式
     */
    private String originalFormat;

    /**
     * 目标文件格式
     */
    private String targetFormat;

    /**
     * 原始文件名
     */
    private String originalFileName;

    /**
     * 转换后的文件名
     */
    private String convertedFileName;

    /**
     * 原始文件大小（字节）
     */
    private Long originalSize;

    /**
     * 转换后文件大小（字节）
     */
    private Long convertedSize;

    /**
     * 文件MIME类型
     */
    private String mimeType;

    /**
     * 转换后文件的Base64编码数据
     */
    private String fileDataBase64;

    /**
     * 原始文件数据（不序列化到JSON）
     */
    @JsonIgnore
    private byte[] rawFileData;
}
