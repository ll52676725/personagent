package com.example.agentplatform.agent.article.service;

import com.example.agentplatform.agent.article.config.PlatformType;
import com.example.agentplatform.agent.article.dto.GenerateRequestDTO;
import com.example.agentplatform.agent.article.dto.GenerateResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleGenerationService {

    private final GenerateService generateService;
    private final MarkdownProcessor markdownProcessor;
    private final ImageGenerationService imageGenerationService;

    private static final String MULTI_PLATFORM_CONTENT_PROMPT = """
        你是一个资深技术作家，擅长撰写高质量的技术教程文章，能够适配多个技术平台发布。
        
        重要：文章通用格式要求：
        1. 标题层级清晰：
           - 一级标题：## 标题
           - 二级标题：### 标题
           - 不要使用 # 作为一级标题
        2. 段落分明：每段 3-5 句话，空行分隔
        3. 重点突出：使用加粗标记核心概念
        4. 代码示例：使用 ```java 代码块，代码要完整可运行，包含详细注释
        5. 列表使用：使用 - 或 1. 2. 3. 格式
        6. 每节之间有空行过渡
        7. 文章结构：
           - 引言（150字左右，吸引读者，说明背景和价值）
           - 核心概念讲解（分小节，每个概念配简单说明）
           - 技术原理分析（深入讲解实现机制）
           - 完整的 Java 代码示例（带注释，可直接运行）
           - 实际应用场景（说明在什么情况下使用）
           - 最佳实践和避坑指南（至少5条）
           - 总结（100字左右，提炼核心要点）
        8. 文章总字数不少于 2000 字
        9. 语言风格：专业但易懂，适合中级开发者
        10. 每个主要章节（除引言和总结外）都应该有配图占位说明
        
        图片占位符格式：
        在每个主要章节标题下方添加图片占位符，格式为：
        [IMAGE: 章节标题 - 描述性文字]
        
        例如：
        ## 核心概念
        
        [IMAGE: 核心概念 - 技术架构示意图]
        
        这将帮助后续自动生成匹配的插图。
        
        只返回文章内容，不要有任何其他说明。
        """;

    private static final String CSDN_CONTENT_PROMPT = """
        你是一个资深技术作家，擅长撰写高质量的CSDN技术博客文章。
        
        CSDN平台特点：
        - 读者以开发者为主，喜欢干货和实战
        - SEO友好，需要包含足够的关键词
        - 支持完整的Markdown语法
        - 适合深度技术文章
        
        文章要求：
        1. 标题包含核心关键词，利于SEO
        2. 开头有摘要和目录导航
        3. 内容深入，代码示例完整
        4. 每节配合适的示意图或流程图
        5. 结尾有总结和扩展阅读链接
        6. 字数不少于1500字
        7. 添加适当的标签
        
        格式要求：
        - 标准Markdown标题：## 标题
        - 代码块带语言标记：```java
        - 使用表格对比技术方案
        - 加粗重点内容
        
        只返回文章内容，不要有任何其他说明。
        """;

    private static final String TOUTIAO_CONTENT_PROMPT = """
        你是一个资深技术作家，擅长撰写今日头条风格的技术文章。
        
        今日头条平台特点：
        - 算法推荐，标题要吸引人
        - 内容要通俗易懂，适合大众
        - 图文并茂，多配图片
        - 段落短小，阅读轻松
        - 字数要求2000字以上
        
        文章要求：
        1. 标题要有吸引力，带数字或悬念
        2. 开头要有痛点引入，引起共鸣
        3. 每段不超过3行，多分段
        4. 每1-2段配一张相关图片
        5. 使用emoji增加趣味性，但不要过多
        6. 多用口语化表达，避免太技术化
        7. 结尾要有互动引导，如"你怎么看？"
        8. 避免使用复杂的Markdown语法
        
        格式要求：
        - 不使用#号标题，用加粗文字作为标题
        - 代码使用缩进格式，不要用```
        - 不使用表格
        - 使用简单的列表符号
        
        图片占位符：[IMAGE: 描述]
        
        只返回文章内容，不要有任何其他说明。
        """;

    private static final String ZHIHU_CONTENT_PROMPT = """
        你是一个资深技术作家，擅长撰写知乎风格的技术回答/文章。
        
        知乎平台特点：
        - 读者追求深度和专业度
        - 喜欢有数据、有案例的干货
        - 支持完整的Markdown和公式
        - 适合系统性的知识分享
        
        文章要求：
        1. 开头点明核心观点
        2. 逻辑严谨，论证充分
        3. 内容有深度，不流于表面
        4. 引用权威资料或数据
        5. 图文并茂，配合示意图
        6. 字数不少于2500字
        7. 结尾给出明确的结论和建议
        
        格式要求：
        - 标准Markdown标题
        - 支持复杂的代码块和表格
        - 使用引用块标注重要信息
        - 适当使用脚注和参考文献
        
        图片占位符：[IMAGE: 描述]
        
        只返回文章内容，不要有任何其他说明。
        """;

    public GenerateResult generateContentForPlatform(GenerateRequestDTO request, PlatformType platform) {
        String title = request.getTitle();
        String outline = request.getOutline() != null ? request.getOutline() : "";

        String systemPrompt = getPlatformPrompt(platform);
        String outlineSection = outline.isEmpty() ? "" :
            String.format("\n参考大纲（可优化）：%s\n", outline);

        String userPrompt = String.format("""
            请撰写一篇完整的技术文章，标题为：%s
            %s
            文章要求：
            - 结构：引言 → 核心概念 → 技术原理 → 实践案例 → 最佳实践 → 总结
            - 包含完整的 Java 代码示例
            - 内容不少于 %d 字
            - 为每个主要章节添加图片占位符 [IMAGE: 章节标题 - 描述]
            """, title, outlineSection, platform.getMinWordCount());

        GenerateResult result = generateService.generateWithPrompt(systemPrompt, userPrompt, "content");

        String contentWithImages = imageGenerationService.insertImagesIntoMarkdown(
            result.getContent(), title, platform
        );

        return GenerateResult.builder()
                .type("content")
                .content(contentWithImages)
                .model(result.getModel())
                .tokens(result.getTokens())
                .build();
    }

    public Flux<String> generateContentStreamForPlatform(GenerateRequestDTO request, PlatformType platform) {
        String title = request.getTitle();
        String outline = request.getOutline() != null ? request.getOutline() : "";

        String systemPrompt = getPlatformPrompt(platform);
        String outlineSection = outline.isEmpty() ? "" :
            String.format("\n参考大纲（可优化）：%s\n", outline);

        String userPrompt = String.format("""
            请撰写一篇完整的技术文章，标题为：%s
            %s
            文章要求：
            - 结构：引言 → 核心概念 → 技术原理 → 实践案例 → 最佳实践 → 总结
            - 包含完整的 Java 代码示例
            - 内容不少于 %d 字
            - 为每个主要章节添加图片占位符 [IMAGE: 章节标题 - 描述]
            """, title, outlineSection, platform.getMinWordCount());

        return generateService.generateWithPromptStream(systemPrompt, userPrompt);
    }

    public GenerateResult generateFullArticle(GenerateRequestDTO request, List<PlatformType> platforms) {
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
            - 内容不少于 2000 字
            - 为每个主要章节添加图片占位符 [IMAGE: 章节标题 - 描述]
            """, title, outlineSection);

        GenerateResult result = generateService.generateWithPrompt(
            MULTI_PLATFORM_CONTENT_PROMPT, userPrompt, "content"
        );

        String contentWithImages = imageGenerationService.insertImagesIntoMarkdown(
            result.getContent(), title, PlatformType.CSDN
        );

        return GenerateResult.builder()
                .type("content")
                .content(contentWithImages)
                .model(result.getModel())
                .tokens(result.getTokens())
                .build();
    }

    public String getPlatformContent(String content, String title, PlatformType platform) {
        String contentWithImages = imageGenerationService.insertImagesIntoMarkdown(content, title, platform);
        return markdownProcessor.convertForPlatform(contentWithImages, platform);
    }

    private String getPlatformPrompt(PlatformType platform) {
        return switch (platform) {
            case CSDN -> CSDN_CONTENT_PROMPT;
            case TOUTIAO -> TOUTIAO_CONTENT_PROMPT;
            case ZHIHU -> ZHIHU_CONTENT_PROMPT;
            case JUEJIN -> CSDN_CONTENT_PROMPT;
            case WECHAT -> TOUTIAO_CONTENT_PROMPT;
            case BILIBILI -> CSDN_CONTENT_PROMPT;
        };
    }

    public String generateTitleForPlatform(String topic, List<String> keywords, PlatformType platform) {
        String systemPrompt = switch (platform) {
            case CSDN -> """
                你是CSDN标题专家，擅长生成SEO友好的技术文章标题。
                要求：
                1. 包含核心技术关键词
                2. 体现实用性和干货感
                3. 长度15-30字
                4. 风格：教程类、深度解析类
                只返回一个最佳标题。
                """;
            case TOUTIAO -> """
                你是今日头条标题专家，擅长生成高点击率的标题。
                要求：
                1. 有数字或悬念
                2. 引发好奇心或共鸣
                3. 长度20-30字
                4. 风格：干货、揭秘、实战
                只返回一个最佳标题。
                """;
            case ZHIHU -> """
                你是知乎标题专家，擅长生成专业严谨的标题。
                要求：
                1. 点明核心问题或观点
                2. 体现深度和专业性
                3. 长度15-25字
                4. 风格：深度、系统、干货
                只返回一个最佳标题。
                """;
            default -> """
                你是技术文章标题专家。
                要求：
                1. 准确反映技术内容
                2. 包含核心技术关键词
                3. 长度15-25字
                只返回一个最佳标题。
                """;
        };

        String keywordsStr = keywords != null ? String.join(", ", keywords) : "";
        String userPrompt = String.format("""
            主题：%s
            关键词：%s
            请生成一个最适合该平台的技术文章标题。
            """, topic, keywordsStr);

        GenerateResult result = generateService.generateWithPrompt(systemPrompt, userPrompt, "title");
        return result.getContent().trim();
    }

    public String generateSummaryForPlatform(String title, String content, PlatformType platform) {
        String systemPrompt = switch (platform) {
            case CSDN -> """
                你是CSDN摘要专家。
                要求：
                1. 概括核心技术内容
                2. 包含SEO关键词
                3. 长度100-150字
                4. 突出技术价值
                """;
            case TOUTIAO -> """
                你是今日头条摘要专家。
                要求：
                1. 制造悬念或痛点
                2. 引发阅读兴趣
                3. 长度80-120字
                4. 口语化表达
                """;
            case ZHIHU -> """
                你是知乎摘要专家。
                要求：
                1. 点明核心观点
                2. 说明文章价值
                3. 长度120-180字
                4. 专业严谨
                """;
            default -> """
                你是技术文章摘要专家。
                要求：
                1. 准确概括文章核心内容
                2. 突出技术价值
                3. 长度100-150字
                """;
        };

        String userPrompt = String.format("""
            文章标题：%s
            文章内容：%s
            请生成适合该平台的摘要。
            """, title, content != null ? content.substring(0, Math.min(1000, content.length())) : "");

        GenerateResult result = generateService.generateWithPrompt(systemPrompt, userPrompt, "summary");
        return result.getContent().trim();
    }
}
