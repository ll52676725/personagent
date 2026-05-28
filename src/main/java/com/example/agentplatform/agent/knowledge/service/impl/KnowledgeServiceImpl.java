package com.example.agentplatform.agent.knowledge.service.impl;

import com.example.agentplatform.agent.knowledge.dto.KnowledgeCreateDTO;
import com.example.agentplatform.agent.knowledge.dto.KnowledgeUpdateDTO;
import com.example.agentplatform.agent.knowledge.entity.Knowledge;
import com.example.agentplatform.agent.knowledge.entity.KnowledgeBase;
import com.example.agentplatform.agent.knowledge.repository.KnowledgeChunkRepository;
import com.example.agentplatform.agent.knowledge.repository.KnowledgeBaseRepository;
import com.example.agentplatform.agent.knowledge.repository.KnowledgeRepository;
import com.example.agentplatform.agent.knowledge.service.KnowledgeService;
import com.example.agentplatform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 知识条目服务实现类
 *
 * 功能概述：
 * 该类负责知识条目的完整生命周期管理，包括创建、查询、更新和删除操作。
 * 知识条目是知识库中存储的基本单元，包含标题、内容、来源、标签等信息。
 *
 * 核心功能：
 * 1. 创建知识条目 - 支持手动输入和文档导入两种方式
 * 2. 更新知识条目 - 内容变更时自动重置分片状态，触发重新向量化
 * 3. 查询知识条目 - 按知识库或用户维度查询
 * 4. 删除知识条目 - 级联删除相关的向量化分片，更新知识库统计
 *
 * 状态说明：
 * - chunkStatus: 分片处理状态
 *   - 0: 未处理（等待向量化）
 *   - 1: 处理中
 *   - 2: 已完成
 *   - 3: 处理失败
 *
 * 数据关联：
 * - 每个知识条目属于一个知识库（baseId）
 * - 每个知识条目可以生成多个向量化分片（KnowledgeChunk）
 * - sourceType: 知识来源类型，支持 manual（手动输入）、file（文件导入）、url（网页抓取）等
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeServiceImpl implements KnowledgeService {

    private final KnowledgeRepository knowledgeRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;

    /**
     * 创建新的知识条目
     *
     * 处理流程：
     * 1. 验证知识库存在且用户有权限访问
     * 2. 构建知识条目实体，设置初始状态（chunkStatus=0 表示未处理）
     * 3. 保存知识条目到数据库
     * 4. 更新知识库的知识条目统计数（knowledgeCount + 1）
     * 5. 记录操作日志
     *
     * 字段说明：
     * - sourceType: 默认为 "manual"（手动输入），可通过 DTO 指定
     * - chunkStatus: 初始为 0，表示需要异步进行向量化处理
     * - chunkCount: 初始为 0，待异步处理完成后更新
     *
     * @param userId 用户ID，用于权限验证
     * @param baseId 所属知识库ID
     * @param dto    知识条目创建请求DTO，包含标题、内容、来源等信息
     * @return 创建成功的知识条目实体
     * @throws BusinessException 如果知识库不存在或用户无权限
     */
    @Override
    @Transactional
    public Knowledge createKnowledge(Long userId, Long baseId, KnowledgeCreateDTO dto) {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findByIdAndUserId(baseId, userId)
                .orElseThrow(() -> new BusinessException("知识库不存在"));

        Knowledge knowledge = Knowledge.builder()
                .userId(userId)
                .baseId(baseId)
                .title(dto.getTitle())
                .content(dto.getContent())
                .sourceType(dto.getSourceType() != null ? dto.getSourceType() : "manual")
                .sourceUrl(dto.getSourceUrl())
                .fileName(dto.getFileName())
                .fileType(dto.getFileType())
                .fileSize(dto.getFileSize())
                .tags(dto.getTags())
                .category(dto.getCategory())
                .chunkStatus(0)
                .chunkCount(0)
                .build();
        Knowledge saved = knowledgeRepository.save(knowledge);

        knowledgeBase.setKnowledgeCount(knowledgeBase.getKnowledgeCount() + 1);
        knowledgeBaseRepository.save(knowledgeBase);

        log.info("用户 {} 在知识库 {} 中创建知识条目: {}", userId, baseId, saved.getId());
        return saved;
    }

    /**
     * 更新知识条目
     *
     * 处理逻辑：
     * 1. 验证用户对该知识条目的访问权限
     * 2. 对 DTO 中的非空字段进行更新
     * 3. 如果内容发生变化，将 chunkStatus 重置为 0，触发重新向量化
     * 4. 保存更新后的知识条目
     *
     * 重要说明：
     * - 内容变更（contentChanged）会触发重新分片和向量化
     * - 标题、标签、分类等元数据变更不会触发重新向量化
     *
     * @param userId      用户ID，用于权限验证
     * @param knowledgeId 知识条目ID
     * @param dto         知识条目更新请求DTO
     * @return 更新后的知识条目实体
     * @throws BusinessException 如果知识条目不存在或用户无权限
     */
    @Override
    @Transactional
    public Knowledge updateKnowledge(Long userId, Long knowledgeId, KnowledgeUpdateDTO dto) {
        Knowledge knowledge = getKnowledge(userId, knowledgeId);
        boolean contentChanged = false;

        if (dto.getTitle() != null) {
            knowledge.setTitle(dto.getTitle());
        }
        if (dto.getContent() != null) {
            knowledge.setContent(dto.getContent());
            contentChanged = true;
        }
        if (dto.getTags() != null) {
            knowledge.setTags(dto.getTags());
        }
        if (dto.getCategory() != null) {
            knowledge.setCategory(dto.getCategory());
        }

        Knowledge saved = knowledgeRepository.save(knowledge);

        if (contentChanged) {
            knowledge.setChunkStatus(0);
            knowledgeRepository.save(knowledge);
        }

        return saved;
    }

    /**
     * 获取单个知识条目详情
     *
     * 通过用户ID和知识条目ID联合查询，确保用户只能访问自己的知识。
     * 这是一个重要的权限控制机制，防止越权访问。
     *
     * @param userId      用户ID
     * @param knowledgeId 知识条目ID
     * @return 知识条目实体
     * @throws BusinessException 如果知识条目不存在或用户无权限
     */
    @Override
    public Knowledge getKnowledge(Long userId, Long knowledgeId) {
        return knowledgeRepository.findByIdAndUserId(knowledgeId, userId)
                .orElseThrow(() -> new BusinessException("知识条目不存在"));
    }

    /**
     * 获取指定知识库下的所有知识条目
     *
     * 查询特定知识库中的知识条目，按创建时间倒序排列（最新的在前）。
     *
     * @param userId 用户ID，用于权限验证
     * @param baseId 知识库ID
     * @return 知识条目列表，按创建时间降序排列
     */
    @Override
    public List<Knowledge> listKnowledge(Long userId, Long baseId) {
        return knowledgeRepository.findByBaseIdAndUserIdOrderByCreatedAtDesc(baseId, userId);
    }

    /**
     * 获取用户的所有知识条目（跨知识库）
     *
     * 查询用户创建的所有知识条目，不区分知识库。
     * 适用于需要全局搜索或展示用户所有知识的场景。
     *
     * @param userId 用户ID
     * @return 知识条目列表，按创建时间降序排列
     */
    @Override
    public List<Knowledge> listAllKnowledge(Long userId) {
        return knowledgeRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 删除知识条目及其关联的分片数据
     *
     * 级联删除流程：
     * 1. 验证用户权限，确保只有知识条目所有者可以删除
     * 2. 删除该知识条目下的所有向量化分片（KnowledgeChunk）
     * 3. 删除知识条目本身
     * 4. 更新所属知识库的统计数（knowledgeCount - 1）
     * 5. 记录操作日志
     *
     * 注意事项：
     * - 此操作不可逆，删除后数据无法恢复
     * - 使用 @Transactional 确保原子性
     * - 知识库统计数使用 Math.max(0, ...) 防止负数
     *
     * @param userId      用户ID，用于权限验证
     * @param knowledgeId 要删除的知识条目ID
     * @throws BusinessException 如果知识条目不存在或用户无权限
     */
    @Override
    @Transactional
    public void deleteKnowledge(Long userId, Long knowledgeId) {
        Knowledge knowledge = getKnowledge(userId, knowledgeId);
        Long baseId = knowledge.getBaseId();

        knowledgeChunkRepository.deleteByKnowledgeId(knowledgeId);
        knowledgeRepository.delete(knowledge);

        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(baseId).orElse(null);
        if (knowledgeBase != null) {
            knowledgeBase.setKnowledgeCount(Math.max(0, knowledgeBase.getKnowledgeCount() - 1));
            knowledgeBaseRepository.save(knowledgeBase);
        }

        log.info("用户 {} 删除知识条目: {}", userId, knowledgeId);
    }

    /**
     * 触发知识条目的分片处理
     *
     * 该方法是异步处理的入口点，实际的分片和向量化逻辑由 KnowledgeProcessService 执行。
     * 当前实现仅记录日志，具体的触发机制可通过消息队列或事件监听实现。
     *
     * 设计意图：
     * - 解耦知识条目管理和向量化处理
     * - 支持异步、批量处理，提升系统响应速度
     * - 便于后续扩展为消息驱动架构
     *
     * @param knowledgeId 要处理的知识条目ID
     */
    @Override
    @Transactional
    public void processKnowledgeChunks(Long knowledgeId) {
        log.info("处理知识条目 {} 的分片请求已记录", knowledgeId);
    }

    /**
     * 保存知识条目
     *
     * 用于更新已存在的知识条目，特别是保存MinerU解析的结构化数据
     *
     * @param knowledge 要保存的知识条目实体
     * @return 保存后的知识条目实体
     */
    @Override
    @Transactional
    public Knowledge save(Knowledge knowledge) {
        return knowledgeRepository.save(knowledge);
    }
}
