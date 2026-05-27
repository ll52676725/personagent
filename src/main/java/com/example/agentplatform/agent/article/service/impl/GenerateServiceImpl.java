package com.example.agentplatform.agent.article.service.impl;

import com.example.agentplatform.agent.article.dto.GenerateRequestDTO;
import com.example.agentplatform.agent.article.dto.GenerateResult;
import com.example.agentplatform.agent.article.service.GenerateService;
import com.example.agentplatform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.StreamingChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageClient;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GenerateServiceImpl implements GenerateService {

    private final ChatClient chatClient;
    private final StreamingChatClient streamingChatClient;
    private final ImageClient imageClient;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Value("${agent.platform.default-model:spring-ai}")
    private String modelName;

    @Value("${agent.platform.ai.fallback-enabled:true}")
    private boolean fallbackEnabled;

    @Value("${agent.platform.image.provider:trae}")
    private String imageProvider;

    @Value("${agent.platform.image.providers.trae.enabled:true}")
    private boolean traeImageEnabled;

    @Value("${agent.platform.image.providers.trae.base-url:https://trae-api-cn.mchost.guru/api/ide/v1}")
    private String traeImageBaseUrl;

    @Value("${agent.platform.image.providers.trae.default-size:landscape_16_9}")
    private String traeDefaultSize;

    @Value("${agent.platform.image.providers.openai.enabled:false}")
    private boolean openaiImageEnabled;

    private static final String TITLE_SYSTEM_PROMPT = """
        你是一个专业的技术文章标题生成专家，擅长为技术博客创作吸引人的标题。
        
        标题要求：
        1. 必须是技术类文章标题，准确反映技术内容
        2. 包含核心技术关键词，利于 SEO
        3. 风格多样：教程类、深度解析类、实战类、对比类等
        4. 长度控制在 15-30 字之间
        5. 避免使用夸张、标题党风格，保持专业严谨
        6. 返回 5 个不同风格的标题，每行一个
        
        只返回标题列表，不要有任何其他文字说明。
        """;

    private static final String SUMMARY_SYSTEM_PROMPT = """
        你是一个专业的技术文章摘要写作专家。
        
        摘要要求：
        1. 准确概括文章核心内容和主要观点
        2. 突出技术价值和读者收益
        3. 长度控制在 100-200 字之间
        4. 使用第三人称，客观描述
        5. 包含 2-3 个核心技术关键词
        6. 适合作为文章的引言
        
        只返回摘要内容，不要有任何其他说明。
        """;

    private static final String OUTLINE_SYSTEM_PROMPT = """
        你是一个专业的技术文章架构师，擅长设计清晰的文章大纲。
        
        大纲要求：
        1. 使用简洁的标题格式，不要用 Markdown 列表符号
        2. 包含 4-6 个主要章节
        3. 每个主要章节下有 2-3 个子节
        4. 逻辑递进：从基础到进阶，从理论到实践
        5. 符合技术文章的标准结构
        6. 格式示例：
           一、XXX 概述
           1.1 什么是 XXX
           1.2 为什么需要 XXX
           二、核心概念与原理
           2.1 核心组件介绍
           ...
        
        只返回大纲内容，不要有任何其他说明。
        """;

    private static final String CONTENT_SYSTEM_PROMPT = """
        你是一个资深技术作家，擅长撰写高质量的技术教程文章。
        
        重要：文章格式要求（适配头条平台）：
        1. 标题层级清晰：
           - 一级标题：## 标题
           - 二级标题：### 标题
           - 不要使用 # 作为一级标题
        2. 段落分明：每段 3-5 句话，空行分隔
        3. 重点突出：使用加粗标记核心概念
        4. 代码示例：使用 ```java 代码块，代码要完整可运行
        5. 列表使用：使用 - 或 1. 2. 3. 格式
        6. 避免使用复杂的 Markdown 语法（表格、引用块等）
        7. 每节之间有空行过渡
        8. 文章结构：
           - 引言（150字左右，吸引读者）
           - 核心概念讲解（分小节）
           - 技术原理分析
           - 完整的 Java 代码示例（带注释）
           - 实际应用场景
           - 最佳实践和避坑指南
           - 总结（100字左右）
        9. 文章总字数不少于 1500 字
        10. 语言风格：专业但易懂，适合中级开发者
        
        只返回文章内容，不要有任何其他说明。
        """;

    @Override
    public GenerateResult generateTitle(GenerateRequestDTO request) {
        String topic = request.getTopic();
        String keywords = request.getKeywords() != null ? String.join(", ", request.getKeywords()) : "";

        String userPrompt = String.format("""
            请为以下技术主题生成文章标题：
            主题：%s
            关键词：%s
            请生成 5 个不同风格的高质量技术文章标题。
            """, topic, keywords);

        try {
            ChatResponse response = callChatApi(TITLE_SYSTEM_PROMPT, userPrompt);
            String content = response.getResult().getOutput().getContent();
            Integer tokens = response.getMetadata().getUsage() != null ?
                response.getMetadata().getUsage().getTotalTokens().intValue() : null;

            List<String> titles = parseListResponse(content);

            log.info("为主题 '{}' 生成了 {} 个标题，token 消耗: {}", topic, titles.size(), tokens);
            return GenerateResult.builder()
                    .type("title")
                    .items(titles)
                    .model(modelName)
                    .tokens(tokens)
                    .build();

        } catch (Exception e) {
            log.error("AI 生成标题失败，主题: {}", topic, e);
            if (fallbackEnabled) {
                return fallbackGenerateTitle(request);
            }
            throw new BusinessException("标题生成失败：" + e.getMessage());
        }
    }

    @Override
    public GenerateResult generateSummary(GenerateRequestDTO request) {
        String title = request.getTitle();
        String content = request.getContent() != null ? request.getContent() : "";

        String userPrompt = String.format("""
            请为以下技术文章撰写摘要：
            文章标题：%s
            文章内容（可选）：%s
            请生成一段 100-200 字的专业技术文章摘要。
            """, title, content);

        try {
            ChatResponse response = callChatApi(SUMMARY_SYSTEM_PROMPT, userPrompt);
            String summary = response.getResult().getOutput().getContent();
            Integer tokens = response.getMetadata().getUsage() != null ?
                response.getMetadata().getUsage().getTotalTokens().intValue() : null;

            log.info("为文章 '{}' 生成了摘要，token 消耗: {}", title, tokens);
            return GenerateResult.builder()
                    .type("summary")
                    .content(summary.trim())
                    .model(modelName)
                    .tokens(tokens)
                    .build();

        } catch (Exception e) {
            log.error("AI 生成摘要失败，标题: {}", title, e);
            if (fallbackEnabled) {
                return fallbackGenerateSummary(request);
            }
            throw new BusinessException("摘要生成失败：" + e.getMessage());
        }
    }

    @Override
    public GenerateResult generateContent(GenerateRequestDTO request) {
        String title = request.getTitle();
        String outline = request.getOutline() != null ? request.getOutline() : "";

        String outlineSection = outline.isEmpty() ? "" :
            String.format("\n参考大纲（可优化）：%s\n", outline);

        String userPrompt = String.format("""
            请撰写一篇完整的技术文章，标题为：%s
            %s
            文章要求：
            - 结构：引言 → 核心概念 → 技术原理 → 实践案例 → 最佳实践 → 总结
            - 包含完整的 Java 代码示例
            - 内容不少于 1500 字
            - 使用 Markdown 格式，适配头条平台
            """, title, outlineSection);

        try {
            ChatResponse response = callChatApi(CONTENT_SYSTEM_PROMPT, userPrompt);
            String content = response.getResult().getOutput().getContent();
            Integer tokens = response.getMetadata().getUsage() != null ?
                response.getMetadata().getUsage().getTotalTokens().intValue() : null;

            log.info("为文章 '{}' 生成了正文内容，token 消耗: {}", title, tokens);
            return GenerateResult.builder()
                    .type("content")
                    .content(content)
                    .model(modelName)
                    .tokens(tokens)
                    .build();

        } catch (Exception e) {
            log.error("AI 生成正文失败，标题: {}", title, e);
            if (fallbackEnabled) {
                return fallbackGenerateContent(request);
            }
            throw new BusinessException("正文生成失败：" + e.getMessage());
        }
    }

    @Override
    public GenerateResult generateOutline(GenerateRequestDTO request) {
        String topic = request.getTopic();
        String keywords = request.getKeywords() != null ? String.join(", ", request.getKeywords()) : "";

        String userPrompt = String.format("""
            请为以下技术主题设计文章大纲：
            主题：%s
            关键词：%s
            请设计一个结构清晰、逻辑严谨的技术文章大纲。
            """, topic, keywords);

        try {
            ChatResponse response = callChatApi(OUTLINE_SYSTEM_PROMPT, userPrompt);
            String content = response.getResult().getOutput().getContent();
            Integer tokens = response.getMetadata().getUsage() != null ?
                response.getMetadata().getUsage().getTotalTokens().intValue() : null;

            log.info("为主题 '{}' 生成了大纲，token 消耗: {}", topic, tokens);
            return GenerateResult.builder()
                    .type("outline")
                    .content(content)
                    .model(modelName)
                    .tokens(tokens)
                    .build();

        } catch (Exception e) {
            log.error("AI 生成大纲失败，主题: {}", topic, e);
            if (fallbackEnabled) {
                return fallbackGenerateOutline(request);
            }
            throw new BusinessException("大纲生成失败：" + e.getMessage());
        }
    }

    @Override
    public GenerateResult generateCoverImage(GenerateRequestDTO request) {
        String title = request.getTitle();
        String style = request.getStyle() != null ? request.getStyle() : "professional";

        String prompt = String.format(
            "Create a professional cover image for a technical article titled '%s'. " +
            "Style: %s. The image should be modern, clean, and suitable for a tech blog. " +
            "Include abstract tech elements like code snippets, circuit patterns, or data visualizations. " +
            "Use a professional color scheme with blues and purples. No text in the image.",
            title, style
        );

        try {
            ImageResponse response = callImageApi(prompt);
            String imageUrl = response.getResult().getOutput().getUrl();

            log.info("为文章 '{}' 生成了封面图", title);
            return GenerateResult.builder()
                    .type("cover-image")
                    .content(imageUrl)
                    .model(modelName)
                    .build();

        } catch (Exception e) {
            log.error("AI 生成封面图失败，标题: {}", title, e);
            if (fallbackEnabled) {
                return fallbackGenerateCoverImage(request);
            }
            throw new BusinessException("封面图生成失败：" + e.getMessage());
        }
    }

    @Override
    public Flux<String> generateTitleStream(GenerateRequestDTO request) {
        String topic = request.getTopic();
        String keywords = request.getKeywords() != null ? String.join(", ", request.getKeywords()) : "";

        String userPrompt = String.format("""
            请为以下技术主题生成文章标题：
            主题：%s
            关键词：%s
            请生成 5 个不同风格的高质量技术文章标题。
            """, topic, keywords);

        try {
            return streamChatApi(TITLE_SYSTEM_PROMPT, userPrompt);
        } catch (Exception e) {
            log.error("流式生成标题失败，主题: {}", topic, e);
            if (fallbackEnabled) {
                return fallbackStream(fallbackGenerateTitle(request));
            }
            return Flux.error(new BusinessException("标题生成失败：" + e.getMessage()));
        }
    }

    @Override
    public Flux<String> generateSummaryStream(GenerateRequestDTO request) {
        String title = request.getTitle();
        String content = request.getContent() != null ? request.getContent() : "";

        String userPrompt = String.format("""
            请为以下技术文章撰写摘要：
            文章标题：%s
            文章内容（可选）：%s
            请生成一段 100-200 字的专业技术文章摘要。
            """, title, content);

        try {
            return streamChatApi(SUMMARY_SYSTEM_PROMPT, userPrompt);
        } catch (Exception e) {
            log.error("流式生成摘要失败，标题: {}", title, e);
            if (fallbackEnabled) {
                return fallbackStream(fallbackGenerateSummary(request));
            }
            return Flux.error(new BusinessException("摘要生成失败：" + e.getMessage()));
        }
    }

    @Override
    public Flux<String> generateContentStream(GenerateRequestDTO request) {
        String title = request.getTitle();
        String outline = request.getOutline() != null ? request.getOutline() : "";

        String outlineSection = outline.isEmpty() ? "" :
            String.format("\n参考大纲（可优化）：%s\n", outline);

        String userPrompt = String.format("""
            请撰写一篇完整的技术文章，标题为：%s
            %s
            文章要求：
            - 结构：引言 → 核心概念 → 技术原理 → 实践案例 → 最佳实践 → 总结
            - 包含完整的 Java 代码示例
            - 内容不少于 1500 字
            - 使用 Markdown 格式，适配头条平台
            """, title, outlineSection);

        try {
            return streamChatApi(CONTENT_SYSTEM_PROMPT, userPrompt);
        } catch (Exception e) {
            log.error("流式生成正文失败，标题: {}", title, e);
            if (fallbackEnabled) {
                return fallbackStream(fallbackGenerateContent(request));
            }
            return Flux.error(new BusinessException("正文生成失败：" + e.getMessage()));
        }
    }

    @Override
    public Flux<String> generateOutlineStream(GenerateRequestDTO request) {
        String topic = request.getTopic();
        String keywords = request.getKeywords() != null ? String.join(", ", request.getKeywords()) : "";

        String userPrompt = String.format("""
            请为以下技术主题设计文章大纲：
            主题：%s
            关键词：%s
            请设计一个结构清晰、逻辑严谨的技术文章大纲。
            """, topic, keywords);

        try {
            return streamChatApi(OUTLINE_SYSTEM_PROMPT, userPrompt);
        } catch (Exception e) {
            log.error("流式生成大纲失败，主题: {}", topic, e);
            if (fallbackEnabled) {
                return fallbackStream(fallbackGenerateOutline(request));
            }
            return Flux.error(new BusinessException("大纲生成失败：" + e.getMessage()));
        }
    }

    @Override
    public Flux<String> generateWithPromptStream(String systemPrompt, String userPrompt) {
        try {
            return streamChatApi(systemPrompt, userPrompt);
        } catch (Exception e) {
            log.error("流式生成内容失败", e);
            if (fallbackEnabled) {
                return Flux.just("[FALLBACK] 流式生成功能需要配置API Key");
            }
            return Flux.error(new BusinessException("生成失败：" + e.getMessage()));
        }
    }

    @Override
    public GenerateResult generateWithPrompt(String systemPrompt, String userPrompt, String type) {
        try {
            ChatResponse response = callChatApi(systemPrompt, userPrompt);
            String content = response.getResult().getOutput().getContent();
            Integer tokens = response.getMetadata().getUsage() != null ?
                response.getMetadata().getUsage().getTotalTokens().intValue() : null;

            log.info("使用自定义Prompt生成内容成功，type: {}, token消耗: {}", type, tokens);
            return GenerateResult.builder()
                    .type(type)
                    .content(content)
                    .model(modelName)
                    .tokens(tokens)
                    .build();

        } catch (Exception e) {
            log.error("使用自定义Prompt生成内容失败，type: {}", type, e);
            if (fallbackEnabled) {
                return GenerateResult.builder()
                        .type(type)
                        .content("降级模式：请配置API Key以获取真实AI生成内容")
                        .model("fallback")
                        .build();
            }
            throw new BusinessException("生成失败：" + e.getMessage());
        }
    }

    private ChatResponse callChatApi(String systemPrompt, String userPrompt) {
        List<Message> messages = List.of(
            new SystemMessage(systemPrompt),
            new UserMessage(userPrompt)
        );

        Prompt prompt = new Prompt(messages);
        return chatClient.call(prompt);
    }

    private Flux<String> streamChatApi(String systemPrompt, String userPrompt) {
        List<Message> messages = List.of(
            new SystemMessage(systemPrompt),
            new UserMessage(userPrompt)
        );

        Prompt prompt = new Prompt(messages);

        return streamingChatClient.stream(prompt)
            .map(response -> {
                String content = response.getResult().getOutput().getContent();
                return content != null ? content : "";
            })
            .filter(content -> !content.isEmpty());
    }

    private Flux<String> fallbackStream(GenerateResult result) {
        String content = result.getContent() != null ? result.getContent() :
            (result.getItems() != null ? String.join("\n", result.getItems()) : "");

        return Flux.create(sink -> {
            executorService.submit(() -> {
                try {
                    for (int i = 0; i < content.length(); i += 5) {
                        int end = Math.min(i + 5, content.length());
                        sink.next(content.substring(i, end));
                        Thread.sleep(20);
                    }
                    sink.complete();
                } catch (InterruptedException e) {
                    sink.complete();
                }
            });
        });
    }

    private ImageResponse callImageApi(String prompt) {
        return switch (imageProvider.toLowerCase()) {
            case "trae" -> callTraeImageApi(prompt);
            case "openai" -> callOpenAiImageApi(prompt);
            default -> {
                log.warn("未配置的图片生成 provider: {}, 默认使用 trae", imageProvider);
                yield callTraeImageApi(prompt);
            }
        };
    }

    private ImageResponse callTraeImageApi(String prompt) {
        if (!traeImageEnabled) {
            throw new BusinessException("Trae 图片生成服务未启用");
        }

        try {
            String encodedPrompt = URLEncoder.encode(prompt, StandardCharsets.UTF_8.toString());
            String imageUrl = String.format("%s/text_to_image?prompt=%s&image_size=%s",
                    traeImageBaseUrl, encodedPrompt, traeDefaultSize);

            log.info("使用 Trae API 生成图片: prompt={}", prompt.substring(0, Math.min(50, prompt.length())) + "...");

            Image image = new Image(imageUrl, prompt);
            return new ImageResponse(List.of(new org.springframework.ai.image.ImageGeneration(image)));
        } catch (Exception e) {
            log.error("Trae 图片生成失败", e);
            throw new BusinessException("Trae 图片生成失败: " + e.getMessage());
        }
    }

    private ImageResponse callOpenAiImageApi(String prompt) {
        if (!openaiImageEnabled) {
            throw new BusinessException("Spring AI 图片生成服务未启用");
        }

        try {
            log.info("使用 Spring AI 生成图片: prompt={}", prompt.substring(0, Math.min(50, prompt.length())) + "...");
            ImagePrompt imagePrompt = new ImagePrompt(prompt);
            return imageClient.call(imagePrompt);
        } catch (Exception e) {
            log.error("Spring AI 图片生成失败", e);
            throw new BusinessException("Spring AI 图片生成失败: " + e.getMessage());
        }
    }

    private List<String> parseListResponse(String content) {
        List<String> items = new ArrayList<>();
        String[] lines = content.split("\n");
        for (String line : lines) {
            String cleaned = line.trim()
                    .replaceAll("^\\d+[.、)\\s]+", "")
                    .replaceAll("^[-*•]\\s+", "")
                    .replaceAll("^[\"'`]", "")
                    .replaceAll("[\"'`]$", "");
            if (!cleaned.isEmpty() && cleaned.length() > 5) {
                items.add(cleaned);
            }
        }
        return items.isEmpty() ? List.of(content.trim()) : items;
    }

    private GenerateResult fallbackGenerateTitle(GenerateRequestDTO request) {
        String topic = request.getTopic();
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

        log.warn("降级模式：为主题 '{}' 生成了 {} 个模拟标题", topic, titles.size());
        return GenerateResult.builder()
                .type("title")
                .items(titles)
                .model("fallback")
                .build();
    }

    private GenerateResult fallbackGenerateSummary(GenerateRequestDTO request) {
        String title = request.getTitle();
        String summary = String.format(
            "本文深入探讨了%s相关技术，通过理论讲解与实践案例相结合的方式，帮助读者全面理解其核心概念和应用场景。" +
            "文章涵盖了基础原理、核心特性、实践技巧等多个方面，适合有一定基础的开发者学习参考。",
            title
        );

        log.warn("降级模式：为文章 '{}' 生成了模拟摘要", title);
        return GenerateResult.builder()
                .type("summary")
                .content(summary)
                .model("fallback")
                .build();
    }

    private GenerateResult fallbackGenerateContent(GenerateRequestDTO request) {
        String title = request.getTitle();
        String content = String.format(
            "## %s\n\n" +
            "### 一、介绍\n\n" +
            "在当今技术飞速发展的时代，%s已经成为开发者必备的技能之一。本文将带你深入了解%s的核心概念和实践应用。\n\n" +
            "### 二、核心概念\n\n" +
            "#### 2.1 基本定义\n\n" +
            "%s是一种重要的技术/概念，它在现代软件开发中扮演着关键角色。\n\n" +
            "#### 2.2 核心特性\n\n" +
            "- **特性一**: 高性能\n" +
            "- **特性二**: 易于使用\n" +
            "- **特性三**: 可扩展性强\n\n" +
            "### 三、实践案例\n\n" +
            "```java\n" +
            "// 示例代码\n" +
            "public class Example {\n" +
            "    public static void main(String[] args) {\n" +
            "        System.out.println(\"Hello, World!\");\n" +
            "    }\n" +
            "}\n" +
            "```\n\n" +
            "### 四、总结\n\n" +
            "通过本文的学习，相信你已经对%s有了全面的了解。希望这些知识能够帮助你在实际项目中更好地应用%s技术。",
            title, title, title, title, title, title
        );

        log.warn("降级模式：为文章 '{}' 生成了模拟正文", title);
        return GenerateResult.builder()
                .type("content")
                .content(content)
                .model("fallback")
                .build();
    }

    private GenerateResult fallbackGenerateOutline(GenerateRequestDTO request) {
        String topic = request.getTopic();
        String outline = String.format(
            "## 一、%s 概述\n" +
            "### 1.1 什么是 %s\n" +
            "### 1.2 为什么需要 %s\n" +
            "### 1.3 发展历程与现状\n\n" +
            "## 二、核心概念与原理\n" +
            "### 2.1 核心组件介绍\n" +
            "### 2.2 工作原理解析\n" +
            "### 2.3 关键技术点\n\n" +
            "## 三、快速入门\n" +
            "### 3.1 环境搭建\n" +
            "### 3.2 Hello World 示例\n" +
            "### 3.3 基础配置\n\n" +
            "## 四、进阶实战\n" +
            "### 4.1 典型应用场景\n" +
            "### 4.2 最佳实践\n" +
            "### 4.3 性能优化\n\n" +
            "## 五、问题与解决方案\n" +
            "### 5.1 常见问题排查\n" +
            "### 5.2 避坑指南\n\n" +
            "## 六、总结与展望\n" +
            "### 6.1 知识总结\n" +
            "### 6.2 未来发展方向",
            topic, topic, topic
        );

        log.warn("降级模式：为主题 '{}' 生成了模拟大纲", topic);
        return GenerateResult.builder()
                .type("outline")
                .content(outline)
                .model("fallback")
                .build();
    }

    private GenerateResult fallbackGenerateCoverImage(GenerateRequestDTO request) {
        String title = request.getTitle();
        String placeholderUrl = String.format(
            "https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=%s&image_size=landscape_16_9",
            java.net.URLEncoder.encode(title, java.nio.charset.StandardCharsets.UTF_8)
        );

        log.warn("降级模式：为文章 '{}' 使用占位封面图", title);
        return GenerateResult.builder()
                .type("cover-image")
                .content(placeholderUrl)
                .model("fallback")
                .build();
    }
}
