package com.example.agentplatform.agent.article.config;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ArticleStructure {
    private String title;
    private String summary;
    private String coverImage;
    private List<Section> sections;
    private String conclusion;
    private List<String> tags;
    private List<String> references;

    @Data
    @Builder
    public static class Section {
        private String heading;
        private int level;
        private String content;
        private List<SubSection> subSections;
        private String imageUrl;
        private String imageCaption;
        private CodeExample codeExample;
        private List<String> keyPoints;
    }

    @Data
    @Builder
    public static class SubSection {
        private String heading;
        private int level;
        private String content;
        private String imageUrl;
        private CodeExample codeExample;
    }

    @Data
    @Builder
    public static class CodeExample {
        private String language;
        private String code;
        private String description;
        private List<String> highlights;
    }

    public static ArticleStructure createTemplate(PlatformType platform, String title) {
        return ArticleStructure.builder()
                .title(title)
                .sections(List.of(
                        Section.builder()
                                .heading("引言")
                                .level(2)
                                .content("")
                                .keyPoints(List.of("背景介绍", "问题陈述", "本文目标"))
                                .build(),
                        Section.builder()
                                .heading("核心概念")
                                .level(2)
                                .content("")
                                .subSections(List.of(
                                        SubSection.builder()
                                                .heading("基本定义")
                                                .level(3)
                                                .content("")
                                                .build(),
                                        SubSection.builder()
                                                .heading("核心特性")
                                                .level(3)
                                                .content("")
                                                .build()
                                ))
                                .build(),
                        Section.builder()
                                .heading("技术原理")
                                .level(2)
                                .content("")
                                .build(),
                        Section.builder()
                                .heading("实践案例")
                                .level(2)
                                .content("")
                                .subSections(List.of(
                                        SubSection.builder()
                                                .heading("环境准备")
                                                .level(3)
                                                .content("")
                                                .build(),
                                        SubSection.builder()
                                                .heading("完整实现")
                                                .level(3)
                                                .content("")
                                                .build()
                                ))
                                .build(),
                        Section.builder()
                                .heading("最佳实践")
                                .level(2)
                                .content("")
                                .build(),
                        Section.builder()
                                .heading("总结")
                                .level(2)
                                .content("")
                                .build()
                ))
                .build();
    }
}
