package com.example.agentplatform.agent.article.service.impl;

import com.example.agentplatform.agent.article.dto.GenerateRequestDTO;
import com.example.agentplatform.agent.article.dto.GenerateResult;
import com.example.agentplatform.agent.article.service.GenerateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GenerateServiceImpl implements GenerateService {
    
    @Override
    public GenerateResult generateTitle(GenerateRequestDTO request) {
        String topic = request.getTopic();
        String keywords = request.getKeywords() != null ? String.join(", ", request.getKeywords()) : "";
        
        String simulatedTitles = String.format(
            "【深入理解%s】\n" +
            "%s实战指南\n" +
            "%s从入门到精通\n" +
            "详解%s核心原理\n" +
            "%s最佳实践与技巧",
            topic, topic, topic, topic, topic
        );
        
        List<String> titles = Arrays.stream(simulatedTitles.split("\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        
        log.info("为主题 '{}' 生成了 {} 个标题", topic, titles.size());
        return GenerateResult.fromItems("title", titles);
    }
    
    @Override
    public GenerateResult generateSummary(GenerateRequestDTO request) {
        String title = request.getTitle();
        String content = request.getContent() != null ? request.getContent() : "";
        
        String summary = String.format(
            "本文深入探讨了%s相关技术，通过理论讲解与实践案例相结合的方式，帮助读者全面理解其核心概念和应用场景。" +
            "文章涵盖了基础原理、核心特性、实践技巧等多个方面，适合有一定基础的开发者学习参考。",
            title
        );
        
        log.info("为文章 '{}' 生成了概要", title);
        return GenerateResult.fromContent("summary", summary);
    }
    
    @Override
    public GenerateResult generateContent(GenerateRequestDTO request) {
        String title = request.getTitle();
        String outline = request.getOutline() != null ? request.getOutline() : "一、介绍\n二、核心概念\n三、实践案例\n四、总结";
        
        String content = String.format(
            "# %s\n\n" +
            "## 一、介绍\n\n" +
            "在当今技术飞速发展的时代，%s已经成为开发者必备的技能之一。本文将带你深入了解%s的核心概念和实践应用。\n\n" +
            "## 二、核心概念\n\n" +
            "### 2.1 基本定义\n\n" +
            "%s是一种重要的技术/概念，它在现代软件开发中扮演着关键角色。\n\n" +
            "### 2.2 核心特性\n\n" +
            "- **特性一**: 高性能\n" +
            "- **特性二**: 易于使用\n" +
            "- **特性三**: 可扩展性强\n\n" +
            "## 三、实践案例\n\n" +
            "```java\n" +
            "// 示例代码\n" +
            "public class Example {\n" +
            "    public static void main(String[] args) {\n" +
            "        System.out.println(\"Hello, World!\");\n" +
            "    }\n" +
            "}\n" +
            "```\n\n" +
            "## 四、总结\n\n" +
            "通过本文的学习，相信你已经对%s有了全面的了解。希望这些知识能够帮助你在实际项目中更好地应用%s技术。",
            title, title, title, title, title, title
        );
        
        log.info("为文章 '{}' 生成了正文内容", title);
        return GenerateResult.fromContent("content", content);
    }
}