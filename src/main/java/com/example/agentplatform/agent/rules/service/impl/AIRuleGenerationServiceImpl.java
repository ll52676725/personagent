package com.example.agentplatform.agent.rules.service.impl;

import com.example.agentplatform.agent.rules.dto.AIRuleGenerateDTO;
import com.example.agentplatform.agent.rules.dto.RuleTemplateCreateDTO;
import com.example.agentplatform.agent.rules.entity.RuleTemplate;
import com.example.agentplatform.agent.rules.enums.RuleCategory;
import com.example.agentplatform.agent.rules.service.AIRuleGenerationService;
import com.example.agentplatform.agent.rules.service.RuleTemplateService;
import com.example.agentplatform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AI规则生成服务实现类
 * <p>基于Spring AI调用大模型生成各类AI编码规则模板
 * <p>支持全局规则、项目规则、编码规范、文档规范、AI工具规则等多种类型
 * 
 * @author System
 * @since 2025-01-01
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AIRuleGenerationServiceImpl implements AIRuleGenerationService {

    private final ChatClient chatClient;
    private final RuleTemplateService ruleTemplateService;

    @Value("${agent.platform.ai.fallback-enabled:true}")
    private boolean fallbackEnabled;

    @Value("${agent.platform.default-model:spring-ai}")
    private String modelName;

    private static final String RULE_GENERATION_SYSTEM_PROMPT = """
        你是一个资深软件开发专家和AI编码助手配置专家。
        你擅长根据用户需求生成高质量的AI编码规则、项目规范、编码标准等配置文件。
        
        你的任务是生成专业、实用、可直接落地的规则文件。
        
        生成规则时请遵循以下原则：
        1. 内容要具体、可执行，避免空泛的描述
        2. 符合行业最佳实践和主流规范
        3. 考虑实际开发场景，规则要可落地
        4. 格式清晰，易于阅读和维护
        5. 包含具体的示例和反例对比
        6. 按模块/章节组织，逻辑清晰
        7. 使用Markdown格式，标题层级分明
        
        只返回生成的规则内容，不要有任何其他说明文字。
        """;

    private static final String GLOBAL_RULES_TEMPLATE = """
        # 全局开发规则
        
        ## 1. 代码质量原则
        
        ### 1.1 基本原则
        - 代码必须清晰、易读、可维护
        - 遵循DRY（Don't Repeat Yourself）原则
        - 保持单一职责，每个模块/函数只做一件事
        
        ### 1.2 命名规范
        - 变量名使用有意义的英文单词，禁止使用拼音
        - 布尔变量以is/has/can开头，如isValid, hasPermission
        - 常量使用全大写下划线分隔，如MAX_RETRY_COUNT
        
        ## 2. 注释规范
        
        ### 2.1 必要注释
        - 所有public类和方法必须添加JavaDoc/TSDoc注释
        - 复杂业务逻辑必须添加行内注释说明设计意图
        - 算法实现必须说明原理和时间复杂度
        
        ### 2.2 注释质量
        - 注释要说明"为什么"，而不是"是什么"
        - 禁止无意义注释，如 `int a = 1; // 定义a等于1`
        - 代码修改时同步更新相关注释
        
        ## 3. 安全规范
        
        ### 3.1 敏感信息处理
        - 禁止在代码中硬编码密码、密钥、Token
        - 禁止在日志中打印敏感信息
        - 使用环境变量或配置中心管理敏感配置
        
        ### 3.2 输入验证
        - 所有外部输入必须进行合法性校验
        - SQL查询使用参数化查询，防止SQL注入
        - 输出用户内容时进行XSS转义
        
        ## 4. 版本控制规范
        
        ### 4.1 Commit规范
        - 使用Conventional Commits格式：type(scope): subject
        - type可选值：feat, fix, docs, style, refactor, test, chore
        - 每个Commit只包含一个逻辑变更
        
        ### 4.2 分支管理
        - 主分支：main/master，保护分支，不允许直接推送
        - 开发分支：develop，功能开发完成后合并
        - 功能分支：feature/xxx，从develop创建
        - 修复分支：bugfix/xxx，从develop创建
        
        ## 5. 开发流程规范
        
        ### 5.1 编码前准备
        - 理解需求，明确功能边界
        - 设计技术方案，必要时进行技术评审
        - 确认依赖版本和兼容性
        
        ### 5.2 编码过程
        - 小步提交，频繁验证
        - 保持代码整洁，及时重构
        - 编写单元测试，核心代码覆盖率不低于80%
        
        ### 5.3 代码审查
        - 代码必须经过同行评审才能合并
        - 审查重点：逻辑正确性、性能、安全性、可读性
        - 及时响应审查意见，认真讨论改进
        
        ## 6. 异常处理规范
        
        ### 6.1 异常捕获原则
        - 只捕获能处理的异常，不要吞异常
        - 捕获异常后必须记录日志，包含异常栈
        - 尽早抛出，延迟处理
        
        ### 6.2 异常转换
        - 底层异常转换为业务异常后抛出
        - 异常信息要清晰，便于问题定位
        - 包含必要的上下文信息
        """;

    private static final String CODING_STANDARD_TEMPLATE = """
        # 代码规范
        
        ## 1. Java 编码规范
        
        ### 1.1 命名规范
        - 类名：大驼峰（UpperCamelCase），如UserService
        - 方法名：小驼峰（lowerCamelCase），如getUserById
        - 变量名：小驼峰，如userId
        - 常量：全大写下划线分隔，如MAX_PAGE_SIZE
        - 包名：全小写，使用域名反转，如com.example.project
        
        ### 1.2 代码格式
        - 使用4空格缩进，禁止使用Tab
        - 每行不超过120字符
        - 方法之间空一行分隔
        - 大括号使用K&R风格，左大括号不换行
        
        ### 1.3 最佳实践
        - 使用try-with-resources管理资源
        - 避免空指针，使用Optional或@NonNull注解
        - 字符串拼接使用StringBuilder或String.format
        - 集合遍历使用增强for循环或Stream API
        
        ## 2. TypeScript 编码规范
        
        ### 2.1 命名规范
        - 接口名：大驼峰，可加I前缀，如IUserService
        - 类型别名：大驼峰，如UserInfo
        - 枚举名：大驼峰，如UserStatus
        - 组件名：大驼峰，如UserList.tsx
        
        ### 2.2 类型定义
        - 优先使用interface定义对象类型
        - 使用type定义联合类型和交叉类型
        - 避免使用any，必要时使用unknown
        - 为所有函数参数和返回值添加类型注解
        
        ### 2.3 React规范
        - 函数组件使用箭头函数
        - 使用React Hooks管理状态和副作用
        - 避免在循环/条件/嵌套函数中调用Hooks
        - 使用useCallback和useMemo优化性能
        
        ## 3. 代码质量检查
        
        ### 3.1 静态检查
        - Java：启用所有SonarQube规则，问题数为0才能合并
        - TypeScript：开启strict模式，noImplicitAny为true
        - 禁用规则必须在代码中添加说明注释
        
        ### 3.2 代码复杂度
        - 单个方法圈复杂度不超过15
        - 类的方法数不超过30
        - 单个文件行数不超过500行
        
        ## 4. 性能规范
        
        ### 4.1 通用原则
        - 避免在循环中执行数据库/网络操作
        - 合理使用缓存，注意缓存失效策略
        - 使用批量操作减少IO次数
        
        ### 4.2 数据库规范
        - 查询必须有索引覆盖
        - 避免SELECT *，只查询需要的字段
        - 使用分页查询，限制单次返回数据量
        
        ## 5. 示例对比
        
        ### ❌ 不好的写法
        ```java
        public void process(String name) {
            if (name != null) {
                System.out.println(name);
            }
        }
        ```
        
        ### ✅ 好的写法
        ```java
        /**
         * 处理用户名
         * @param name 用户名，不能为空
         * @throws IllegalArgumentException 当name为null时抛出
         */
        public void processUserName(String name) {
            Assert.hasText(name, "用户名不能为空");
            log.debug("处理用户名: {}", name);
        }
        ```
        """;

    private static final String DOCUMENTATION_RULES_TEMPLATE = """
        # 文档规范
        
        ## 1. 代码注释规范
        
        ### 1.1 JavaDoc 规范
        - 所有public类必须添加类注释，说明功能、职责、使用场景
        - 所有public方法必须添加方法注释，说明功能、参数、返回值、异常
        - 使用@author、@since、@see、@param、@return、@throws等标签
        
        示例：
        ```java
        /**
         * 用户服务类
         * <p>提供用户注册、登录、信息查询等核心功能
         * <p>主要功能包括：
         * <ul>
         *   <li>用户注册 - 新用户创建账号</li>
         *   <li>用户登录 - 验证用户身份</li>
         *   <li>信息查询 - 查询用户详细信息</li>
         * </ul>
         * 
         * @author System
         * @since 2025-01-01
         * @see UserController
         * @see UserRepository
         */
        @Service
        public class UserService {
            /**
             * 用户登录
             * <p>验证用户名和密码，生成访问令牌
             * 
             * @param username 用户名
             * @param password 密码（明文）
             * @return 登录结果，包含访问令牌和用户信息
             * @throws BusinessException 用户名或密码错误时抛出
             */
            public LoginResult login(String username, String password) {
                // 实现逻辑
            }
        }
        ```
        
        ### 1.2 TypeScript/前端注释规范
        - 所有接口、类型定义必须添加注释
        - 组件props和state必须添加注释
        - 复杂逻辑函数必须添加注释
        
        示例：
        ```typescript
        /**
         * 用户信息数据结构
         * @property id - 用户唯一标识
         * @property username - 用户名
         * @property email - 邮箱地址
         * @property avatar - 头像URL
         * @property role - 用户角色
         */
        export interface UserInfo {
          id: string;
          username: string;
          email: string;
          avatar?: string;
          role: UserRole;
        }
        ```
        
        ## 2. API 文档规范
        
        ### 2.1 接口文档要求
        - 所有对外API必须有完整文档
        - 包含接口路径、HTTP方法、请求参数、响应格式
        - 提供完整的请求示例和响应示例
        - 说明可能的错误码和错误信息
        
        ### 2.2 文档格式
        - 使用OpenAPI 3.0规范
        - 接口路径使用RESTful风格
        - 参数说明要清晰，包含是否必填、类型、示例值
        
        示例：
        ```
        ## 用户登录
        
        ### 接口说明
        用户登录验证接口，验证成功后返回访问令牌。
        
        ### 请求信息
        - 路径：`POST /api/v1/auth/login`
        - 方法：`POST`
        - Content-Type: `application/json`
        
        ### 请求参数
        | 参数名 | 类型 | 必填 | 说明 | 示例 |
        |--------|------|------|------|------|
        | username | string | 是 | 用户名 | admin |
        | password | string | 是 | 密码 | 123456 |
        
        ### 请求示例
        ```json
        {
          "username": "admin",
          "password": "123456"
        }
        ```
        
        ### 响应示例
        ```json
        {
          "code": 200,
          "message": "success",
          "data": {
            "accessToken": "eyJhbGciOiJIUzI1NiIs...",
            "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
            "expiresIn": 7200
          }
        }
        ```
        ```
        
        ## 3. 项目文档规范
        
        ### 3.1 README.md
        项目根目录必须包含README.md，包含以下内容：
        1. 项目简介和功能特性
        2. 技术栈说明
        3. 环境要求和快速开始
        4. 项目结构说明
        5. 配置说明
        6. 部署方式
        
        ### 3.2 设计文档
        重要功能必须编写设计文档，包含：
        1. 需求背景和目标
        2. 架构设计和模块划分
        3. 核心流程图
        4. 数据模型设计
        5. 接口设计
        6. 异常处理和降级方案
        
        ### 3.3 数据库变更文档
        所有数据库变更必须有文档记录：
        1. 变更原因和影响范围
        2. SQL脚本
        3. 回滚方案
        4. 执行时间和执行人
        
        ## 4. 日志规范
        
        ### 4.1 日志级别
        - error：系统错误、异常、关键功能失败，必须包含异常对象
        - warn：警告信息、非预期但可恢复的情况、降级处理
        - info：关键业务流程节点、重要操作、状态变更
        - debug：调试信息、详细执行过程（生产环境可关闭）
        - trace：最详细的追踪信息（极少使用）
        
        ### 4.2 日志格式
        - 使用占位符，禁止字符串拼接
        - 业务日志必须包含用户标识、操作类型、关键参数、执行结果
        - 异常日志必须包含异常对象
        
        ### ❌ 错误示例
        ```java
        log.info("处理完成，找到" + count + "个问题");
        log.error("发生错误: " + e.getMessage());
        ```
        
        ### ✅ 正确示例
        ```java
        log.info("【用户登录】登录成功，用户ID: {}, 耗时: {}ms", userId, duration);
        log.error("【用户登录】登录失败，用户ID: {}, 错误: {}", userId, e.getMessage(), e);
        ```
        
        ## 5. 任务完结文档
        
        每个功能/任务完成后必须输出完结文档，包含：
        1. 任务背景和目标
        2. 实现方案概述
        3. 完成的功能列表
        4. 代码变更统计
        5. 测试情况（单元测试、集成测试）
        6. 上线注意事项
        7. 遗留问题和后续优化建议
        """;

    private static final String AI_TOOL_RULES_TEMPLATE = """
        # AI 编码助手配置规则
        
        ## 1. Trae 助手配置
        
        ### 1.1 全局系统提示词
        ```
        你是一个资深的全栈开发工程师，精通Java、TypeScript、React、Spring Boot等技术栈。
        
        你的工作方式：
        1. 先理解需求，确认边界和约束
        2. 设计技术方案，说明选型理由
        3. 分步实现，每步给出清晰的解释
        4. 遵循项目的编码规范和设计模式
        5. 编写完整可运行的代码，包含必要的注释
        6. 提供测试用例和验证方法
        
        代码原则：
        - 清晰优先于性能，可维护性优先于小聪明
        - 遵循DRY原则，避免重复代码
        - 错误处理要完善，不能吞异常
        - 敏感信息不能硬编码
        - 所有外部输入必须验证
        
        回答风格：
        - 先给出方案设计，再给出具体代码
        - 关键代码添加注释说明设计思路
        - 复杂问题分步骤解释
        - 给出代码后说明如何验证
        ```
        
        ### 1.2 项目级提示词
        ```
        当前项目：${projectName}
        技术栈：Spring Boot 3.x + React 18 + TypeScript 5.x
        数据库：MySQL 8.x
        缓存：Redis 7.x
        
        项目规范：
        1. 后端遵循Spring Boot最佳实践，分层清晰
        2. 前端使用React Hooks + Zustand状态管理
        3. API接口遵循RESTful规范
        4. 所有接口需要权限控制
        5. 数据库表名、字段名使用下划线命名
        
        代码风格：
        - Java类使用@Slf4j、@RequiredArgsConstructor等Lombok注解
        - Controller返回统一Result包装
        - Service层使用@Transactional管理事务
        - 异常使用自定义BusinessException
        - TypeScript使用strict模式，避免any
        
        请严格遵守以上规范生成代码。
        ```
        
        ## 2. Cursor 配置
        
        ### 2.1 全局规则
        - 代码生成遵循项目的.eslintrc、.prettierrc配置
        - 自动导入按字母排序
        - 生成的代码包含完整的类型注解
        - React组件默认使用函数组件+Hooks
        - 优先使用项目已有的组件和工具函数
        
        ### 2.2 自定义指令
        ```json
        {
          "review": "请审查以下代码，检查是否有bug、性能问题、安全漏洞，并给出改进建议",
          "explain": "请详细解释以下代码的功能和实现原理",
          "test": "请为以下代码编写完整的单元测试用例，覆盖正常、边界、异常场景",
          "refactor": "请重构以下代码，提高可读性和可维护性，保持功能不变",
          "optimize": "请分析以下代码的性能问题，并给出优化方案"
        }
        ```
        
        ## 3. GitHub Copilot 配置
        
        ### 3.1 提示词建议
        - 开头用注释描述需求，越详细越好
        - 给出函数签名和参数说明
        - 提供几个输入输出示例
        - 说明使用的技术栈和框架
        
        示例：
        ```
        // 函数：验证邮箱格式
        // 参数：email - 待验证的邮箱字符串
        // 返回：true-格式正确，false-格式错误
        // 要求：
        // 1. 使用正则表达式验证
        // 2. 支持常见域名后缀
        // 3. 不区分大小写
        // 示例：
        //   isValidEmail("test@example.com") -> true
        //   isValidEmail("invalid-email") -> false
        ```
        
        ## 4. 通用AI编码规则
        
        ### 4.1 代码生成要求
        1. 生成的代码必须可以直接运行
        2. 包含必要的import语句
        3. 复杂逻辑添加注释说明
        4. 边缘情况和异常要处理
        5. 遵循项目的命名规范
        
        ### 4.2 禁止事项
        1. 不要生成有安全漏洞的代码
        2. 不要硬编码敏感信息
        3. 不要忽略错误处理
        4. 不要使用已废弃的API
        5. 不要生成过于复杂的单文件代码
        
        ### 4.3 代码审查检查清单
        AI生成代码后，需要人工检查：
        - [ ] 逻辑是否正确，有没有理解错需求
        - [ ] 是否有安全漏洞
        - [ ] 错误处理是否完善
        - [ ] 性能是否有问题
        - [ ] 是否符合项目规范
        - [ ] 有没有测试用例
        - [ ] 注释是否清晰
        """;

    @Override
    public String generateRules(AIRuleGenerateDTO dto) {
        log.info("【AI规则生成】生成规则，分类: {}, 工具: {}", dto.getCategory(), dto.getTargetTool());
        
        String systemPrompt = buildSystemPrompt(dto);
        String userPrompt = buildUserPrompt(dto);
        
        try {
            ChatResponse response = callChatApi(systemPrompt, userPrompt);
            String content = response.getResult().getOutput().getContent();
            Integer tokens = response.getMetadata().getUsage() != null ?
                response.getMetadata().getUsage().getTotalTokens().intValue() : null;
            
            log.info("【AI规则生成】规则生成成功，token消耗: {}", tokens);
            return content;
            
        } catch (Exception e) {
            log.error("【AI规则生成】AI生成规则失败", e);
            if (fallbackEnabled) {
                return fallbackGenerateRules(dto);
            }
            throw new BusinessException("规则生成失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public RuleTemplate generateAndSaveTemplate(Long userId, AIRuleGenerateDTO dto) {
        log.info("【AI规则生成】用户 {} 生成并保存模板，分类: {}", userId, dto.getCategory());
        
        String content = generateRules(dto);
        
        String fileName = dto.getFileName() != null ? dto.getFileName() 
                : generateDefaultFileName(dto.getCategory());
        
        RuleTemplateCreateDTO createDTO = new RuleTemplateCreateDTO();
        createDTO.setName("AI生成 - " + dto.getDescription().substring(0, Math.min(30, dto.getDescription().length())));
        createDTO.setDescription(dto.getDescription());
        createDTO.setCategory(dto.getCategory());
        createDTO.setSourceType("ai_generated");
        createDTO.setTargetTool(dto.getTargetTool());
        createDTO.setFileName(fileName);
        createDTO.setFilePath("./" + fileName);
        createDTO.setContent(content);
        createDTO.setVersion("1.0.0");
        createDTO.setIsPublic(false);
        
        return ruleTemplateService.createTemplate(userId, createDTO);
    }

    @Override
    public String generateRulesByCategory(String category, String targetTool, String description) {
        AIRuleGenerateDTO dto = new AIRuleGenerateDTO();
        dto.setCategory(category);
        dto.setTargetTool(targetTool);
        dto.setDescription(description);
        return generateRules(dto);
    }

    private String buildSystemPrompt(AIRuleGenerateDTO dto) {
        StringBuilder prompt = new StringBuilder(RULE_GENERATION_SYSTEM_PROMPT);
        
        try {
            RuleCategory category = RuleCategory.fromCode(dto.getCategory());
            prompt.append("\n\n当前生成的规则类型：").append(category.getDesc());
        } catch (Exception e) {
            prompt.append("\n\n当前生成的规则类型：").append(dto.getCategory());
        }
        
        if (dto.getTargetTool() != null && !dto.getTargetTool().isEmpty()) {
            prompt.append("\n目标AI工具：").append(dto.getTargetTool());
        }
        
        if (dto.getCodingLanguage() != null && !dto.getCodingLanguage().isEmpty()) {
            prompt.append("\n主要编程语言：").append(dto.getCodingLanguage());
        }
        
        if (dto.getProjectType() != null && !dto.getProjectType().isEmpty()) {
            prompt.append("\n项目类型：").append(dto.getProjectType());
        }
        
        return prompt.toString();
    }

    private String buildUserPrompt(AIRuleGenerateDTO dto) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("请根据以下需求生成规则文件：\n\n");
        prompt.append("规则描述：").append(dto.getDescription()).append("\n\n");
        
        if (dto.getAdditionalRequirements() != null && !dto.getAdditionalRequirements().isEmpty()) {
            prompt.append("额外要求：\n").append(dto.getAdditionalRequirements()).append("\n\n");
        }
        
        prompt.append("请生成完整的规则内容，使用Markdown格式，结构清晰，内容具体可落地。");
        
        return prompt.toString();
    }

    private ChatResponse callChatApi(String systemPrompt, String userPrompt) {
        List<Message> messages = List.of(
            new SystemMessage(systemPrompt),
            new UserMessage(userPrompt)
        );
        Prompt prompt = new Prompt(messages);
        return chatClient.call(prompt);
    }

    private String fallbackGenerateRules(AIRuleGenerateDTO dto) {
        log.warn("【AI规则生成】降级模式，使用预设模板");
        
        String category = dto.getCategory();
        String result = switch (category) {
            case "global" -> GLOBAL_RULES_TEMPLATE;
            case "coding_standard" -> CODING_STANDARD_TEMPLATE;
            case "documentation" -> DOCUMENTATION_RULES_TEMPLATE;
            case "ai_tool" -> AI_TOOL_RULES_TEMPLATE;
            default -> GLOBAL_RULES_TEMPLATE;
        };
        
        if (dto.getDescription() != null && !dto.getDescription().isEmpty()) {
            result = "<!-- 降级模式：以下为预设模板，可根据实际需求修改 -->\n\n" + result;
        }
        
        return result;
    }

    private String generateDefaultFileName(String category) {
        return switch (category) {
            case "global" -> "global_rules.md";
            case "project" -> "project_rules.md";
            case "coding_standard" -> "coding_standard.md";
            case "documentation" -> "documentation_rules.md";
            case "ai_tool" -> "ai_tool_rules.md";
            default -> "rules.md";
        };
    }
}
