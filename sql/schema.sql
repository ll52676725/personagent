-- ============================================================
-- 个人Agent平台 (agent-platform) - MySQL 8.x 建表脚本
-- 数据库名与 application.yml 一致: agent_platform
-- ============================================================

CREATE DATABASE IF NOT EXISTS agent_platform
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE agent_platform;

-- ------------------------------------------------------------
-- 1. 用户表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username      VARCHAR(64)  NOT NULL COMMENT '用户名',
    email         VARCHAR(128) NOT NULL COMMENT '邮箱',
    password_hash VARCHAR(256) NOT NULL COMMENT '密码哈希(BCrypt)',
    avatar        VARCHAR(256)          DEFAULT NULL COMMENT '头像URL',
    status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ------------------------------------------------------------
-- 2. Agent 表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS sys_agent;
CREATE TABLE sys_agent (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name        VARCHAR(64)  NOT NULL COMMENT 'Agent名称',
    code        VARCHAR(32)  NOT NULL COMMENT 'Agent编码(唯一)',
    description VARCHAR(512)          DEFAULT NULL COMMENT '描述',
    module_name VARCHAR(64)  NOT NULL COMMENT '后端模块/包标识',
    icon        VARCHAR(256)          DEFAULT NULL COMMENT '图标URL',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_code (code),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent表';

-- ------------------------------------------------------------
-- 3. Agent 权限申请表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS agent_permission;
CREATE TABLE agent_permission (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id       BIGINT       NOT NULL COMMENT '用户ID',
    agent_id      BIGINT       NOT NULL COMMENT 'Agent ID',
    status        TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0-待审核, 1-已通过, 2-已拒绝',
    apply_reason  VARCHAR(512)          DEFAULT NULL COMMENT '申请理由',
    reject_reason VARCHAR(512)          DEFAULT NULL COMMENT '拒绝理由',
    approved_at   DATETIME              DEFAULT NULL COMMENT '审批时间',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_agent (user_id, agent_id),
    KEY idx_user_id (user_id),
    KEY idx_agent_id (agent_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent权限申请表';

-- ------------------------------------------------------------
-- 4. 文章表 (技术文章 Agent)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS article;
CREATE TABLE article (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id       BIGINT        NOT NULL COMMENT '用户ID',
    collection_id BIGINT                 DEFAULT NULL COMMENT '合集ID',
    title         VARCHAR(256)  NOT NULL COMMENT '标题',
    summary       VARCHAR(1024)          DEFAULT NULL COMMENT '概要',
    content       LONGTEXT               DEFAULT NULL COMMENT '正文(Markdown)',
    cover_image   VARCHAR(256)           DEFAULT NULL COMMENT '封面图URL',
    tags          VARCHAR(512)           DEFAULT NULL COMMENT '标签,逗号分隔',
    status        TINYINT       NOT NULL DEFAULT 0 COMMENT '状态: 0-草稿, 1-已发布, 2-归档',
    publish_links JSON                   DEFAULT NULL COMMENT '各平台发布结果JSON',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    published_at  DATETIME               DEFAULT NULL COMMENT '发布时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_collection_id (collection_id),
    KEY idx_status (status),
    KEY idx_created_at (created_at),
    KEY idx_user_status (user_id, status),
    FULLTEXT KEY ft_title_content (title, content)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章表';

-- ------------------------------------------------------------
-- 5. 文章合集表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS article_collection;
CREATE TABLE article_collection (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id       BIGINT        NOT NULL COMMENT '用户ID',
    title         VARCHAR(256)  NOT NULL COMMENT '合集标题',
    cover_image   VARCHAR(256)           DEFAULT NULL COMMENT '封面图URL',
    description   LONGTEXT               DEFAULT NULL COMMENT '合集描述',
    outlines      JSON                   DEFAULT NULL COMMENT '文章大纲列表JSON',
    article_count INT           NOT NULL DEFAULT 0 COMMENT '文章数量',
    status        TINYINT       NOT NULL DEFAULT 0 COMMENT '状态: 0-草稿, 1-已完成',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章合集表';

-- ------------------------------------------------------------
-- 6. 发布配置表 (多平台发布账号配置)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS publish_config;
CREATE TABLE publish_config (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id        BIGINT       NOT NULL COMMENT '用户ID',
    platform       VARCHAR(32)  NOT NULL COMMENT '平台: csdn/juejin/zhihu 等',
    account_name   VARCHAR(128)          DEFAULT NULL COMMENT '账号名称',
    api_key        VARCHAR(512)          DEFAULT NULL COMMENT 'API Key',
    api_secret     VARCHAR(512)          DEFAULT NULL COMMENT 'API Secret',
    access_token   VARCHAR(1024)         DEFAULT NULL COMMENT '访问Token/Cookie',
    refresh_token  VARCHAR(1024)         DEFAULT NULL COMMENT '刷新Token',
    expires_at     DATETIME              DEFAULT NULL COMMENT '过期时间',
    config         JSON                  DEFAULT NULL COMMENT '扩展配置JSON',
    enabled        TINYINT      NOT NULL DEFAULT 1 COMMENT '是否启用: 0-禁用, 1-启用',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_platform (user_id, platform),
    KEY idx_user_id (user_id),
    KEY idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发布配置表';

-- ------------------------------------------------------------
-- 初始化 Agent 数据 (与 DataInitializer 一致, 可重复执行需先清空或改 INSERT IGNORE)
-- ------------------------------------------------------------
INSERT INTO sys_agent (name, code, description, module_name, icon, status) VALUES
('技术文章Agent', 'article', '帮助您生成高质量的技术文章，包括标题生成、概要生成、正文生成等功能', 'agent.article', 'https://api.iconify.design/material-symbols/article.svg', 1),
('代码审查Agent', 'code-review', '帮助您审查代码质量，发现潜在问题和安全漏洞', 'agent-code-review', 'https://api.iconify.design/material-symbols/code-review.svg', 1),
('文档生成Agent', 'doc', '帮助您生成各类技术文档，如API文档、需求文档等', 'agent-doc', 'https://api.iconify.design/material-symbols/file-document.svg', 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    module_name = VALUES(module_name),
    icon = VALUES(icon),
    status = VALUES(status);
