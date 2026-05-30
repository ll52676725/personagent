package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 病毒扫描结果数据传输对象
 * <p>用于封装完整的系统安全扫描结果，包括健康评分、统计数据、问题列表等
 * <p>这是系统扫描接口的主要返回数据结构，包含前端展示所需的全部信息
 * 
 * @author System
 * @since 2025-01-01
 * @see SuspiciousProgramDTO
 * @see VulnerabilityDTO
 * @see com.example.agentplatform.tools.service.VirusDetectionService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VirusScanResultDTO {
    
    /**
     * 扫描结果摘要，简要说明本次扫描发现的主要问题
     */
    private String summary;
    
    /**
     * 系统健康等级：EXCELLENT(优秀)/GOOD(良好)/FAIR(一般)/POOR(较差)/CRITICAL(危险)
     */
    private String systemHealthLevel;
    
    /**
     * 系统健康评分，范围0-100，分数越高表示系统越安全
     * <p>评分规则：满分100，高危问题扣10分，中危扣5分，低危扣2分
     */
    private Integer systemHealthScore;
    
    /**
     * 扫描耗时（毫秒），用于性能监控和用户体验展示
     */
    private Long scanDurationMs;
    
    /**
     * 发现的可疑程序总数
     */
    private Integer totalSuspiciousPrograms;
    
    /**
     * 发现的安全漏洞总数
     */
    private Integer totalVulnerabilities;
    
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
     * 可疑程序分类统计，key为分类编码，value为数量
     * <p>如：{"PROCESS": 5, "STARTUP": 3, "SERVICE": 2}
     */
    private Map<String, Integer> programCategoryStats;
    
    /**
     * 安全漏洞分类统计，key为分类编码，value为数量
     * <p>如：{"SYSTEM_VULNERABILITY": 2, "OUTDATED_SOFTWARE": 4}
     */
    private Map<String, Integer> vulnerabilityCategoryStats;
    
    /**
     * 风险等级统计，key为等级编码，value为数量
     * <p>如：{"CRITICAL": 1, "HIGH": 3, "MEDIUM": 5, "LOW": 8}
     */
    private Map<String, Integer> severityStats;
    
    /**
     * 可疑程序详情列表
     */
    private List<SuspiciousProgramDTO> suspiciousPrograms;
    
    /**
     * 安全漏洞详情列表
     */
    private List<VulnerabilityDTO> vulnerabilities;
    
    /**
     * 免责声明，说明本工具的局限性和使用注意事项
     */
    private String disclaimer;
    
    /**
     * 扫描完成时间，格式：yyyy-MM-dd HH:mm:ss
     */
    private String scanTime;
}
