package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 可疑程序数据传输对象
 * <p>用于封装系统扫描中发现的可疑程序或进程的详细信息，包括基本信息、风险评估、数字签名等
 * <p>支持的分类包括：系统进程、启动项、服务、计划任务、临时文件等
 * 
 * @author System
 * @since 2025-01-01
 * @see VirusScanResultDTO
 * @see com.example.agentplatform.tools.service.VirusDetectionService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuspiciousProgramDTO {
    
    /**
     * 唯一标识符，用于前端选中和批量操作
     */
    private String id;
    
    /**
     * 程序名称，即可疑程序的显示名称
     */
    private String programName;
    
    /**
     * 程序文件的完整路径，用于定位和处理
     */
    private String programPath;
    
    /**
     * 进程名称（如果是正在运行的进程）
     */
    private String processName;
    
    /**
     * 进程ID（PID），仅当程序正在运行时有值
     */
    private Integer processId;
    
    /**
     * 分类编码：PROCESS/STARTUP/SERVICE/SCHEDULED_TASK/TEMP_FILE等
     */
    private String category;
    
    /**
     * 分类标签，用于前端显示：进程/启动项/服务/计划任务/临时文件等
     */
    private String categoryLabel;
    
    /**
     * 风险等级：CRITICAL/HIGH/MEDIUM/LOW/SAFE
     */
    private String severity;
    
    /**
     * 程序描述信息，说明程序的用途和功能
     */
    private String description;
    
    /**
     * 风险原因说明，为什么被判定为可疑程序
     */
    private String riskReason;
    
    /**
     * 可疑行为列表，列举该程序的可疑行为特征
     */
    private List<String> suspiciousBehaviors;
    
    /**
     * 注册表路径（如果是通过注册表启动的程序）
     */
    private String registryPath;
    
    /**
     * 启动类型：自动/手动/禁用（仅适用于服务和启动项）
     */
    private String startupType;
    
    /**
     * 公司/发行商名称，从文件属性中获取
     */
    private String company;
    
    /**
     * 文件版本号，从文件属性中获取
     */
    private String fileVersion;
    
    /**
     * 文件大小（字节）
     */
    private Long fileSize;
    
    /**
     * 最后修改时间，格式：yyyy-MM-dd HH:mm:ss
     */
    private String lastModified;
    
    /**
     * 数字签名信息，包括签名者和证书信息
     */
    private String digitalSignature;
    
    /**
     * 是否已进行数字签名，true表示已签名，false表示未签名或签名无效
     */
    private Boolean isSigned;
    
    /**
     * 前端选中状态标记，用于批量修复操作
     */
    private Boolean selected;
    
    /**
     * 处理建议，如：终止进程、禁用启动项、删除文件、无需处理等
     */
    private String recommendation;
}
