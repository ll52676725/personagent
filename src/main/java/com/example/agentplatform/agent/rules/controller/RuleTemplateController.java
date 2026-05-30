package com.example.agentplatform.agent.rules.controller;

import com.example.agentplatform.agent.rules.dto.RuleTemplateCreateDTO;
import com.example.agentplatform.agent.rules.dto.RuleTemplateUpdateDTO;
import com.example.agentplatform.agent.rules.entity.RuleTemplate;
import com.example.agentplatform.agent.rules.service.RuleTemplateService;
import com.example.agentplatform.common.dto.Result;
import com.example.agentplatform.common.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 规则模板管理控制器
 * <p>提供规则模板的CRUD操作接口
 * <p>规则模板是预设的AI编码规则，可复用、可分享、可AI生成
 * 
 * @author System
 * @since 2025-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agent-rules/templates")
@RequiredArgsConstructor
public class RuleTemplateController {

    private final RuleTemplateService ruleTemplateService;

    /**
     * 创建规则模板
     * <p>用户可以创建自定义的规则模板，用于后续的规则拉取
     *
     * @param dto 模板创建请求
     * @return 创建的模板实体
     */
    @PostMapping
    public ResponseEntity<Result<RuleTemplate>> createTemplate(@Valid @RequestBody RuleTemplateCreateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        RuleTemplate template = ruleTemplateService.createTemplate(userId, dto);
        log.info("用户 {} 创建规则模板成功: {}", userId, template.getId());
        return ResponseEntity.ok(Result.success("模板创建成功", template));
    }

    /**
     * 更新规则模板
     * <p>更新自定义模板的内容和配置，系统模板不允许修改
     *
     * @param id 模板ID
     * @param dto 更新内容
     * @return 更新后的模板
     */
    @PutMapping("/{id}")
    public ResponseEntity<Result<RuleTemplate>> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody RuleTemplateUpdateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        RuleTemplate template = ruleTemplateService.updateTemplate(userId, id, dto);
        log.info("用户 {} 更新规则模板: {}", userId, id);
        return ResponseEntity.ok(Result.success("模板更新成功", template));
    }

    /**
     * 删除规则模板
     * <p>删除自定义模板，系统模板不允许删除
     *
     * @param id 模板ID
     * @return 空结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Result<Void>> deleteTemplate(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        ruleTemplateService.deleteTemplate(userId, id);
        log.info("用户 {} 删除规则模板: {}", userId, id);
        return ResponseEntity.ok(Result.success("模板删除成功", null));
    }

    /**
     * 获取单个模板详情
     *
     * @param id 模板ID
     * @return 模板详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<RuleTemplate>> getTemplate(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        RuleTemplate template = ruleTemplateService.getTemplate(userId, id);
        return ResponseEntity.ok(Result.success(template));
    }

    /**
     * 获取用户可用的模板列表
     * <p>包括用户自定义模板和系统预设模板
     *
     * @return 模板列表
     */
    @GetMapping
    public ResponseEntity<Result<List<RuleTemplate>>> listTemplates() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<RuleTemplate> templates = ruleTemplateService.listTemplates(userId);
        return ResponseEntity.ok(Result.success(templates));
    }

    /**
     * 按分类获取模板列表
     *
     * @param category 规则分类
     * @return 模板列表
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<Result<List<RuleTemplate>>> listTemplatesByCategory(@PathVariable String category) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<RuleTemplate> templates = ruleTemplateService.listTemplatesByCategory(userId, category);
        return ResponseEntity.ok(Result.success(templates));
    }

    /**
     * 获取系统预设模板列表
     *
     * @return 系统模板列表
     */
    @GetMapping("/system")
    public ResponseEntity<Result<List<RuleTemplate>>> listSystemTemplates() {
        List<RuleTemplate> templates = ruleTemplateService.listSystemTemplates();
        return ResponseEntity.ok(Result.success(templates));
    }

    /**
     * 获取公开模板列表
     *
     * @return 公开模板列表
     */
    @GetMapping("/public")
    public ResponseEntity<Result<List<RuleTemplate>>> listPublicTemplates() {
        List<RuleTemplate> templates = ruleTemplateService.listPublicTemplates();
        return ResponseEntity.ok(Result.success(templates));
    }

    /**
     * 复制模板
     * <p>将系统模板或公开模板复制为用户自己的模板，以便自定义修改
     *
     * @param id 源模板ID
     * @return 复制后的新模板
     */
    @PostMapping("/{id}/copy")
    public ResponseEntity<Result<RuleTemplate>> copyTemplate(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        RuleTemplate template = ruleTemplateService.copyTemplate(userId, id);
        log.info("用户 {} 复制模板成功，源模板: {}, 新模板: {}", userId, id, template.getId());
        return ResponseEntity.ok(Result.success("模板复制成功", template));
    }
}
