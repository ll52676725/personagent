package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 修复脚本数据传输对象
 * <p>用于封装自动生成的系统修复脚本信息，包括脚本内容、使用说明、警告信息等
 * <p>脚本类型支持Windows批处理脚本(.bat)，可直接下载执行
 * 
 * @author System
 * @since 2025-01-01
 * @see GenerateRemediationScriptRequestDTO
 * @see com.example.agentplatform.tools.service.VirusDetectionService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemediationScriptDTO {
    
    /**
     * 脚本文件名，如：virus_remediation_20250101_123456.bat
     */
    private String scriptName;
    
    /**
     * 脚本内容，完整的批处理脚本代码
     */
    private String scriptContent;
    
    /**
     * 脚本类型：BAT(Windows批处理)/PS1(PowerShell)/SH(Linux Shell)
     */
    private String scriptType;
    
    /**
     * 脚本编码格式：UTF-8/GBK等
     */
    private String encoding;
    
    /**
     * 本次脚本修复的问题数量
     */
    private Integer issueCount;
    
    /**
     * 修复的问题列表，包含所有将被此脚本处理的问题描述
     */
    private List<String> fixedIssues;
    
    /**
     * 警告信息，提示用户执行脚本前需要注意的事项
     */
    private String warning;
    
    /**
     * 使用说明，指导用户如何正确使用此修复脚本
     */
    private String usageInstructions;
}
