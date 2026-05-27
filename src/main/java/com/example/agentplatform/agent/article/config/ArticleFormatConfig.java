package com.example.agentplatform.agent.article.config;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ArticleFormatConfig {
    private PlatformType platform;
    private HeadingStyle headingStyle;
    private boolean useBoldForEmphasis;
    private boolean addImageCaptions;
    private String codeBlockStyle;
    private String listStyle;
    private boolean addDividerBetweenSections;
    private int maxParagraphLength;
    private List<String> forbiddenElements;
    private ImagePlacement imagePlacement;

    public enum HeadingStyle {
        STANDARD,
        NUMBERED,
        CHINESE_NUMBERED,
        NO_HASH
    }

    public enum ImagePlacement {
        AFTER_HEADING,
        AFTER_PARAGRAPH,
        RANDOM,
        END_OF_SECTION
    }

    public static ArticleFormatConfig forPlatform(PlatformType platform) {
        return switch (platform) {
            case CSDN -> ArticleFormatConfig.builder()
                    .platform(platform)
                    .headingStyle(HeadingStyle.STANDARD)
                    .useBoldForEmphasis(true)
                    .addImageCaptions(true)
                    .codeBlockStyle("fenced")
                    .listStyle("standard")
                    .addDividerBetweenSections(false)
                    .maxParagraphLength(150)
                    .forbiddenElements(List.of())
                    .imagePlacement(ImagePlacement.AFTER_HEADING)
                    .build();

            case TOUTIAO -> ArticleFormatConfig.builder()
                    .platform(platform)
                    .headingStyle(HeadingStyle.NO_HASH)
                    .useBoldForEmphasis(true)
                    .addImageCaptions(false)
                    .codeBlockStyle("indented")
                    .listStyle("simple")
                    .addDividerBetweenSections(true)
                    .maxParagraphLength(100)
                    .forbiddenElements(List.of("table"))
                    .imagePlacement(ImagePlacement.AFTER_PARAGRAPH)
                    .build();

            case ZHIHU -> ArticleFormatConfig.builder()
                    .platform(platform)
                    .headingStyle(HeadingStyle.STANDARD)
                    .useBoldForEmphasis(true)
                    .addImageCaptions(true)
                    .codeBlockStyle("fenced")
                    .listStyle("standard")
                    .addDividerBetweenSections(false)
                    .maxParagraphLength(200)
                    .forbiddenElements(List.of())
                    .imagePlacement(ImagePlacement.END_OF_SECTION)
                    .build();

            case JUEJIN -> ArticleFormatConfig.builder()
                    .platform(platform)
                    .headingStyle(HeadingStyle.STANDARD)
                    .useBoldForEmphasis(true)
                    .addImageCaptions(true)
                    .codeBlockStyle("fenced")
                    .listStyle("standard")
                    .addDividerBetweenSections(false)
                    .maxParagraphLength(150)
                    .forbiddenElements(List.of())
                    .imagePlacement(ImagePlacement.AFTER_HEADING)
                    .build();

            case WECHAT -> ArticleFormatConfig.builder()
                    .platform(platform)
                    .headingStyle(HeadingStyle.NUMBERED)
                    .useBoldForEmphasis(true)
                    .addImageCaptions(true)
                    .codeBlockStyle("image")
                    .listStyle("standard")
                    .addDividerBetweenSections(true)
                    .maxParagraphLength(100)
                    .forbiddenElements(List.of("code"))
                    .imagePlacement(ImagePlacement.RANDOM)
                    .build();

            case BILIBILI -> ArticleFormatConfig.builder()
                    .platform(platform)
                    .headingStyle(HeadingStyle.STANDARD)
                    .useBoldForEmphasis(true)
                    .addImageCaptions(false)
                    .codeBlockStyle("fenced")
                    .listStyle("standard")
                    .addDividerBetweenSections(false)
                    .maxParagraphLength(150)
                    .forbiddenElements(List.of())
                    .imagePlacement(ImagePlacement.AFTER_PARAGRAPH)
                    .build();
        };
    }
}
