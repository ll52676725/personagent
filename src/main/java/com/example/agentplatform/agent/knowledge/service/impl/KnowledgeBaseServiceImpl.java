package com.example.agentplatform.agent.knowledge.service.impl;

import com.example.agentplatform.agent.knowledge.dto.KnowledgeBaseCreateDTO;
import com.example.agentplatform.agent.knowledge.dto.KnowledgeBaseUpdateDTO;
import com.example.agentplatform.agent.knowledge.entity.KnowledgeBase;
import com.example.agentplatform.agent.knowledge.repository.KnowledgeBaseRepository;
import com.example.agentplatform.agent.knowledge.repository.KnowledgeChunkRepository;
import com.example.agentplatform.agent.knowledge.repository.KnowledgeRepository;
import com.example.agentplatform.agent.knowledge.service.KnowledgeBaseService;
import com.example.agentplatform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 知识库服务实现类
 *
 * 功能概述：
 * 该类负责知识库的完整生命周期管理，包括知识库的创建、查询、更新和删除操作。
 * 知识库是知识条目的容器，每个用户可以创建多个独立的知识库来分类管理不同主题的知识。
 *
 * 核心功能：
 * 1. 创建知识库 - 初始化知识库属性，设置知识条数和分片数为0
 * 2. 更新知识库 - 修改知识库的名称、描述、图标等元数据
 * 3. 查询知识库 - 按用户ID查询单个知识库或知识库列表
 * 4. 删除知识库 - 级联删除知识库下的所有知识条目和向量化分片
 *
 * 数据关联：
 * - 知识库与知识条目：一对多关系，通过 baseId 关联
 * - 知识库与知识分片：一对多关系，通过 baseId 关联
 *
 * 状态说明：
 * - status: 1=正常, 0=禁用
 * - knowledgeCount: 知识条目总数统计
 * - chunkCount: 向量化分片总数统计
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeRepository knowledgeRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;

    /**
     * 创建新的知识库
     *
     * 处理流程：
     * 1. 从 DTO 中提取知识库名称、描述、图标等信息
     * 2. 初始化统计字段：knowledgeCount=0, chunkCount=0
     * 3. 设置状态为正常（status=1）
     * 4. 持久化到数据库并记录操作日志
     *
     * @param userId 用户ID，用于标识知识库所属用户
     * @param dto    知识库创建请求DTO，包含名称、描述、图标等字段
     * @return 创建成功的知识库实体，包含数据库生成的ID
     */
    @Override
    @Transactional
    public KnowledgeBase createKnowledgeBase(Long userId, KnowledgeBaseCreateDTO dto) {
        KnowledgeBase knowledgeBase = KnowledgeBase.builder()
                .userId(userId)
                .name(dto.getName())
                .description(dto.getDescription())
                .icon(dto.getIcon())
                .knowledgeCount(0)
                .chunkCount(0)
                .status(1)
                .build();
        KnowledgeBase saved = knowledgeBaseRepository.save(knowledgeBase);
        log.info("用户 {} 创建知识库: {}", userId, saved.getId());
        return saved;
    }

    /**
     * 更新知识库信息
     *
     * 处理流程：
     * 1. 验证用户对该知识库的访问权限（通过 userId 和 baseId 联合查询）
     * 2. 对 DTO 中的非空字段进行更新
     * 3. 保存更新后的知识库实体
     *
     * 支持更新的字段：
     * - name: 知识库名称
     * - description: 知识库描述
     * - icon: 知识库图标
     *
     * @param userId 用户ID，用于权限验证
     * @param baseId 知识库ID
     * @param dto    知识库更新请求DTO，仅非空字段会被更新
     * @return 更新后的知识库实体
     * @throws BusinessException 如果知识库不存在或用户无权限
     */
    @Override
    @Transactional
    public KnowledgeBase updateKnowledgeBase(Long userId, Long baseId, KnowledgeBaseUpdateDTO dto) {
        KnowledgeBase knowledgeBase = getKnowledgeBase(userId, baseId);
        if (dto.getName() != null) {
            knowledgeBase.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            knowledgeBase.setDescription(dto.getDescription());
        }
        if (dto.getIcon() != null) {
            knowledgeBase.setIcon(dto.getIcon());
        }
        return knowledgeBaseRepository.save(knowledgeBase);
    }

    /**
     * 获取单个知识库详情
     *
     * 通过用户ID和知识库ID联合查询，确保用户只能访问自己的知识库。
     * 这是一个重要的权限控制机制，防止越权访问。
     *
     * @param userId 用户ID
     * @param baseId 知识库ID
     * @return 知识库实体
     * @throws BusinessException 如果知识库不存在或用户无权限
     */
    @Override
    public KnowledgeBase getKnowledgeBase(Long userId, Long baseId) {
        return knowledgeBaseRepository.findByIdAndUserId(baseId, userId)
                .orElseThrow(() -> new BusinessException("知识库不存在"));
    }

    /**
     * 获取用户的所有知识库列表
     *
     * 查询当前用户创建的所有知识库，按创建时间倒序排列（最新的在前）。
     *
     * @param userId 用户ID
     * @return 知识库列表，按创建时间降序排列
     */
    @Override
    public List<KnowledgeBase> listKnowledgeBases(Long userId) {
        return knowledgeBaseRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 删除知识库及其所有关联数据
     *
     * 级联删除流程：
     * 1. 验证用户权限，确保只有知识库所有者可以删除
     * 2. 删除该知识库下的所有知识分片（KnowledgeChunk）
     * 3. 删除该知识库下的所有知识条目（Knowledge）
     * 4. 最后删除知识库本身
     * 5. 记录操作日志
     *
     * 注意事项：
     * - 此操作不可逆，删除后数据无法恢复
     * - 使用 @Transactional 确保原子性，任何一步失败都会回滚
     *
     * @param userId 用户ID，用于权限验证
     * @param baseId 要删除的知识库ID
     * @throws BusinessException 如果知识库不存在或用户无权限
     */
    @Override
    @Transactional
    public void deleteKnowledgeBase(Long userId, Long baseId) {
        KnowledgeBase knowledgeBase = getKnowledgeBase(userId, baseId);
        knowledgeChunkRepository.findByBaseId(baseId).forEach(chunk ->
                knowledgeChunkRepository.delete(chunk));
        knowledgeRepository.deleteByBaseIdAndUserId(baseId, userId);
        knowledgeBaseRepository.delete(knowledgeBase);
        log.info("用户 {} 删除知识库: {}", userId, baseId);
    }
}
