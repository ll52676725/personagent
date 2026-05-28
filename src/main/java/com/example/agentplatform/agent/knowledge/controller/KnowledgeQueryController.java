package com.example.agentplatform.agent.knowledge.controller;

import com.example.agentplatform.agent.knowledge.dto.KnowledgeQueryDTO;
import com.example.agentplatform.agent.knowledge.dto.KnowledgeQueryResult;
import com.example.agentplatform.agent.knowledge.dto.KnowledgeSearchDTO;
import com.example.agentplatform.agent.knowledge.service.KnowledgeQueryService;
import com.example.agentplatform.common.dto.Result;
import com.example.agentplatform.common.exception.BusinessException;
import com.example.agentplatform.common.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 知识库查询控制器
 * 提供基于 RAG（检索增强生成）的智能问答和语义搜索功能
 *
 * RAG 技术架构：
 * 1. 检索（Retrieval）：根据用户问题从向量库中检索最相关的知识片段
 * 2. 增强（Augmentation）：将检索到的知识片段作为上下文注入到提示词中
 * 3. 生成（Generation）：大模型基于上下文生成精准回答
 *
 * 优势：
 * - 回答基于私有知识库，可控制知识边界
 * - 可溯源，回答附带引用来源
 * - 无需微调，更新知识库即时生效
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agent-knowledge")
@RequiredArgsConstructor
public class KnowledgeQueryController {

    private final KnowledgeQueryService knowledgeQueryService;

    /**
     * 知识库智能问答（同步模式）
     * 等待完整回答生成后一次性返回
     * 适合需要完整答案进行后续处理的场景
     *
     * @param dto 查询请求
     *            - question: 用户问题（必填）
     *            - baseId: 指定知识库ID，null表示全部知识库
     *            - topK: 返回最相关的知识片段数量，默认5
     *            - similarityThreshold: 相似度阈值，默认0.5
     * @return 问答结果，包含回答、引用来源、Token使用量等
     */
    @PostMapping("/query")
    public ResponseEntity<Result<KnowledgeQueryResult>> query(@Valid @RequestBody KnowledgeQueryDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("用户 {} 发起知识库问答: {}", userId, dto.getQuestion());
        KnowledgeQueryResult result = knowledgeQueryService.query(dto, userId);
        return ResponseEntity.ok(Result.success(result));
    }

    /**
     * 知识库智能问答（流式模式）
     * 使用 SSE（Server-Sent Events）流式输出回答
     * 边生成边返回，提供更好的用户体验
     * 适合对话式交互场景
     *
     * @param dto 查询请求，参数同同步接口
     * @return 流式字符串，按 Token 逐步输出
     */
    @PostMapping(value = "/query/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> queryStream(@Valid @RequestBody KnowledgeQueryDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("用户 {} 发起流式知识库问答: {}", userId, dto.getQuestion());
        try {
            return knowledgeQueryService.queryStream(dto, userId);
        } catch (Exception e) {
            log.error("流式问答失败", e);
            return Flux.error(new BusinessException("问答失败：" + e.getMessage()));
        }
    }

    /**
     * 知识库语义搜索
     * 根据语义相似度检索相关知识片段
     * 不经过大模型生成，直接返回匹配的知识内容
     * 适合需要快速查找相关知识的场景
     *
     * @param dto 搜索请求
     *            - keyword: 搜索关键词/问题
     *            - baseId: 指定知识库ID，null表示全部知识库
     *            - topK: 返回结果数量，默认10
     * @return 按相似度排序的知识引用列表
     */
    @PostMapping("/search")
    public ResponseEntity<Result<List<KnowledgeQueryResult.SourceReference>>> search(@Valid @RequestBody KnowledgeSearchDTO dto) {
        Long userId = SecurityUtils.getCurrentUserId();
        log.info("用户 {} 知识库语义搜索: {}", userId, dto.getKeyword());
        List<KnowledgeQueryResult.SourceReference> results = knowledgeQueryService.search(dto, userId);
        return ResponseEntity.ok(Result.success(results));
    }
}
