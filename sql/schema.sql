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
-- 7. 知识库表 (个人知识库 Agent)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS knowledge_base;
CREATE TABLE knowledge_base (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id         BIGINT       NOT NULL COMMENT '用户ID',
    name            VARCHAR(128) NOT NULL COMMENT '知识库名称',
    description     VARCHAR(512)          DEFAULT NULL COMMENT '描述',
    icon            VARCHAR(256)          DEFAULT NULL COMMENT '图标URL',
    knowledge_count INT          NOT NULL DEFAULT 0 COMMENT '知识条目数',
    chunk_count     INT          NOT NULL DEFAULT 0 COMMENT '向量分片数',
    status          TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表';

-- ------------------------------------------------------------
-- 8. 知识条目表 (个人知识库 Agent)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS knowledge;
CREATE TABLE knowledge (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id       BIGINT        NOT NULL COMMENT '用户ID',
    base_id       BIGINT        NOT NULL COMMENT '知识库ID',
    title         VARCHAR(256)  NOT NULL COMMENT '知识标题',
    content       LONGTEXT               DEFAULT NULL COMMENT '知识内容(Markdown)',
    source_type   VARCHAR(32)            DEFAULT 'manual' COMMENT '来源类型: manual/file/url',
    source_url    VARCHAR(512)           DEFAULT NULL COMMENT '来源URL',
    tags          VARCHAR(512)           DEFAULT NULL COMMENT '标签,逗号分隔',
    category      VARCHAR(64)            DEFAULT NULL COMMENT '分类',
    chunk_status  TINYINT      NOT NULL DEFAULT 0 COMMENT '分片状态: 0-未处理, 1-处理中, 2-已完成, 3-失败',
    chunk_count   INT          NOT NULL DEFAULT 0 COMMENT '分片数量',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_base_id (base_id),
    KEY idx_category (category),
    KEY idx_chunk_status (chunk_status),
    FULLTEXT KEY ft_title_content (title, content)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识条目表';

-- ------------------------------------------------------------
-- 9. 知识向量分片表 (个人知识库 Agent)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS knowledge_chunk;
CREATE TABLE knowledge_chunk (
    id             BIGINT    NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    knowledge_id   BIGINT    NOT NULL COMMENT '知识条目ID',
    base_id        BIGINT    NOT NULL COMMENT '知识库ID',
    chunk_index    INT       NOT NULL COMMENT '分片序号',
    content        TEXT      NOT NULL COMMENT '分片内容',
    embedding      MEDIUMTEXT          DEFAULT NULL COMMENT '向量数据(JSON)',
    token_count    INT                DEFAULT NULL COMMENT 'Token数量',
    created_at     DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_knowledge_id (knowledge_id),
    KEY idx_base_id (base_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识向量分片表';

-- ------------------------------------------------------------
-- 9. 规则模板表 (AI编码规则 Agent)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS rule_template;
CREATE TABLE rule_template (
    id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id         BIGINT                 DEFAULT NULL COMMENT '用户ID(系统模板为NULL)',
    name            VARCHAR(128)  NOT NULL COMMENT '模板名称',
    category        VARCHAR(32)   NOT NULL COMMENT '分类: global/project/coding_standard/documentation/ai_tool',
    source_type     VARCHAR(32)   NOT NULL COMMENT '来源: system/user/ai',
    target_tool     VARCHAR(32)            DEFAULT NULL COMMENT '目标AI工具: trae/cursor/copilot等',
    file_name       VARCHAR(256)  NOT NULL COMMENT '文件名,如.trae/project_rules.md',
    content         LONGTEXT      NOT NULL COMMENT '模板内容(Markdown)',
    description     VARCHAR(512)           DEFAULT NULL COMMENT '描述',
    is_system       TINYINT       NOT NULL DEFAULT 0 COMMENT '是否系统模板: 0-否, 1-是',
    is_public       TINYINT       NOT NULL DEFAULT 0 COMMENT '是否公开: 0-私有, 1-公开',
    use_count       INT           NOT NULL DEFAULT 0 COMMENT '使用次数',
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_category (category),
    KEY idx_is_system (is_system),
    KEY idx_is_public (is_public),
    FULLTEXT KEY ft_name_content (name, content)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='规则模板表';

-- ------------------------------------------------------------
-- 10. 规则拉取配置表 (AI编码规则 Agent)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS rule_config;
CREATE TABLE rule_config (
    id                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id           BIGINT        NOT NULL COMMENT '用户ID',
    template_id       BIGINT        NOT NULL COMMENT '模板ID',
    project_path      VARCHAR(512)  NOT NULL COMMENT '项目本地路径',
    target_path       VARCHAR(512)  NOT NULL COMMENT '目标文件相对路径',
    conflict_strategy VARCHAR(32)   NOT NULL DEFAULT 'ASK' COMMENT '冲突策略: OVERWRITE/KEEP_LOCAL/MERGE/RENAME/ASK',
    file_hash         VARCHAR(64)            DEFAULT NULL COMMENT '上次拉取的文件哈希',
    last_pull_at      DATETIME               DEFAULT NULL COMMENT '上次拉取时间',
    pull_count        INT           NOT NULL DEFAULT 0 COMMENT '拉取次数',
    auto_pull         TINYINT       NOT NULL DEFAULT 0 COMMENT '是否自动拉取: 0-否, 1-是',
    enabled           TINYINT       NOT NULL DEFAULT 1 COMMENT '是否启用: 0-禁用, 1-启用',
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_template_id (template_id),
    KEY idx_project_path (project_path)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='规则拉取配置表';

-- ------------------------------------------------------------
-- 11. 规则拉取日志表 (AI编码规则 Agent)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS rule_pull_log;
CREATE TABLE rule_pull_log (
    id                 BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id            BIGINT        NOT NULL COMMENT '用户ID',
    template_id        BIGINT        NOT NULL COMMENT '模板ID',
    config_id          BIGINT                 DEFAULT NULL COMMENT '配置ID',
    target_path        VARCHAR(512)  NOT NULL COMMENT '目标文件路径',
    has_conflict       TINYINT       NOT NULL DEFAULT 0 COMMENT '是否有冲突: 0-否, 1-是',
    conflict_type      VARCHAR(32)            DEFAULT NULL COMMENT '冲突类型: CONTENT_MODIFIED/FILE_NOT_EXISTS/DELETE_CONFLICT',
    conflict_strategy  VARCHAR(32)            DEFAULT NULL COMMENT '使用的冲突策略',
    status             VARCHAR(32)   NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/RESOLVED/IGNORED',
    local_content      LONGTEXT               DEFAULT NULL COMMENT '拉取时的本地内容',
    remote_content     LONGTEXT               DEFAULT NULL COMMENT '远程模板内容',
    merged_content     LONGTEXT               DEFAULT NULL COMMENT '合并后的内容',
    diff_result        LONGTEXT               DEFAULT NULL COMMENT '差异结果',
    resolved_at        DATETIME               DEFAULT NULL COMMENT '解决时间',
    resolved_by        BIGINT                 DEFAULT NULL COMMENT '解决人(用户ID)',
    created_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_template_id (template_id),
    KEY idx_config_id (config_id),
    KEY idx_status (status),
    KEY idx_has_conflict (has_conflict)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='规则拉取日志表';

-- ------------------------------------------------------------
-- 初始化 Agent 数据 (与 DataInitializer 一致, 可重复执行需先清空或改 INSERT IGNORE)
-- ------------------------------------------------------------
INSERT INTO sys_agent (name, code, description, module_name, icon, status) VALUES
('技术文章Agent', 'article', '帮助您生成高质量的技术文章，包括标题生成、概要生成、正文生成等功能', 'agent.article', 'https://api.iconify.design/material-symbols/article.svg', 1),
('代码审查Agent', 'code-review', '帮助您审查代码质量，发现潜在问题和安全漏洞', 'agent-code-review', 'https://api.iconify.design/material-symbols/code-review.svg', 1),
('文档生成Agent', 'doc', '帮助您生成各类技术文档，如API文档、需求文档等', 'agent-doc', 'https://api.iconify.design/material-symbols/file-document.svg', 1),
('个人知识库', 'knowledge', '构建您的专属知识库，支持文档导入、智能问答、语义搜索', 'agent.knowledge', 'https://api.iconify.design/material-symbols/library-books.svg', 1),
('AI编码规则Agent', 'agent-rules', '统一管理您的AI编码规则，支持模板管理、规则拉取、冲突处理、AI生成规则', 'agent.rules', 'https://api.iconify.design/material-symbols/rule.svg', 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    module_name = VALUES(module_name),
    icon = VALUES(icon),
    status = VALUES(status);

-- ------------------------------------------------------------
-- 初始化系统规则模板
-- ------------------------------------------------------------
INSERT INTO rule_template (name, category, source_type, target_tool, file_name, content, description, is_system, is_public, use_count) VALUES
('全局开发规范', 'global', 'system', NULL, 'global_rules.md', 
'# 全局开发规范

## 代码质量
- 所有提交的代码必须通过编译，无警告
- 遵循SOLID原则和DRY原则
- 保持函数单一职责，单个函数不超过50行
- 避免重复代码，公共逻辑抽离为工具类

## 版本控制
- Commit message使用Conventional Commits规范
- 分支命名: feature/xxx, bugfix/xxx, hotfix/xxx
- 禁止直接向main/master分支提交代码
- 每次提交必须关联Issue或任务

## 测试要求
- 核心业务逻辑必须有单元测试，覆盖率不低于80%
- 修复Bug必须先写测试用例复现问题
- 新功能必须包含集成测试

## 安全规范
- 所有用户输入必须进行参数校验
- 禁止在日志中输出敏感信息(密码、密钥等)
- SQL必须使用预编译语句，禁止拼接SQL
- 敏感数据必须加密存储',
'通用的全局开发规范，适用于所有项目', 1, 1, 0),

('Java编码规范', 'coding_standard', 'system', NULL, 'java_coding_standard.md',
'# Java编码规范

## 命名规范
- 类名使用大驼峰命名法: UserService, OrderController
- 方法名使用小驼峰命名法: getUserById, createOrder
- 常量使用全大写下划线分隔: MAX_PAGE_SIZE
- 包名全小写，域名反序: com.example.project

## 代码格式
- 使用4空格缩进，禁止使用Tab
- 每行代码不超过120字符
- 方法之间空一行，逻辑块之间空一行
- 大括号风格：左括号在行尾，右括号独占一行

## 注释规范
- 类必须包含Javadoc注释，说明功能、作者、日期
- 公共方法必须包含Javadoc注释，说明参数、返回值、异常
- 复杂的业务逻辑必须添加行内注释
- TODO注释必须关联Issue

## 异常处理
- 禁止捕获Throwable和Exception后不处理
- 自定义异常必须包含错误码和错误信息
- 全局异常处理器统一处理异常返回
- 必须打印完整的异常栈信息

## 性能优化
- 避免在循环中创建不必要的对象
- 使用StringBuilder处理字符串拼接
- 优先使用局部变量，减少GC压力
- 数据库操作使用批量处理',
'Java语言编码规范，遵循阿里巴巴Java开发手册', 1, 1, 0),

('TypeScript编码规范', 'coding_standard', 'system', NULL, 'typescript_coding_standard.md',
'# TypeScript编码规范

## 命名规范
- 类/接口/类型使用大驼峰: UserService, IUserRepository
- 函数/变量使用小驼峰: getUserById, userName
- 常量使用全大写下划线: MAX_PAGE_SIZE
- 组件文件名使用大驼峰: UserProfile.tsx

## 类型定义
- 禁止使用any类型，使用unknown代替
- 优先使用interface定义对象类型
- 联合类型和交叉类型用于复杂场景
- 所有公共API必须显式声明返回类型

## React规范
- 函数组件优先于类组件
- 使用TypeScript泛型定义Props和State
- Hooks必须在顶层调用，不能在条件判断中使用
- 自定义Hook使用use前缀命名

## 代码组织
- 每个文件只导出一个主要类或函数
- 导入分组：第三方库、内部模块、样式
- 导出类型使用单独的type-only导入
- 绝对路径导入优先于相对路径',
'TypeScript和React编码规范', 1, 1, 0),

('Python编码规范', 'coding_standard', 'system', NULL, 'python_coding_standard.md',
'# Python编码规范

## 命名规范
- 类使用大驼峰: UserService
- 函数/变量使用蛇形命名: get_user_by_id, user_name
- 常量使用全大写下划线: MAX_PAGE_SIZE
- 私有成员使用单下划线前缀: _internal_method

## 代码格式
- 遵循PEP 8规范，使用4空格缩进
- 每行代码不超过88字符
- 使用双引号作为字符串默认引号
- 导入按标准库、第三方库、本地模块分组

## 类型提示
- 所有公共函数必须添加类型注解
- 使用typing模块定义复杂类型
- 优先使用TypeVar和泛型
- 避免使用Optional，考虑默认值

## 异常处理
- 捕获具体异常，禁止裸except
- 上下文管理器(with语句)管理资源
- 自定义异常继承自合适的基类
- 记录异常上下文信息便于调试',
'Python语言编码规范，遵循PEP 8', 1, 1, 0),

('任务完结文档规范', 'documentation', 'system', NULL, 'task_completion_template.md',
'# 任务完结报告

## 任务信息
- **任务编号**: XXX
- **任务名称**: XXX
- **开发人员**: XXX
- **开始日期**: YYYY-MM-DD
- **完成日期**: YYYY-MM-DD
- **实际工时**: XX小时

## 需求概述
简述任务的业务背景和目标。

## 实现方案
### 技术选型
列出使用的技术栈和框架。

### 架构设计
描述核心设计思路、数据流、关键算法。

### 主要改动
- 新增文件: 
- 修改文件:
- 数据库变更:
- 配置变更:

## 测试情况
### 单元测试
- 测试用例数: XX
- 覆盖率: XX%
- 关键测试场景:

### 集成测试
列出测试的场景和结果。

## 风险与问题
- 已知问题:
- 潜在风险:
- 后续优化建议:

## 部署说明
- 部署步骤:
- 依赖服务:
- 回滚方案:

## 变更记录
| 版本 | 日期 | 变更内容 | 变更人 |
|------|------|----------|--------|
| 1.0  |      | 初始版本 |        |',
'任务完结后的标准文档模板', 1, 1, 0),

('API文档规范', 'documentation', 'system', NULL, 'api_documentation_standard.md',
'# API文档规范

## 文档结构
每个API文档必须包含以下部分：

### 1. 接口概述
- 接口名称
- 功能描述
- 接口版本

### 2. 请求信息
- HTTP方法
- 请求URL
- 请求头

### 3. 请求参数
#### Query参数
| 参数名 | 类型 | 必填 | 描述 | 示例 |
|--------|------|------|------|------|

#### Body参数
```json
{
  "field1": "value1",
  "field2": 123
}
```

### 4. 响应信息
#### 成功响应
```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

#### 错误响应
| 错误码 | 描述 | 解决方案 |
|--------|------|----------|

### 5. 权限说明
- 需要的角色/权限
- Token获取方式

### 6. 调用示例
```bash
curl -X POST http://api.example.com/v1/users \\
  -H "Authorization: Bearer <token>" \\
  -H "Content-Type: application/json" \\
  -d "{""name"": ""test""}"
```',
'API接口文档规范', 1, 1, 0),

('Trae配置规则', 'ai_tool', 'system', 'trae', '.trae/global_rules.md',
'# Trae全局配置规则

## 核心原则
- 理解上下文：优先阅读项目结构、README、现有代码风格
- 遵循规范：严格遵守项目的编码规范和架构模式
- 增量修改：小步迭代，每次修改控制在合理范围
- 解释决策：复杂改动必须说明设计思路和权衡

## 代码生成规则
- 优先使用项目中已有的库和工具类
- 遵循现有的命名约定和代码风格
- 生成的代码必须包含必要的注释
- 公共API必须包含类型定义

## 重构规则
- 重构前必须确认理解原有逻辑
- 提供重构前后的对比说明
- 确保重构不改变原有行为
- 大型重构拆分为多个步骤

## 安全规则
- 不得在代码中硬编码密钥或密码
- 必须处理用户输入验证
- 必须考虑SQL注入、XSS等攻击
- 敏感数据必须脱敏处理

## 测试规则
- 新功能必须包含对应的测试用例
- 修复Bug必须先写测试复现
- 测试用例应覆盖边界场景
- 测试代码必须清晰易读',
'Trae AI编码助手的全局配置规则', 1, 1, 0),

('Cursor配置规则', 'ai_tool', 'system', 'cursor', '.cursor/rules.md',
'# Cursor配置规则

## 核心指令
- 理解项目：先分析项目结构、技术栈、代码风格
- 遵循惯例：严格遵循项目的约定和模式
- 完整实现：提供完整可运行的代码，不省略关键部分
- 思考过程：对于复杂问题，展示思考过程和替代方案

## 代码风格
- 语言特性：充分利用编程语言的现代特性
- 可读性优先：代码首先是写给人看的
- 避免过早优化：先正确后优化
- 依赖管理：谨慎引入新的依赖

## 对话规则
- 提问澄清：需求不明确时主动提问
- 分步执行：复杂任务拆分为多个步骤
- 及时反馈：遇到问题及时说明
- 提供选项：给出多种实现方案并说明优缺点

## Bug修复
- 定位根因：找到问题的根本原因，而不是表面现象
- 影响分析：评估修复对其他部分的影响
- 回归测试：确保修复不会引入新问题
- 预防措施：总结如何避免类似问题

## 代码审查
- 客观性：基于事实和标准，不针对个人
- 建设性：提供具体的改进建议
- 优先级：区分严重问题和风格问题
- 正向反馈：好的实现也要给予肯定',
'Cursor编辑器的AI规则配置', 1, 1, 0),

('项目README规范', 'project', 'system', NULL, 'README.md',
'# 项目名称

## 项目简介
简要描述项目的目标、功能和价值。

## 技术栈
- 前端: React 18 + TypeScript + Vite
- 后端: Spring Boot 3.x + JPA + MySQL
- 部署: Docker + Kubernetes

## 快速开始

### 环境要求
- Node.js >= 18
- JDK >= 17
- MySQL >= 8.0
- Maven >= 3.8

### 开发环境搭建

1. 克隆项目
```bash
git clone https://github.com/xxx/xxx.git
cd xxx
```

2. 启动后端
```bash
cd backend
mvn spring-boot:run
```

3. 启动前端
```bash
cd frontend
npm install
npm run dev
```

4. 访问应用: http://localhost:3000

## 项目结构
```
├── backend/          # 后端代码
├── frontend/         # 前端代码
├── docs/            # 文档
├── docker/          # Docker配置
└── sql/             # 数据库脚本
```

## 开发规范
- [编码规范](./docs/coding-standard.md)
- [Git工作流](./docs/git-workflow.md)
- [代码审查规范](./docs/code-review.md)

## 部署说明
详见 [部署文档](./docs/deployment.md)

## 贡献指南
详见 [贡献指南](./CONTRIBUTING.md)

## License
MIT License',
'标准的项目README模板', 1, 1, 0)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    category = VALUES(category),
    source_type = VALUES(source_type),
    target_tool = VALUES(target_tool),
    file_name = VALUES(file_name),
    content = VALUES(content),
    description = VALUES(description),
    is_system = VALUES(is_system),
    is_public = VALUES(is_public);
