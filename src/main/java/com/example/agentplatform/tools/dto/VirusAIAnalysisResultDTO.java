package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI分析结果数据传输对象
 * <p>用于封装AI对系统安全状态的完整分析结果，包括安全评估、优化建议、风险统计等
 * <p>这是AI分析接口的主要返回数据结构，包含智能分析的全部输出内容
 * 
 * @author System
 * @since 2025-01-01
 * @see VirusAISuggestionDTO
 * @see com.example.agentplatform.tools.service.VirusDetectionService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VirusAIAnalysisResultDTO {
    
    /**
     * 分析结果摘要，简要总结系统整体安全状况
     */
    private String summary;
    
    /**
     * 系统健康评分（AI评估），范围0-100
     */
    private String systemHealthScore;
    
    /**
     * 系统健康等级：EXCELLENT(优秀)/GOOD(良好)/FAIR(一般)/POOR(较差)/CRITICAL(危险)
     */
    private String systemHealthLevel;
    
    /**
     * AI分析洞察，深入分析系统存在的安全隐患和潜在风险
     */
    private String analysisInsight;
    
    /**
     * 安全评估结论，AI对系统整体安全性的综合评价
     */
    private String securityAssessment;
    
    /**
     * 优化建议，AI给出的系统安全和性能优化建议
     */
    private String optimizationAdvice;
    
    /**
     * 发现的安全问题总数
     */
    private Integer totalIssues;
    
    /**
     * 高危问题数量（CRITICAL + HIGH级别）
     */
    private Integer highRiskCount;
    
    /**
     * 中危问题数量（MEDIUM级别）
     */
    private Integer mediumRiskCount;
    
    /**
     * 低危问题数量（LOW级别）
     */
    private Integer lowRiskCount;
    
    /**
     * AI生成的安全建议列表，按优先级排序
     */
    private List<VirusAISuggestionDTO> suggestions;
    
    /**
     * 使用的AI模型名称，如：gpt-4o-mini、qwen-plus等
     */
    private String model;
    
    /**
     * 消耗的token数量，用于统计和计费
     */
    private Integer tokens;
    
    /**
     * AI分析耗时（毫秒），用于性能监控
     */
    private Long analysisDurationMs;
}
