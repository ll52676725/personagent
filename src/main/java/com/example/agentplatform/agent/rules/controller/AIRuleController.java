package com.example.agentplatform.agent.rules.controller;

import com.example.agentplatform.agent.rules.dto.AIRuleGenerateDTO;
import com.example.agentplatform.agent.rules.entity.RuleTemplate;
import com.example.agentplatform.agent.rules.service.AIRuleGenerationService;
import com.example.agentplatform.common.dto.Result;
import com.example.agentplatform.common.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AI规则生成控制器
 * <p>基于大模型AI生成各类编码规则模板
 * <p>支持多种规则类型：全局规则、项目规则、编码规范、文档规范、AI工具配置等
 * 
 * @author System
 * @since 2025-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agent-rules/ai")
@RequiredArgsConstructor
public class AIRuleController {

    private final AIRuleGenerationService aiRuleGenerationService;

    /**
     * 生成规则内容（仅预览）
     * <p>根据用户需求描述，使用AI生成规则内容，不保存到数据库
     *
     * @param dto 生成请求，包含规则类型、描述、目标工具等
     * @return 生成的规则内容（Markdown格式）
     */
    @PostMapping("/generate")
    public ResponseEntity<Result<String>> generateRules(@Valid @RequestBody AIRuleGenerateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("用户 {} 请求AI生成规则，分类: {}, 工具: {}", userId, dto.getCategory(), dto.getTargetTool());
        
        String content = aiRuleGenerationService.generateRules(dto);
        
        return ResponseEntity.ok(Result.success("规则生成成功", content));
    }

    /**
     * 生成规则并保存为模板
     * <p>使用AI生成规则内容，并保存为用户的规则模板
     *
     * @param dto 生成请求
     * @return 保存后的规则模板实体
     */
    @PostMapping("/generate/save")
    public ResponseEntity<Result<RuleTemplate>> generateAndSave(@Valid @RequestBody AIRuleGenerateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("用户 {} 请求AI生成并保存规则模板，分类: {}", userId, dto.getCategory());
        
        RuleTemplate template = aiRuleGenerationService.generateAndSaveTemplate(userId, dto);
        
        return ResponseEntity.ok(Result.success("规则模板生成并保存成功", template));
    }

    /**
     * 快速生成指定分类的规则
     * <p>简化接口，根据分类和简单描述快速生成规则
     *
     * @param category 规则分类：global/project/coding_standard/documentation/ai_tool
     * @param description 规则描述
     * @param targetTool 目标AI工具（可选）
     * @return 生成的规则内容
     */
    @GetMapping("/quick")
    public ResponseEntity<Result<String>> quickGenerate(
            @RequestParam String category,
            @RequestParam String description,
            @RequestParam(required = false) String targetTool) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("用户 {} 快速生成规则，分类: {}", userId, category);
        
        String content = aiRuleGenerationService.generateRulesByCategory(category, targetTool, description);
        
        return ResponseEntity.ok(Result.success(content));
    }
}
