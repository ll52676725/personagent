package com.example.agentplatform.agent.article.service;

import com.example.agentplatform.agent.article.config.ArticleFormatConfig;
import com.example.agentplatform.agent.article.config.PlatformType;
import com.example.agentplatform.agent.article.dto.GenerateRequestDTO;
import com.example.agentplatform.agent.article.dto.GenerateResult;
import com.example.agentplatform.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageGenerationService {

    private final GenerateService generateService;

    public String generateImageForSection(String sectionTitle, String articleTitle, String style) {
        String prompt = buildImagePrompt(sectionTitle, articleTitle, style);

        try {
            GenerateRequestDTO request = GenerateRequestDTO.builder()
                    .title(sectionTitle)
                    .style(style)
                    .build();

            GenerateResult result = generateService.generateCoverImage(request);
            return result.getContent();
        } catch (Exception e) {
            log.warn("AI生成图片失败，使用占位图: {}", e.getMessage());
            return generatePlaceholderImage(sectionTitle, articleTitle);
        }
    }

    public List<String> generateImagesForArticle(String markdownContent, String articleTitle, PlatformType platform) {
        ArticleFormatConfig config = ArticleFormatConfig.forPlatform(platform);
        int imageCount = Math.max(config.getPlatform().getMinImageCount(), 3);

        List<String> sections = extractSectionTitles(markdownContent);
        List<String> imageUrls = new ArrayList<>();

        for (int i = 0; i < Math.min(sections.size(), imageCount); i++) {
            String imageUrl = generateImageForSection(sections.get(i), articleTitle, "professional");
            imageUrls.add(imageUrl);
        }

        return imageUrls;
    }

    public String insertImagesIntoMarkdown(String markdown, String articleTitle, PlatformType platform) {
        ArticleFormatConfig config = ArticleFormatConfig.forPlatform(platform);
        int targetImageCount = config.getPlatform().getMinImageCount();

        List<String> sections = extractSectionTitles(markdown);
        if (sections.isEmpty()) {
            return markdown;
        }

        int imagesToGenerate = Math.min(targetImageCount, sections.size());
        if (imagesToGenerate == 0) {
            return markdown;
        }

        List<String> imageUrls = new ArrayList<>();
        for (int i = 0; i < imagesToGenerate; i++) {
            String imageUrl = generateImageForSection(sections.get(i), articleTitle, "professional");
            imageUrls.add(imageUrl);
        }

        return insertImagesAtPositions(markdown, sections, imageUrls, config.getImagePlacement());
    }

    private String insertImagesAtPositions(String markdown, List<String> sections, List<String> imageUrls,
                                           ArticleFormatConfig.ImagePlacement placement) {
        StringBuilder result = new StringBuilder(markdown);
        int imageIndex = 0;

        for (int i = 0; i < sections.size() && imageIndex < imageUrls.size(); i++) {
            String section = sections.get(i);
            String imageUrl = imageUrls.get(imageIndex);

            Pattern sectionPattern = Pattern.compile("^##\\s+" + Pattern.quote(section) + "\\s*$", Pattern.MULTILINE);
            Matcher matcher = sectionPattern.matcher(result);

            if (matcher.find()) {
                String imageMarkdown = buildImageMarkdown(section, imageUrl, placement);
                int insertPosition = matcher.end();

                if (placement == ArticleFormatConfig.ImagePlacement.AFTER_HEADING) {
                    result.insert(insertPosition, "\n\n" + imageMarkdown + "\n");
                } else if (placement == ArticleFormatConfig.ImagePlacement.AFTER_PARAGRAPH) {
                    int paragraphEnd = findNextParagraphEnd(result, insertPosition);
                    if (paragraphEnd > insertPosition) {
                        result.insert(paragraphEnd, "\n\n" + imageMarkdown + "\n");
                    } else {
                        result.insert(insertPosition, "\n\n" + imageMarkdown + "\n");
                    }
                }

                imageIndex++;
            }
        }

        return result.toString();
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

    private String buildImageMarkdown(String caption, String imageUrl, ArticleFormatConfig.ImagePlacement placement) {
        return String.format("![%s](%s)", caption, imageUrl);
    }

    private String buildImagePrompt(String sectionTitle, String articleTitle, String style) {
        return String.format(
            "Create a professional technical illustration for an article section titled '%s' from the article '%s'. " +
            "Style: %s. The image should be modern, clean, and suitable for a tech blog. " +
            "Include relevant abstract tech elements like code snippets, circuit patterns, data visualizations, or architectural diagrams. " +
            "Use a professional color scheme with blues, purples, and teals. No text in the image. " +
            "The image should have a 16:9 aspect ratio and be visually appealing.",
            sectionTitle, articleTitle, style
        );
    }

    private String generatePlaceholderImage(String sectionTitle, String articleTitle) {
        String combinedTitle = articleTitle + " - " + sectionTitle;
        try {
            String encodedPrompt = URLEncoder.encode(combinedTitle, StandardCharsets.UTF_8.toString());
            return String.format("https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=%s&image_size=landscape_16_9",
                    encodedPrompt);
        } catch (Exception e) {
            return "https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=technology&image_size=landscape_16_9";
        }
    }

    private List<String> extractSectionTitles(String markdown) {
        List<String> sections = new ArrayList<>();
        Pattern h2Pattern = Pattern.compile("^##\\s+(.+)$", Pattern.MULTILINE);
        Matcher matcher = h2Pattern.matcher(markdown);

        while (matcher.find()) {
            String title = matcher.group(1).trim();
            if (!title.equals("引言") && !title.equals("总结") && !title.equals("结语")) {
                sections.add(title);
            }
        }

        return sections;
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
            return generatePlaceholderImage("封面", articleTitle);
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
}
