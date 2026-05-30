package com.example.agentplatform.tools.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 生成修复脚本请求数据传输对象
 * <p>用于封装前端生成修复脚本的请求参数，指定需要修复的问题ID列表
 * <p>支持批量选择问题，生成对应的修复脚本
 * 
 * @author System
 * @since 2025-01-01
 * @see RemediationScriptDTO
 * @see com.example.agentplatform.tools.service.VirusDetectionService
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateRemediationScriptRequestDTO {
    
    /**
     * 选中的问题ID列表，包含需要生成修复脚本的所有问题的唯一标识
     * <p>ID来源包括SuspiciousProgramDTO.id和VulnerabilityDTO.id
     */
    private List<String> selectedIssueIds;
}
