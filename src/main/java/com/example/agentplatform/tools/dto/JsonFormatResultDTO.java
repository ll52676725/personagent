package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * JSON格式化结果DTO
 * 用于返回JSON格式化操作的结果，包括格式化后的内容、错误信息和AI修复建议
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JsonFormatResultDTO {

    /**
     * 格式化是否成功
     */
    private Boolean success;

    /**
     * 格式化后的JSON字符串
     * 格式化成功时返回
     */
    private String formattedJson;

    /**
     * 原始JSON字符串（压缩格式）
     */
    private String compactJson;

    /**
     * 错误详情列表
     * 格式化失败时返回，包含所有检测到的错误
     */
    private List<JsonErrorDetailDTO> errors;

    /**
     * AI修复后的JSON字符串
     * 当格式化失败且启用AI修复时返回
     */
    private String aiFixedJson;

    /**
     * AI修复说明
     * 描述AI对JSON进行了哪些修改
     */
    private String aiFixDescription;

    /**
     * AI修复是否成功
     */
    private Boolean aiFixSuccess;

    /**
     * 格式化使用的缩进空格数
     */
    private Integer indentSize;

    /**
     * JSON数据结构类型
     * 如：object（对象）、array（数组）、string（字符串）等
     */
    private String jsonType;

    /**
     * 数据统计信息
     * 包含键值对数量、数组长度等统计数据
     */
    private String statistics;
}
