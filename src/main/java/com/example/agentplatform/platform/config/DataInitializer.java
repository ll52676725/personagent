package com.example.agentplatform.platform.config;

import com.example.agentplatform.platform.entity.Agent;
import com.example.agentplatform.platform.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    
    private final AgentRepository agentRepository;
    
    @Override
    public void run(String... args) throws Exception {
        initAgents();
    }
    
    private void initAgents() {
        if (!agentRepository.existsByCode("article")) {
            Agent articleAgent = Agent.builder()
                    .name("技术文章Agent")
                    .code("article")
                    .description("帮助您生成高质量的技术文章，包括标题生成、概要生成、正文生成等功能")
                    .moduleName("agent.article")
                    .icon("https://api.iconify.design/material-symbols/article.svg")
                    .status(1)
                    .build();
            agentRepository.save(articleAgent);
            log.info("初始化技术文章Agent");
        }
        
        if (!agentRepository.existsByCode("code-review")) {
            Agent codeReviewAgent = Agent.builder()
                    .name("代码审查Agent")
                    .code("code-review")
                    .description("帮助您审查代码质量，发现潜在问题和安全漏洞")
                    .moduleName("agent-code-review")
                    .icon("https://api.iconify.design/material-symbols/code-review.svg")
                    .status(1)
                    .build();
            agentRepository.save(codeReviewAgent);
            log.info("初始化代码审查Agent");
        }
        
        if (!agentRepository.existsByCode("doc")) {
            Agent docAgent = Agent.builder()
                    .name("文档生成Agent")
                    .code("doc")
                    .description("帮助您生成各类技术文档，如API文档、需求文档等")
                    .moduleName("agent-doc")
                    .icon("https://api.iconify.design/material-symbols/file-document.svg")
                    .status(1)
                    .build();
            agentRepository.save(docAgent);
            log.info("初始化文档生成Agent");
        }

        if (!agentRepository.existsByCode("knowledge")) {
            Agent knowledgeAgent = Agent.builder()
                    .name("个人知识库")
                    .code("knowledge")
                    .description("构建您的专属知识库，支持文档导入、智能问答、语义搜索")
                    .moduleName("agent.knowledge")
                    .icon("https://api.iconify.design/material-symbols/library-books.svg")
                    .status(1)
                    .build();
            agentRepository.save(knowledgeAgent);
            log.info("初始化个人知识库Agent");
        }
        
        if (!agentRepository.existsByCode("agent-rules")) {
            Agent rulesAgent = Agent.builder()
                    .name("AI编码规则Agent")
                    .code("agent-rules")
                    .description("统一管理您的AI编码规则，支持模板管理、规则拉取、冲突处理、AI生成规则")
                    .moduleName("agent.rules")
                    .icon("https://api.iconify.design/material-symbols/rule.svg")
                    .status(1)
                    .build();
            agentRepository.save(rulesAgent);
            log.info("初始化AI编码规则Agent");
        }

        if (!agentRepository.existsByCode("tech-report")) {
            Agent techReportAgent = Agent.builder()
                    .name("技术汇报Agent")
                    .code("tech-report")
                    .description("架构师专属技术汇报助手，输入场景自动生成专业PPT汇报方案，含执行摘要、架构设计、实施路线、ROI分析等完整内容")
                    .moduleName("agent.techreport")
                    .icon("https://api.iconify.design/material-symbols/present-to-all.svg")
                    .status(1)
                    .build();
            agentRepository.save(techReportAgent);
            log.info("初始化技术汇报Agent");
        }
    }
}