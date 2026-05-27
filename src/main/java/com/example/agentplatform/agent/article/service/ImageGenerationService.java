package com.example.agentplatform.agent.article.service;

import com.example.agentplatform.agent.article.config.ArticleFormatConfig;
import com.example.agentplatform.agent.article.config.PlatformType;
import com.example.agentplatform.agent.article.dto.GenerateRequestDTO;
import com.example.agentplatform.agent.article.dto.GenerateResult;
import com.example.agentplatform.agent.article.dto.SectionImageResult;
import com.example.agentplatform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageGenerationService {

    private final GenerateService generateService;

    @Value("${agent.platform.image.parallel-threshold:3}")
    private int parallelThreshold;

    @Value("${agent.platform.image.cache-enabled:true}")
    private boolean cacheEnabled;

    private final Map<String, String> imageCache = new ConcurrentHashMap<>();
    private final ExecutorService imageExecutor = Executors.newFixedThreadPool(5);

    private static final Set<String> SKIP_SECTIONS = Set.of("引言", "总结", "结语", "前言", "后记", "致谢");

    public List<SectionImageResult> generateSectionImages(String markdownContent, String articleTitle, PlatformType platform) {
        ArticleFormatConfig config = ArticleFormatConfig.forPlatform(platform);
        List<SectionInfo> sections = analyzeArticleStructure(markdownContent);

        if (sections.isEmpty()) {
            return Collections.emptyList();
        }

        int targetImageCount = Math.min(config.getPlatform().getMinImageCount(), sections.size());
        List<SectionInfo> selectedSections = selectSectionsForImages(sections, targetImageCount);

        List<CompletableFuture<SectionImageResult>> futures = selectedSections.stream()
                .map(section -> CompletableFuture.supplyAsync(() ->
                        generateImageForSection(section, articleTitle, platform), imageExecutor))
                .toList();

        List<SectionImageResult> results = new ArrayList<>();
        for (CompletableFuture<SectionImageResult> future : futures) {
            try {
                results.add(future.get(30, TimeUnit.SECONDS));
            } catch (Exception e) {
                log.warn("图片生成超时或失败: {}", e.getMessage());
            }
        }

        return results;
    }

    public String insertImagesIntoMarkdown(String markdown, String articleTitle, PlatformType platform) {
        List<SectionImageResult> images = generateSectionImages(markdown, articleTitle, platform);
        if (images.isEmpty()) {
            return markdown;
        }
        return insertImagesAtSmartPositions(markdown, images, ArticleFormatConfig.forPlatform(platform));
    }

    public SectionImageResult generateImageForSection(SectionInfo section, String articleTitle, PlatformType platform) {
        String cacheKey = cacheEnabled ? buildCacheKey(section.getTitle(), articleTitle, platform.name()) : null;

        if (cacheKey != null && imageCache.containsKey(cacheKey)) {
            log.debug("从缓存获取图片: section={}", section.getTitle());
            return SectionImageResult.builder()
                    .sectionTitle(section.getTitle())
                    .imageUrl(imageCache.get(cacheKey))
                    .insertPosition(section.getInsertPosition())
                    .caption(buildCaption(section, articleTitle))
                    .build();
        }

        String prompt = buildSmartImagePrompt(section, articleTitle, platform);

        try {
            String imageUrl = generateViaLLM(prompt, section.getTitle(), articleTitle);
            if (cacheKey != null) {
                imageCache.put(cacheKey, imageUrl);
            }
            return SectionImageResult.builder()
                    .sectionTitle(section.getTitle())
                    .imageUrl(imageUrl)
                    .insertPosition(section.getInsertPosition())
                    .caption(buildCaption(section, articleTitle))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.warn("AI生成图片失败，使用占位图: section={}, error={}", section.getTitle(), e.getMessage());
            String placeholderUrl = generateSmartPlaceholder(section, articleTitle);
            return SectionImageResult.builder()
                    .sectionTitle(section.getTitle())
                    .imageUrl(placeholderUrl)
                    .insertPosition(section.getInsertPosition())
                    .caption(buildCaption(section, articleTitle))
                    .success(false)
                    .build();
        }
    }

    private String generateViaLLM(String prompt, String sectionTitle, String articleTitle) {
        try {
            GenerateRequestDTO request = GenerateRequestDTO.builder()
                    .title(sectionTitle)
                    .style("professional")
                    .build();

            GenerateResult result = generateService.generateCoverImage(request);
            return result.getContent();
        } catch (Exception e) {
            throw new BusinessException("LLM图片生成失败: " + e.getMessage());
        }
    }

    private List<SectionInfo> analyzeArticleStructure(String markdownContent) {
        List<SectionInfo> sections = new ArrayList<>();

        Pattern sectionPattern = Pattern.compile("^(#{2,3})\\s+(.+)$", Pattern.MULTILINE);
        Matcher matcher = sectionPattern.matcher(markdownContent);

        int lastPosition = 0;
        String lastTitle = null;
        int lastLevel = 0;

        while (matcher.find()) {
            if (lastTitle != null) {
                int contentLength = matcher.start() - lastPosition;
                String contentSnippet = markdownContent.substring(lastPosition, matcher.start()).trim();

                if (!SKIP_SECTIONS.contains(lastTitle)) {
                    SectionInfo sectionInfo = SectionInfo.builder()
                            .title(lastTitle)
                            .level(lastLevel)
                            .contentLength(contentLength)
                            .startPosition(lastPosition)
                            .endPosition(matcher.start())
                            .codeBlocks(containsCodeBlocks(contentSnippet))
                            .lists(containsLists(contentSnippet))
                            .keywordDensity(calculateKeywordDensity(contentSnippet))
                            .insertPosition(determineInsertPosition(contentSnippet))
                            .build();
                    sections.add(sectionInfo);
                }
            }

            lastTitle = matcher.group(2).trim();
            lastLevel = matcher.group(1).length();
            lastPosition = matcher.end();
        }

        if (lastTitle != null && !SKIP_SECTIONS.contains(lastTitle)) {
            int contentLength = markdownContent.length() - lastPosition;
            String contentSnippet = markdownContent.substring(lastPosition).trim();

            SectionInfo sectionInfo = SectionInfo.builder()
                    .title(lastTitle)
                    .level(lastLevel)
                    .contentLength(contentLength)
                    .startPosition(lastPosition)
                    .endPosition(markdownContent.length())
                    .codeBlocks(containsCodeBlocks(contentSnippet))
                    .lists(containsLists(contentSnippet))
                    .keywordDensity(calculateKeywordDensity(contentSnippet))
                    .insertPosition(determineInsertPosition(contentSnippet))
                    .build();
            sections.add(sectionInfo);
        }

        return sections;
    }

    private List<SectionInfo> selectSectionsForImages(List<SectionInfo> sections, int count) {
        if (sections.size() <= count) {
            return sections;
        }

        sections.sort((a, b) -> {
            int scoreA = computeSectionScore(a);
            int scoreB = computeSectionScore(b);
            return Integer.compare(scoreB, scoreA);
        });

        return sections.stream()
                .limit(count)
                .sorted(Comparator.comparingInt(SectionInfo::getStartPosition))
                .toList();
    }

    private int computeSectionScore(SectionInfo section) {
        int score = 0;
        score += Math.min(section.getContentLength() / 100, 10);
        if (section.isCodeBlocks()) score += 5;
        if (section.isLists()) score += 2;
        score += (int)(section.getKeywordDensity() * 10);
        if (section.getLevel() == 2) score += 3;
        return score;
    }

    private String determineInsertPosition(String content) {
        if (content.length() < 50) {
            return "AFTER_HEADING";
        }

        int firstParagraphEnd = findFirstParagraphEnd(content);
        if (firstParagraphEnd > 0 && firstParagraphEnd < 200) {
            return "AFTER_FIRST_PARAGRAPH";
        }

        return "AFTER_HEADING";
    }

    private int findFirstParagraphEnd(String content) {
        int pos = 0;
        while (pos < content.length()) {
            if (pos + 1 < content.length() && content.charAt(pos) == '\n' && content.charAt(pos + 1) == '\n') {
                return pos + 2;
            }
            pos++;
        }
        return content.length();
    }

    private boolean containsCodeBlocks(String content) {
        return content.contains("```");
    }

    private boolean containsLists(String content) {
        Pattern listPattern = Pattern.compile("^[-*]\\s|^\\d+[.、)]", Pattern.MULTILINE);
        return listPattern.matcher(content).find();
    }

    private float calculateKeywordDensity(String content) {
        if (content.isEmpty()) return 0;
        String[] words = content.split("[\\s,，。.！？!?,;:，、]");
        long techWords = Arrays.stream(words)
                .filter(w -> w.length() > 2)
                .filter(this::isTechKeyword)
                .count();
        return (float) techWords / words.length;
    }

    private boolean isTechKeyword(String word) {
        Set<String> techKeywords = Set.of(
                "技术", "架构", "系统", "模块", "接口", "实现", "配置",
                "代码", "算法", "数据", "性能", "优化", "框架", "组件",
                "部署", "测试", "开发", "函数", "方法", "类", "对象",
                "网络", "协议", "缓存", "数据库", "安全", "认证",
                "Java", "Spring", "Redis", "MySQL", "API", "HTTP"
        );
        return techKeywords.stream().anyMatch(word::contains);
    }

    private String buildSmartImagePrompt(SectionInfo section, String articleTitle, PlatformType platform) {
        String style = getConsistentStyle(platform);
        StringBuilder prompt = new StringBuilder();

        prompt.append(String.format(
                "Create a professional technical illustration for an article section about '%s'. ",
                section.getTitle()
        ));

        prompt.append(String.format("Article title: '%s'. ", articleTitle));
        prompt.append(String.format("Style: %s. ", style));

        prompt.append("The image should be modern, clean, and suitable for a tech blog. ");
        prompt.append("Include relevant abstract tech elements like code snippets, circuit patterns, data visualizations, or architectural diagrams. ");
        prompt.append("Use a professional color scheme with blues, purples, and teals. ");
        prompt.append("No text in the image. ");
        prompt.append("The image should have a 16:9 aspect ratio and be visually appealing. ");

        if (section.isCodeBlocks()) {
            prompt.append("Emphasize coding-related elements like syntax highlighting, IDE interfaces, or code flow diagrams. ");
        }
        if (section.isLists()) {
            prompt.append("Include structured elements like flowcharts, diagrams, or hierarchical visualizations. ");
        }

        return prompt.toString();
    }

    private String getConsistentStyle(PlatformType platform) {
        return switch (platform) {
            case CSDN -> "professional technical illustration with code elements";
            case TOUTIAO -> "vibrant modern illustration with engaging colors";
            case ZHIHU -> "sophisticated academic style with clean lines and data visualizations";
            case JUEJIN -> "developer-focused style with dark mode aesthetics";
            case WECHAT -> "warm friendly illustration with soft colors";
            case BILIBILI -> "tech anime fusion with modern UI elements";
        };
    }

    private String buildCaption(SectionInfo section, String articleTitle) {
        return String.format("%s - %s 示意图", articleTitle, section.getTitle());
    }

    private String insertImagesAtSmartPositions(String markdown, List<SectionImageResult> images, ArticleFormatConfig config) {
        StringBuilder result = new StringBuilder(markdown);
        int offset = 0;

        Map<String, SectionImageResult> imageMap = new HashMap<>();
        for (SectionImageResult img : images) {
            imageMap.put(img.getSectionTitle(), img);
        }

        Pattern sectionPattern = Pattern.compile("^(#{2,3})\\s+(.+)$", Pattern.MULTILINE);
        Matcher matcher = sectionPattern.matcher(result);

        List<InsertionPoint> insertionPoints = new ArrayList<>();

        while (matcher.find()) {
            String title = matcher.group(2).trim();
            if (imageMap.containsKey(title)) {
                SectionImageResult image = imageMap.get(title);
                int insertPos = calculateInsertPosition(result, matcher.end(), image.getInsertPosition());
                insertionPoints.add(new InsertionPoint(insertPos, image));
            }
        }

        insertionPoints.sort(Comparator.comparingInt(InsertionPoint::position).reversed());

        for (InsertionPoint point : insertionPoints) {
            String imageMarkdown = buildImageMarkdown(point.image());
            result.insert(point.position() + offset, "\n\n" + imageMarkdown + "\n");
            offset += imageMarkdown.length() + 3;
        }

        return result.toString();
    }

    private int calculateInsertPosition(StringBuilder content, int headingEnd, String positionType) {
        return switch (positionType) {
            case "AFTER_FIRST_PARAGRAPH" -> {
                int paraEnd = findNextParagraphEnd(content, headingEnd);
                yield paraEnd > headingEnd ? paraEnd : headingEnd;
            }
            default -> headingEnd;
        };
    }

    private int findNextParagraphEnd(StringBuilder content, int startPos) {
        int pos = startPos;
        while (pos < content.length()) {
            if (content.charAt(pos) == '\n' && pos + 1 < content.length() && content.charAt(pos + 1) == '\n') {
                return pos + 2;
            }
            pos++;
        }
        return startPos;
    }

    private String buildImageMarkdown(SectionImageResult image) {
        return String.format("![%s](%s)", image.getCaption(), image.getImageUrl());
    }

    private String generateSmartPlaceholder(SectionInfo section, String articleTitle) {
        String combinedTitle = articleTitle + " - " + section.getTitle();
        try {
            String encodedPrompt = URLEncoder.encode(combinedTitle, StandardCharsets.UTF_8.toString());
            return String.format("https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=%s&image_size=landscape_16_9",
                    encodedPrompt);
        } catch (Exception e) {
            return "https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=technology&image_size=landscape_16_9";
        }
    }

    private String buildCacheKey(String sectionTitle, String articleTitle, String platform) {
        return (articleTitle + ":" + sectionTitle + ":" + platform).hashCode() + "";
    }

    public void clearCache() {
        imageCache.clear();
        log.info("图片缓存已清空");
    }

    public int getCacheSize() {
        return imageCache.size();
    }

    public String generateCoverImage(String articleTitle, String style) {
        try {
            GenerateRequestDTO request = GenerateRequestDTO.builder()
                    .title(articleTitle)
                    .style(style)
                    .build();

            GenerateResult result = generateService.generateCoverImage(request);
            return result.getContent();
        } catch (Exception e) {
            log.warn("AI生成封面图失败，使用占位图: {}", e.getMessage());
            return generateSmartPlaceholder(
                    SectionInfo.builder().title("封面").build(),
                    articleTitle
            );
        }
    }

    public void validateArticleImages(String markdown, PlatformType platform) {
        int requiredImages = platform.getMinImageCount();
        int actualImages = countImagesInMarkdown(markdown);

        if (actualImages < requiredImages) {
            throw new BusinessException(String.format(
                "发布到%s需要至少%d张图片，当前文章只有%d张图片",
                platform.getName(), requiredImages, actualImages
            ));
        }
    }

    private int countImagesInMarkdown(String markdown) {
        Pattern imgPattern = Pattern.compile("!\\[.*?\\]\\(.*?\\)");
        Matcher matcher = imgPattern.matcher(markdown);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    public List<String> extractImageUrls(String markdown) {
        List<String> urls = new ArrayList<>();
        Pattern imgPattern = Pattern.compile("!\\[.*?\\]\\((.*?)\\)");
        Matcher matcher = imgPattern.matcher(markdown);
        while (matcher.find()) {
            urls.add(matcher.group(1));
        }
        return urls;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SectionInfo {
        private String title;
        private int level;
        private int contentLength;
        private int startPosition;
        private int endPosition;
        private boolean codeBlocks;
        private boolean lists;
        private float keywordDensity;
        private String insertPosition;
    }

    private record InsertionPoint(int position, SectionImageResult image) {}
}
