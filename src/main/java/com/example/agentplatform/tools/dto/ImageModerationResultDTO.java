package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 图片内容检测结果DTO
 * <p>封装完整的图片内容检测结果，包含总体结论、各分类检测详情、处理建议等
 * 
 * @author System
 * @since 2025-06-02
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageModerationResultDTO {

    /**
     * 检测任务唯一标识
     */
    private String taskId;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件大小，单位：字节
     */
    private Long fileSize;

    /**
     * 图片MIME类型
     */
    private String mimeType;

    /**
     * 图片宽度，单位：像素
     */
    private Integer width;

    /**
     * 图片高度，单位：像素
     */
    private Integer height;

    /**
     * 检测是否成功完成
     */
    private Boolean success;

    /**
     * 总体检测结论：pass-通过、review-待复审、block-拦截
     */
    private String conclusion;

    /**
     * 总体结论中文描述
     */
    private String conclusionLabel;

    /**
     * 是否检测到违规内容（只要有一个分类检测到违规即为true）
     */
    private Boolean hasViolation;

    /**
     * 总体风险等级：high-高危、medium-中危、low-低危、safe-安全
     */
    private String overallRiskLevel;

    /**
     * 最高置信度，所有分类中的最高置信度值，0-100
     */
    private Integer maxConfidence;

    /**
     * 涉黄内容检测详情
     */
    private ModerationCategoryDetailDTO pornographyResult;

    /**
     * 涉政内容检测详情
     */
    private ModerationCategoryDetailDTO politicalResult;

    /**
     * 涉爆/暴力内容检测详情
     */
    private ModerationCategoryDetailDTO violenceResult;

    /**
     * 其他违规内容检测详情
     */
    private ModerationCategoryDetailDTO otherResult;

    /**
     * 检测到的所有违规标签汇总
     */
    private List<String> allViolationLabels;

    /**
     * 审核建议，描述应该如何处理此图片
     */
    private String suggestion;

    /**
     * 详细的检测说明，供审核人员参考
     */
    private String auditNote;

    /**
     * 使用的AI模型名称
     */
    private String model;

    /**
     * Token消耗数量（AI模式下）
     */
    private Integer tokens;

    /**
     * 是否为降级模式返回的结果
     */
    private Boolean fallback;

    /**
     * 检测耗时，单位：毫秒
     */
    private Long detectionDurationMs;

    /**
     * 检测完成时间，ISO格式字符串
     */
    private String detectionTime;

    /**
     * 免责声明
     */
    private String disclaimer;
}
