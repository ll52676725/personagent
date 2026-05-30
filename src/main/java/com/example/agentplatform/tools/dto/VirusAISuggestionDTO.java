package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI安全建议数据传输对象
 * <p>用于封装AI智能分析生成的单条安全建议，包括问题分类、风险评估、处理方案等
 * <p>每条建议针对一类安全问题，提供可执行的修复步骤和预防措施
 * 
 * @author System
 * @since 2025-01-01
 * @see VirusAIAnalysisResultDTO
 * @see com.example.agentplatform.tools.service.VirusDetectionService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VirusAISuggestionDTO {
    
    /**
     * 建议分类编码：MALWARE_REMOVAL/SECURITY_CONFIG/VULNERABILITY_FIX/PERFORMANCE_OPTIMIZATION等
     */
    private String category;
    
    /**
     * 建议分类标签，用于前端显示：恶意软件清除/安全配置/漏洞修复/性能优化等
     */
    private String categoryLabel;
    
    /**
     * 建议标题，简要说明需要处理的问题
     */
    private String title;
    
    /**
     * 问题详细描述，说明此问题的影响和严重性
     */
    private String description;
    
    /**
     * 受影响的程序或组件名称列表
     */
    private List<String> affectedPrograms;
    
    /**
     * 此类问题的数量统计
     */
    private Integer issueCount;
    
    /**
     * 风险等级：CRITICAL/HIGH/MEDIUM/LOW
     */
    private String riskLevel;
    
    /**
     * 建议采取的行动：立即处理/尽快处理/观察/无需处理
     */
    private String action;
    
    /**
     * 建议理由，说明为什么需要采取此行动
     */
    private String reason;
    
    /**
     * 处理优先级，数字越小优先级越高，范围1-5
     */
    private Integer priority;
    
    /**
     * 问题影响说明，描述如果不处理可能造成的后果
     */
    private String impact;
    
    /**
     * 预防措施，说明如何避免此类问题再次发生
     */
    private String precaution;
    
    /**
     * 修复步骤列表，按顺序列出解决此问题需要执行的具体操作
     */
    private List<String> remediationSteps;
}
