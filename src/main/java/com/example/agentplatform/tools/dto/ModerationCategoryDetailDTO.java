package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 内容检测分类详情DTO
 * <p>描述某一类违规内容的检测结果，包含检测状态、置信度、违规标签等
 * 
 * @author System
 * @since 2025-06-02
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModerationCategoryDetailDTO {

    /**
     * 分类编码：pornography-涉黄、political-涉政、violence-涉爆、other-其他
     */
    private String category;

    /**
     * 分类中文名称，用于界面显示
     */
    private String categoryLabel;

    /**
     * 是否检测到违规内容
     */
    private Boolean violated;

    /**
     * 置信度，0-100，表示AI对检测结果的把握程度
     */
    private Integer confidence;

    /**
     * 风险等级：high-高危、medium-中危、low-低危
     */
    private String riskLevel;

    /**
     * 检测到的违规标签列表，如：["性感暴露", "色情暗示"]
     */
    private List<String> labels;

    /**
     * 检测说明，描述具体检测到的违规内容
     */
    private String description;
}
