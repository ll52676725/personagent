package com.example.agentplatform.agent.knowledge.service.impl;

import com.example.agentplatform.agent.knowledge.dto.KnowledgeQueryDTO;
import com.example.agentplatform.agent.knowledge.dto.KnowledgeQueryResult;
import com.example.agentplatform.agent.knowledge.dto.KnowledgeSearchDTO;
import com.example.agentplatform.agent.knowledge.entity.Knowledge;
import com.example.agentplatform.agent.knowledge.entity.KnowledgeChunk;
import com.example.agentplatform.agent.knowledge.repository.KnowledgeChunkRepository;
import com.example.agentplatform.agent.knowledge.repository.KnowledgeRepository;
import com.example.agentplatform.agent.knowledge.service.EmbeddingService;
import com.example.agentplatform.agent.knowledge.service.KnowledgeQueryService;
import com.example.agentplatform.common.exception.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.StreamingChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识库查询服务实现类
 *
 * 功能概述：
 * 该类实现了 RAG（检索增强生成）架构的核心查询逻辑，是用户与知识库交互的核心入口。
 *
 * RAG 技术架构：
 * RAG (Retrieval-Augmented Generation) 是一种结合检索和生成的 AI 技术：
 * 1. 检索阶段（Retrieval）：从知识库中找到与用户问题最相关的知识片段
 * 2. 增强阶段（Augmentation）：将检索到的知识作为上下文注入到 Prompt 中
 * 3. 生成阶段（Generation）：LLM 基于上下文和用户问题生成回答
 *
 * 核心优势：
 * - 知识时效性：可以使用最新的私有知识，不受 LLM 训练数据 cutoff 限制
 * - 来源可追溯：回答基于具体的知识片段，可以提供来源引用
 * - 降低幻觉：LLM 基于提供的事实回答，减少编造信息
 * - 成本更低：相比微调模型，RAG 方案成本更低、迭代更快
 *
 * 查询流程：
 * ┌─────────────────────────────────────────────────────────────┐
 * │  用户问题                                                     │
 * │     ↓                                                         │
 * │  问题向量化（Embedding）                                       │
 * │     ↓                                                         │
 * │  向量相似度检索（Cosine Similarity）                           │
 * │     ↓                                                         │
 * │  Top K 相关知识片段                                            │
 * │     ↓                                                         │
 * │  构建上下文 Prompt（System + Context + Question）              │
 * │     ↓                                                         │
 * │  LLM 生成回答（同步 / 流式）                                    │
 * │     ↓                                                         │
 * │  返回结果 + 来源引用                                           │
 * └─────────────────────────────────────────────────────────────┘
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeQueryServiceImpl implements KnowledgeQueryService {

    private final ChatClient chatClient;
    private final StreamingChatClient streamingChatClient;
    private final EmbeddingService embeddingService;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final KnowledgeRepository knowledgeRepository;
    private final ObjectMapper objectMapper;

    @Value("${agent.platform.default-model:spring-ai}")
    private String modelName;

    /**
     * RAG 系统提示词
     *
     * 设计原则：
     * 1. 明确角色定位：专业的知识库问答助手
     * 2. 设定行为边界：严格基于知识库内容回答，不编造信息
     * 3. 规定输出格式：使用 Markdown，标注来源
     * 4. 异常处理：无相关信息时明确告知用户
     *
     * 提示词工程最佳实践：
     * - 使用分隔符清晰区分不同部分
     * - 采用结构化指令（编号列表）
     * - 给出正面示例（可通过 Few-Shot 增强）
     * - 明确失败场景的处理方式
     */
    private static final String RAG_SYSTEM_PROMPT = """
            你是一个专业的知识库问答助手。请根据提供的知识内容，准确、详细地回答用户的问题。

            回答要求：
            1. 严格基于提供的知识内容回答，不要编造知识库中没有的信息
            2. 如果知识库内容中没有相关信息，请明确告知用户"当前知识库中没有相关信息"
            3. 引用具体的知识来源，使用【来源：知识标题】格式标注
            4. 回答结构清晰，重点突出，使用 Markdown 格式增强可读性
            5. 如有多条相关知识，综合分析后给出完整回答
            6. 保持客观、专业的语气
            """;

    /**
     * 同步知识库问答
     *
     * 适用场景：
     * - 简单问题，期望快速得到完整回答
     * - 需要对完整回答进行后处理（如格式化、存储等）
     * - 前端不支持流式展示
     *
     * 处理流程：
     * 1. 检索相关知识片段（retrieveRelevantChunks）
     * 2. 构建上下文和用户提示词
     * 3. 调用 LLM 同步接口，等待完整回答
     * 4. 解析响应，提取回答内容和 Token 用量
     * 5. 封装结果返回
     *
     * @param dto    查询请求，包含问题、知识库ID、Top K、相似度阈值
     * @param userId 用户ID，用于权限过滤
     * @return 问答结果，包含问题、回答、来源引用、模型信息、Token 用量
     */
    @Override
    public KnowledgeQueryResult query(KnowledgeQueryDTO dto, Long userId) {
        log.info("用户 {} 发起知识库问答: {}", userId, dto.getQuestion());

        List<KnowledgeQueryResult.SourceReference> sources = retrieveRelevantChunks(
                dto.getQuestion(),
                dto.getBaseId(),
                dto.getTopK() != null ? dto.getTopK() : 5,
                dto.getSimilarityThreshold() != null ? dto.getSimilarityThreshold() : 0.5,
                userId
        );

        if (sources.isEmpty()) {
            return KnowledgeQueryResult.builder()
                    .question(dto.getQuestion())
                    .answer("当前知识库中未找到与您问题相关的信息。请尝试：\n\n1. 添加更多相关知识\n2. 调整问题表述\n3. 检查知识库选择")
                    .sources(List.of())
                    .model(modelName)
                    .tokens(0)
                    .build();
        }

        String context = buildContext(sources);
        String userPrompt = buildUserPrompt(dto.getQuestion(), context);

        try {
            List<Message> messages = List.of(
                    new SystemMessage(RAG_SYSTEM_PROMPT),
                    new UserMessage(userPrompt)
            );

            ChatResponse response = chatClient.call(new Prompt(messages));
            String answer = response.getResult().getOutput().getContent();

            Integer tokens = response.getMetadata().getUsage() != null ?
                    response.getMetadata().getUsage().getTotalTokens().intValue() : null;

            log.info("用户 {} 问答完成，使用 Token: {}", userId, tokens);

            return KnowledgeQueryResult.builder()
                    .question(dto.getQuestion())
                    .answer(answer)
                    .sources(sources)
                    .model(modelName)
                    .tokens(tokens)
                    .build();

        } catch (Exception e) {
            log.error("知识库问答失败", e);
            throw new BusinessException("问答失败：" + e.getMessage());
        }
    }

    /**
     * 流式知识库问答
     *
     * 适用场景：
     * - 复杂问题，回答较长
     * - 追求更好的用户体验（打字机效果）
     * - 现代前端应用，支持流式渲染
     *
     * 技术实现：
     * - 使用 Reactor Flux 进行响应式流式处理
     * - 每个流元素是 LLM 返回的 token 或文本片段
     * - 前端可以逐字渲染，提升 perceived performance
     *
     * 注意事项：
     * - 流式响应不便于获取完整的 Token 用量统计
     * - 需要在前端处理异常情况（流中断）
     *
     * @param dto    查询请求
     * @param userId 用户ID
     * @return Flux<String> 流式回答文本
     */
    @Override
    public Flux<String> queryStream(KnowledgeQueryDTO dto, Long userId) {
        log.info("用户 {} 发起流式知识库问答: {}", userId, dto.getQuestion());

        List<KnowledgeQueryResult.SourceReference> sources = retrieveRelevantChunks(
                dto.getQuestion(),
                dto.getBaseId(),
                dto.getTopK() != null ? dto.getTopK() : 5,
                dto.getSimilarityThreshold() != null ? dto.getSimilarityThreshold() : 0.5,
                userId
        );

        if (sources.isEmpty()) {
            return Flux.just("当前知识库中未找到与您问题相关的信息。请尝试添加更多相关知识或调整问题表述。");
        }

        String context = buildContext(sources);
        String userPrompt = buildUserPrompt(dto.getQuestion(), context);

        try {
            List<Message> messages = List.of(
                    new SystemMessage(RAG_SYSTEM_PROMPT),
                    new UserMessage(userPrompt)
            );

            return streamingChatClient.stream(new Prompt(messages))
                    .map(response -> {
                        String content = response.getResult().getOutput().getContent();
                        return content != null ? content : "";
                    })
                    .filter(content -> !content.isEmpty());

        } catch (Exception e) {
            log.error("流式知识库问答失败", e);
            return Flux.error(new BusinessException("问答失败：" + e.getMessage()));
        }
    }

    /**
     * 语义搜索
     *
     * 与 query() 的区别：
     * - 仅执行检索阶段，不调用 LLM 生成回答
     * - 返回原始的知识片段和相似度分数
     * - 速度更快，成本更低
     *
     * 应用场景：
     * - 用户只想查找相关资料，不需要生成回答
     * - 调试检索效果，评估召回率
     * - 批量获取相关知识用于其他处理
     *
     * @param dto    搜索请求，包含关键词、知识库ID、Top K
     * @param userId 用户ID
     * @return 相关知识引用列表，按相似度降序排列
     */
    @Override
    public List<KnowledgeQueryResult.SourceReference> search(KnowledgeSearchDTO dto, Long userId) {
        log.info("用户 {} 知识库语义搜索: {}", userId, dto.getKeyword());

        return retrieveRelevantChunks(
                dto.getKeyword(),
                dto.getBaseId(),
                dto.getTopK() != null ? dto.getTopK() : 10,
                0.3,
                userId
        );
    }

    /**
     * 检索与查询最相关的知识片段
     *
     * 这是 RAG 系统的核心检索算法，决定了问答质量的上限。
     *
     * 检索策略：
     * 1. 稠密向量检索（Dense Retrieval）：基于语义相似度
     *    - 优点：理解语义，支持同义词、 paraphrase
     *    - 缺点：计算成本高，需要向量化
     *
     * 2. 可扩展的混合检索（Hybrid Search）：
     *    - 稠密向量 + 稀疏向量（BM25/TF-IDF）
     *    - 使用 RRF（Reciprocal Rank Fusion）融合结果
     *    - 结合语义匹配和关键词匹配的优势
     *
     * 3. 重排序（Reranking）：
     *    - 先粗召回 Top 100，再用交叉编码器重排序
     *    - 大幅提升检索精度
     *
     * 当前实现：基础的稠密向量检索
     *
     * @param query     查询文本（问题或关键词）
     * @param baseId    知识库ID（null表示全部知识库）
     * @param topK      返回的最相关条目数
     * @param threshold 相似度阈值（低于此值的结果被过滤）
     * @param userId    用户ID（权限过滤）
     * @return 相关的知识引用列表，按相似度降序排列
     */
    private List<KnowledgeQueryResult.SourceReference> retrieveRelevantChunks(
            String query, Long baseId, int topK, Double threshold, Long userId) {

        try {
            List<Double> queryEmbedding = embeddingService.embed(query);

            List<KnowledgeChunk> candidateChunks = getCandidateChunks(baseId, userId);

            List<ChunkSimilarity> similarities = new ArrayList<>();

            for (KnowledgeChunk chunk : candidateChunks) {
                if (chunk.getEmbedding() == null || chunk.getEmbedding().isBlank()) {
                    continue;
                }

                try {
                    List<Double> chunkEmbedding = objectMapper.readValue(
                            chunk.getEmbedding(),
                            new TypeReference<List<Double>>() {}
                    );

                    double similarity = embeddingService.cosineSimilarity(queryEmbedding, chunkEmbedding);

                    if (threshold == null || similarity >= threshold) {
                        similarities.add(new ChunkSimilarity(chunk, similarity));
                    }
                } catch (Exception e) {
                    log.warn("解析向量数据失败，chunkId: {}", chunk.getId());
                }
            }

            similarities.sort((a, b) -> Double.compare(b.similarity, a.similarity));

            return similarities.stream()
                    .limit(topK)
                    .map(this::mapToSourceReference)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("检索相关知识失败", e);
            return List.of();
        }
    }

    /**
     * 获取待检索的候选分片
     *
     * 这是一个简单的实现，实际生产环境可优化：
     *
     * 优化方向 1：向量数据库
     * - 使用专门的向量数据库（Pinecone, Weaviate, Milvus, Qdrant）
     * - 支持 ANN（Approximate Nearest Neighbor）索引
     * - 毫秒级检索，支持亿级向量规模
     *
     * 优化方向 2：元数据过滤
     * - 按知识库、标签、分类、时间范围等预过滤
     * - 减少需要计算相似度的向量数量
     *
     * 优化方向 3：多级缓存
     * - 热门查询结果缓存
     * - 用户级向量缓存
     *
     * @param baseId 知识库ID
     * @param userId 用户ID
     * @return 分片列表
     */
    private List<KnowledgeChunk> getCandidateChunks(Long baseId, Long userId) {
        if (baseId != null) {
            return knowledgeChunkRepository.findByBaseId(baseId);
        }

        List<Long> userBaseIds = knowledgeRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(Knowledge::getBaseId)
                .distinct()
                .toList();

        return userBaseIds.stream()
                .flatMap(id -> knowledgeChunkRepository.findByBaseId(id).stream())
                .toList();
    }

    /**
     * 将分片相似度结果转换为知识引用对象
     *
     * 组装前端展示需要的字段：
     * - knowledgeId: 知识条目ID，用于跳转详情
     * - title: 知识标题，展示来源
     * - content: 分片内容，展示相关片段
     * - similarity: 相似度分数，用于排序和展示
     *
     * @param cs 分片相似度记录
     * @return 知识引用 DTO
     */
    private KnowledgeQueryResult.SourceReference mapToSourceReference(ChunkSimilarity cs) {
        Knowledge knowledge = knowledgeRepository.findById(cs.chunk.getKnowledgeId()).orElse(null);

        return KnowledgeQueryResult.SourceReference.builder()
                .knowledgeId(cs.chunk.getKnowledgeId())
                .title(knowledge != null ? knowledge.getTitle() : "未知来源")
                .content(cs.chunk.getContent())
                .similarity(cs.similarity)
                .build();
    }

    /**
     * 构建 RAG 上下文
     *
     * 将检索到的相关知识片段格式化为 LLM 易于理解的上下文文本。
     *
     * 设计要点：
     * 1. 清晰的分隔符（---）区分不同知识来源
     * 2. 明确标注每个来源的标题，便于 LLM 引用
     * 3. 编号标识（知识来源 1/2/3）便于追溯
     *
     * 上下文窗口管理：
     * - 需要控制总长度不超过模型上下文限制
     * - 可根据 Token 估算动态截断
     * - 重要的来源放在前面（注意力机制影响）
     *
     * @param sources 相关知识引用列表
     * @return 格式化的上下文文本
     */
    private String buildContext(List<KnowledgeQueryResult.SourceReference> sources) {
        StringBuilder context = new StringBuilder();

        for (int i = 0; i < sources.size(); i++) {
            KnowledgeQueryResult.SourceReference source = sources.get(i);
            context.append("【知识来源 ").append(i + 1).append(": ").append(source.getTitle()).append("】\n");
            context.append(source.getContent()).append("\n\n---\n\n");
        }

        return context.toString();
    }

    /**
     * 构建用户提示词
     *
     * 将知识库上下文和用户问题组合成最终的用户消息。
     *
     * 提示词结构：
     * - 明确标记【知识库内容】和【用户问题】区域
     * - 补充规则说明（无相关信息时的处理方式）
     * - 使用清晰的分隔增强可读性
     *
     * 后续可扩展：
     * - 支持用户指定回答风格（简洁/详细/专业/通俗）
     * - 支持指定回答语言
     * - 支持自定义输出模板
     *
     * @param question 用户问题
     * @param context  知识库上下文
     * @return 完整的用户提示词
     */
    private String buildUserPrompt(String question, String context) {
        return String.format("""
                【知识库内容】
                %s

                【用户问题】
                %s

                请基于以上知识库内容回答问题。如果知识库中没有相关信息，请明确说明。
                """, context, question);
    }

    /**
     * 分片相似度记录（内部使用的 Record 类）
     *
     * 用于在检索过程中临时存储分片和其相似度分数，
     * 便于后续排序和转换。
     *
     * Java 16+ Record 特性：不可变数据类，自动生成构造器、getter、equals、hashCode、toString
     *
     * @param chunk      知识分片实体
     * @param similarity 与查询的相似度分数
     */
    private record ChunkSimilarity(KnowledgeChunk chunk, double similarity) {}
}
