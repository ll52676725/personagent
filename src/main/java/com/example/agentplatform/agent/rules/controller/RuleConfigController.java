package com.example.agentplatform.agent.rules.controller;

import com.example.agentplatform.agent.rules.dto.RuleConfigCreateDTO;
import com.example.agentplatform.agent.rules.dto.RulePullRequestDTO;
import com.example.agentplatform.agent.rules.dto.RulePullResultDTO;
import com.example.agentplatform.agent.rules.entity.RuleConfig;
import com.example.agentplatform.agent.rules.service.RuleConfigService;
import com.example.agentplatform.common.dto.Result;
import com.example.agentplatform.common.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 规则配置管理控制器
 * <p>管理规则配置的CRUD和规则拉取操作
 * <p>规则配置定义了模板如何应用到具体项目，包含目标路径、冲突处理策略等
 * 
 * @author System
 * @since 2025-01-01
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agent-rules/configs")
@RequiredArgsConstructor
public class RuleConfigController {

    private final RuleConfigService ruleConfigService;

    /**
     * 创建规则配置
     * <p>将规则模板关联到具体的项目路径，定义拉取规则
     *
     * @param dto 配置创建请求
     * @return 创建的配置实体
     */
    @PostMapping
    public ResponseEntity<Result<RuleConfig>> createConfig(@Valid @RequestBody RuleConfigCreateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        RuleConfig config = ruleConfigService.createConfig(userId, dto);
        log.info("用户 {} 创建规则配置成功: {}", userId, config.getId());
        return ResponseEntity.ok(Result.success("配置创建成功", config));
    }

    /**
     * 更新规则配置
     *
     * @param id 配置ID
     * @param dto 更新内容
     * @return 更新后的配置
     */
    @PutMapping("/{id}")
    public ResponseEntity<Result<RuleConfig>> updateConfig(
            @PathVariable Long id,
            @Valid @RequestBody RuleConfigCreateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        RuleConfig config = ruleConfigService.updateConfig(userId, id, dto);
        log.info("用户 {} 更新规则配置: {}", userId, id);
        return ResponseEntity.ok(Result.success("配置更新成功", config));
    }

    /**
     * 删除规则配置
     *
     * @param id 配置ID
     * @return 空结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Result<Void>> deleteConfig(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        ruleConfigService.deleteConfig(userId, id);
        log.info("用户 {} 删除规则配置: {}", userId, id);
        return ResponseEntity.ok(Result.success("配置删除成功", null));
    }

    /**
     * 获取单个配置详情
     *
     * @param id 配置ID
     * @return 配置详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<RuleConfig>> getConfig(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        RuleConfig config = ruleConfigService.getConfig(userId, id);
        return ResponseEntity.ok(Result.success(config));
    }

    /**
     * 获取用户的所有规则配置列表
     *
     * @return 配置列表
     */
    @GetMapping
    public ResponseEntity<Result<List<RuleConfig>>> listConfigs() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<RuleConfig> configs = ruleConfigService.listConfigs(userId);
        return ResponseEntity.ok(Result.success(configs));
    }

    /**
     * 按分类获取配置列表
     *
     * @param category 规则分类
     * @return 配置列表
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<Result<List<RuleConfig>>> listConfigsByCategory(@PathVariable String category) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<RuleConfig> configs = ruleConfigService.listConfigsByCategory(userId, category);
        return ResponseEntity.ok(Result.success(configs));
    }

    /**
     * 按项目路径获取配置列表
     *
     * @param projectPath 项目路径（URL编码）
     * @return 配置列表
     */
    @GetMapping("/project")
    public ResponseEntity<Result<List<RuleConfig>>> listConfigsByProject(@RequestParam String projectPath) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<RuleConfig> configs = ruleConfigService.listConfigsByProject(userId, projectPath);
        return ResponseEntity.ok(Result.success(configs));
    }

    /**
     * 执行规则拉取
     * <p>将模板内容拉取到本地文件，自动检测并处理冲突
     * <p>冲突处理策略：
     * - ask: 询问用户（默认）
     * - overwrite: 直接覆盖
     * - keep_local: 保留本地
     * - merge: 智能合并
     * - rename: 重命名保存
     *
     * @param dto 拉取请求
     * @return 拉取结果，包含冲突信息和处理建议
     */
    @PostMapping("/pull")
    public ResponseEntity<Result<RulePullResultDTO>> pullRules(@Valid @RequestBody RulePullRequestDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("用户 {} 执行规则拉取，templateId: {}, configId: {}", 
                userId, dto.getTemplateId(), dto.getConfigId());
        
        RulePullResultDTO result = ruleConfigService.pullRules(userId, dto);
        
        if ("SUCCESS".equals(result.getStatus())) {
            return ResponseEntity.ok(Result.success(result.getMessage(), result));
        } else if ("CONFLICT".equals(result.getStatus())) {
            return ResponseEntity.ok(Result.success("检测到文件冲突，请选择处理策略", result));
        } else {
            return ResponseEntity.ok(Result.success(result.getMessage(), result));
        }
    }
}
