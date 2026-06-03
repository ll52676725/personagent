package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 图片内容检测请求DTO
 * <p>封装前端上传的图片检测请求参数，包含图片数据和检测配置
 * 
 * @author System
 * @since 2025-06-02
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageModerationRequestDTO {

    /**
     * 图片Base64编码数据（不包含data:image前缀）
     */
    private String imageBase64;

    /**
     * 原始文件名，用于日志记录和结果展示
     */
    private String fileName;

    /**
     * 文件大小，单位：字节
     */
    private Long fileSize;

    /**
     * 图片MIME类型，如：image/jpeg、image/png等
     */
    private String mimeType;

    /**
     * 是否检测涉黄内容，默认true
     */
    @Builder.Default
    private Boolean detectPornography = true;

    /**
     * 是否检测涉政内容，默认true
     */
    @Builder.Default
    private Boolean detectPolitical = true;

    /**
     * 是否检测涉爆/暴力内容，默认true
     */
    @Builder.Default
    private Boolean detectViolence = true;

    /**
     * 是否检测其他违规内容（赌博、毒品、烟酒等），默认true
     */
    @Builder.Default
    private Boolean detectOther = true;

    /**
     * 检测敏感度阈值，0-100，数值越高越严格，默认80
     */
    @Builder.Default
    private Integer sensitivityThreshold = 80;
}
