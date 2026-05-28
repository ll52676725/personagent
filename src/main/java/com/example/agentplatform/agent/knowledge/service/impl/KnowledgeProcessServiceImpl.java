package com.example.agentplatform.agent.knowledge.service.impl;

import com.example.agentplatform.agent.knowledge.entity.Knowledge;
import com.example.agentplatform.agent.knowledge.entity.KnowledgeBase;
import com.example.agentplatform.agent.knowledge.entity.KnowledgeChunk;
import com.example.agentplatform.agent.knowledge.repository.KnowledgeBaseRepository;
import com.example.agentplatform.agent.knowledge.repository.KnowledgeChunkRepository;
import com.example.agentplatform.agent.knowledge.repository.KnowledgeRepository;
import com.example.agentplatform.agent.knowledge.service.EmbeddingService;
import com.example.agentplatform.agent.knowledge.service.KnowledgeProcessService;
import com.example.agentplatform.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 知识处理服务实现类
 *
 * 功能概述：
 * 该类负责知识条目的异步处理流水线，包括文本分片、向量化和持久化存储。
 * 是 RAG 系统中连接知识入库和检索的关键环节。
 *
 * 处理流水线：
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  知识条目创建 → 状态: 未处理(0)                                    │
 * │         ↓                                                          │
 * │  异步触发 processAndEmbedKnowledge()                               │
 * │         ↓                                                          │
 * │  状态更新: 处理中(1)                                                │
 * │         ↓                                                          │
 * │  1. 删除旧分片（如果是重新处理）                                    │
 * │  2. 智能文本分片 splitIntoChunks()                                 │
 * │  3. 批量向量化 embeddingService.embed()                            │
 * │  4. 持久化分片 knowledgeChunkRepository.save()                     │
 * │         ↓                                                          │
 * │  状态更新: 已完成(2) / 失败(3)                                      │
 * │         ↓                                                          │
 * │  更新知识库统计: chunkCount                                        │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * 核心技术点：
 * 1. 异步处理 - @Async 注解，不阻塞主线程
 * 2. 事务管理 - @Transactional 确保数据一致性
 * 3. 智能分片 - 基于段落的语义分片，保留上下文重叠
 * 4. 容错机制 - 单个分片失败不影响整体处理
 *
 * 状态流转：
 * chunkStatus: 0=未处理, 1=处理中, 2=已完成, 3=失败
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeProcessServiceImpl implements KnowledgeProcessService {

    private final KnowledgeRepository knowledgeRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final EmbeddingService embeddingService;
    private final ObjectMapper objectMapper;

    @Value("${agent.platform.knowledge.chunk.size:500}")
    private int chunkSize;

    @Value("${agent.platform.knowledge.chunk.overlap:50}")
    private int chunkOverlap;

    /**
     * 异步处理知识条目：分片 + 向量化
     *
     * 这是知识处理的核心入口方法，使用 @Async 注解标记为异步执行。
     * 需要在 Spring Boot 启动类上添加 @EnableAsync 注解才能生效。
     *
     * 处理流程详解：
     * 1. 加载知识条目，更新状态为"处理中"(1)
     * 2. 删除该知识条目已有的旧分片（幂等性处理）
     * 3. 对内容进行空值检查
     * 4. 调用 splitIntoChunks() 进行智能分片
     * 5. 遍历每个分片，依次向量化并保存
     * 6. 更新知识条目状态为"已完成"(2)，设置分片数量
     * 7. 更新所属知识库的分片总数统计
     *
     * 容错设计：
     * - 外层 try-catch 捕获整体异常，设置状态为"失败"(3)
     * - 内层循环 try-catch 保证单个分片失败不影响其他分片
     *
     * @param knowledgeId 知识条目ID
     */
    @Override
    @Async
    @Transactional
    public void processAndEmbedKnowledge(Long knowledgeId) {
        log.info("开始处理知识条目: {}", knowledgeId);

        Knowledge knowledge = knowledgeRepository.findById(knowledgeId)
                .orElseThrow(() -> new BusinessException("知识条目不存在: " + knowledgeId));

        knowledge.setChunkStatus(1);
        knowledgeRepository.save(knowledge);

        try {
            knowledgeChunkRepository.deleteByKnowledgeId(knowledgeId);

            String content = knowledge.getContent();
            if (content == null || content.isBlank()) {
                log.warn("知识条目 {} 内容为空，跳过处理", knowledgeId);
                knowledge.setChunkStatus(2);
                knowledge.setChunkCount(0);
                knowledgeRepository.save(knowledge);
                return;
            }

            List<String> chunks = splitIntoChunks(content);
            log.info("知识条目 {} 分为 {} 个片段", knowledgeId, chunks.size());

            List<KnowledgeChunk> savedChunks = new ArrayList<>();

            for (int i = 0; i < chunks.size(); i++) {
                String chunkContent = chunks.get(i);

                try {
                    List<Double> embedding = embeddingService.embed(chunkContent);

                    KnowledgeChunk chunk = KnowledgeChunk.builder()
                            .knowledgeId(knowledgeId)
                            .baseId(knowledge.getBaseId())
                            .chunkIndex(i)
                            .content(chunkContent)
                            .embedding(objectMapper.writeValueAsString(embedding))
                            .tokenCount(estimateTokenCount(chunkContent))
                            .build();

                    savedChunks.add(knowledgeChunkRepository.save(chunk));
                } catch (Exception e) {
                    log.error("处理分片 {}/{} 失败", i + 1, chunks.size(), e);
                }
            }

            knowledge.setChunkStatus(2);
            knowledge.setChunkCount(savedChunks.size());
            knowledgeRepository.save(knowledge);

            updateBaseChunkCount(knowledge.getBaseId());

            log.info("知识条目 {} 处理完成，成功生成 {} 个向量化分片",
                    knowledgeId, savedChunks.size());

        } catch (Exception e) {
            log.error("知识条目 {} 处理失败", knowledgeId, e);
            knowledge.setChunkStatus(3);
            knowledgeRepository.save(knowledge);
        }
    }

    /**
     * 重新处理知识条目
     *
     * 与 processAndEmbedKnowledge 逻辑相同，作为语义化的独立方法存在。
     * 便于在知识内容更新后手动触发重新处理。
     *
     * 使用场景：
     * - 知识条目内容修改后重新向量化
     * - 更换嵌入模型后批量重新处理
     * - 处理失败的知识重试
     *
     * @param knowledgeId 知识条目ID
     */
    @Override
    @Async
    @Transactional
    public void reprocessKnowledge(Long knowledgeId) {
        log.info("重新处理知识条目: {}", knowledgeId);
        processAndEmbedKnowledge(knowledgeId);
    }

    /**
     * 将文本智能分割为多个片段
     *
     * 分片策略：基于段落的语义分片
     *
     * 为什么选择段落级分片？
     * 1. 语义完整性：段落通常表达一个完整的意思，作为分片单元更合理
     * 2. 可读性：检索到的分片更易读，便于 LLM 理解
     * 3. 避免断句：不会在句子中间切断
     *
     * 算法流程：
     * 1. 将文本按空行分割为段落（\n\n+）
     * 2. 遍历每个段落，累加至当前分片
     * 3. 当累计长度超过 chunkSize 时，切出一个分片
     * 4. 保留尾部 overlap 长度的文本作为下一个分片的开头
     * 5. 处理剩余文本作为最后一个分片
     *
     * 重叠机制（Overlap）的作用：
     * - 解决上下文断层问题
     * - 确保跨分片的语义关联
     * - 提高检索准确率
     *
     * @param content 原始文本内容
     * @return 分片列表
     */
    private List<String> splitIntoChunks(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();

        String[] paragraphs = content.split("\n\n+");

        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) {
                continue;
            }

            if (currentChunk.length() + paragraph.length() > chunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                String overlap = getOverlapText(currentChunk.toString());
                currentChunk = new StringBuilder(overlap);
            }

            if (currentChunk.length() > 0) {
                currentChunk.append("\n\n");
            }
            currentChunk.append(paragraph);
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        return chunks;
    }

    /**
     * 获取分片重叠文本，确保上下文连续性
     *
     * 智能重叠策略：
     * 不是简单截取末尾 N 个字符，而是尝试在单词/句子边界处截断，
     * 避免将一个完整的词切成两半。
     *
     * 算法：
     * 1. 如果文本长度 ≤ chunkOverlap，直接返回全文
     * 2. 从目标位置向前查找最近的空格
     * 3. 返回空格之后的文本作为重叠部分
     * 4. 如果找不到空格，退化为简单截断
     *
     * @param text 当前分片文本
     * @return 重叠部分文本
     */
    private String getOverlapText(String text) {
        if (text.length() <= chunkOverlap) {
            return text;
        }
        int lastSpace = text.lastIndexOf(' ', text.length() - chunkOverlap);
        if (lastSpace > 0) {
            return text.substring(lastSpace + 1);
        }
        return text.substring(text.length() - chunkOverlap);
    }

    /**
     * 估算文本的 Token 数量
     *
     * 为什么需要估算 Token 数？
     * 1. 成本控制：预估 API 调用费用
     * 2. 限流参考：判断是否超过模型上下文限制
     * 3. 统计分析：知识库 Token 用量统计
     *
     * 估算规则（经验值）：
     * - 中文：约 1.5 个字符 = 1 Token
     * - 英文：约 4 个字符 = 1 Token
     *
     * 注意：这只是粗略估算，精确值需要调用模型 Tokenizer。
     * 如需精确计算，可使用 JTokkit 或 HuggingFace Tokenizers 库。
     *
     * @param text 输入文本
     * @return 估算的 Token 数
     */
    private int estimateTokenCount(String text) {
        if (text == null) return 0;

        int chineseChars = 0;
        int otherChars = 0;

        for (char c : text.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                chineseChars++;
            } else {
                otherChars++;
            }
        }

        return (int) (chineseChars / 1.5 + otherChars / 4.0);
    }

    /**
     * 更新知识库的分片总数统计
     *
     * 在知识条目处理完成后，重新统计该知识库下的总分片数，
     * 并更新知识库实体的 chunkCount 字段。
     *
     * 这个统计字段用于：
     * - 前端展示知识库规模
     * - 检索时的性能参考
     * - 知识库容量配额管理
     *
     * @param baseId 知识库ID
     */
    private void updateBaseChunkCount(Long baseId) {
        long count = knowledgeChunkRepository.countByBaseId(baseId);
        knowledgeBaseRepository.findById(baseId).ifPresent(base -> {
            base.setChunkCount((int) count);
            knowledgeBaseRepository.save(base);
        });
    }
}
