package com.example.agentplatform.agent.knowledge.controller;

import com.example.agentplatform.agent.knowledge.dto.KnowledgeBaseCreateDTO;
import com.example.agentplatform.agent.knowledge.dto.KnowledgeBaseUpdateDTO;
import com.example.agentplatform.agent.knowledge.entity.KnowledgeBase;
import com.example.agentplatform.agent.knowledge.service.KnowledgeBaseService;
import com.example.agentplatform.common.dto.Result;
import com.example.agentplatform.common.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库管理控制器
 * 提供知识库的 CRUD 操作接口
 * 知识库是知识条目的容器，一个用户可以拥有多个知识库
 * 所有接口均需要用户登录认证，数据按用户隔离
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agent-knowledge/bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 创建新的知识库
     * 用户可以创建多个知识库用于分类管理不同领域的知识
     *
     * @param dto 知识库创建请求，包含名称、描述、图标
     * @return 创建的知识库实体
     */
    @PostMapping
    public ResponseEntity<Result<KnowledgeBase>> createKnowledgeBase(@Valid @RequestBody KnowledgeBaseCreateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        KnowledgeBase knowledgeBase = knowledgeBaseService.createKnowledgeBase(userId, dto);
        log.info("用户 {} 创建知识库成功: {}", userId, knowledgeBase.getId());
        return ResponseEntity.ok(Result.success("知识库创建成功", knowledgeBase));
    }

    /**
     * 获取用户的所有知识库列表
     * 按创建时间倒序排列，最新创建的在前
     *
     * @return 知识库列表
     */
    @GetMapping
    public ResponseEntity<Result<List<KnowledgeBase>>> listKnowledgeBases() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<KnowledgeBase> bases = knowledgeBaseService.listKnowledgeBases(userId);
        return ResponseEntity.ok(Result.success(bases));
    }

    /**
     * 获取单个知识库的详细信息
     * 包括名称、描述、知识条目数、向量化分片数等统计信息
     *
     * @param id 知识库ID
     * @return 知识库详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Result<KnowledgeBase>> getKnowledgeBase(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        KnowledgeBase knowledgeBase = knowledgeBaseService.getKnowledgeBase(userId, id);
        return ResponseEntity.ok(Result.success(knowledgeBase));
    }

    /**
     * 更新知识库信息
     * 可修改名称、描述、图标等属性
     *
     * @param id 知识库ID
     * @param dto 更新内容
     * @return 更新后的知识库
     */
    @PutMapping("/{id}")
    public ResponseEntity<Result<KnowledgeBase>> updateKnowledgeBase(
            @PathVariable Long id,
            @Valid @RequestBody KnowledgeBaseUpdateDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        KnowledgeBase knowledgeBase = knowledgeBaseService.updateKnowledgeBase(userId, id, dto);
        log.info("用户 {} 更新知识库: {}", userId, id);
        return ResponseEntity.ok(Result.success("知识库更新成功", knowledgeBase));
    }

    /**
     * 删除知识库
     * 级联删除：会同时删除该知识库下的所有知识条目和向量分片数据
     * 注意：此操作不可恢复，请谨慎使用
     *
     * @param id 知识库ID
     * @return 空结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Result<Void>> deleteKnowledgeBase(@PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        knowledgeBaseService.deleteKnowledgeBase(userId, id);
        log.info("用户 {} 删除知识库: {}", userId, id);
        return ResponseEntity.ok(Result.success("知识库删除成功", null));
    }
}
