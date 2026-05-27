package com.example.agentplatform.agent.article.service;

import com.example.agentplatform.agent.article.config.ArticleFormatConfig;
import com.example.agentplatform.agent.article.config.ArticleStructure;
import com.example.agentplatform.agent.article.config.PlatformType;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MarkdownProcessor {

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();

    public String convertForPlatform(String markdown, PlatformType platform) {
        ArticleFormatConfig config = ArticleFormatConfig.forPlatform(platform);
        return processMarkdown(markdown, config);
    }

    public String convertForPlatform(ArticleStructure structure, PlatformType platform) {
        ArticleFormatConfig config = ArticleFormatConfig.forPlatform(platform);
        return renderStructure(structure, config);
    }

    private String processMarkdown(String markdown, ArticleFormatConfig config) {
        String processed = markdown;

        processed = processHeadings(processed, config);
        processed = processCodeBlocks(processed, config);
        processed = processLists(processed, config);
        processed = processParagraphs(processed, config);
        processed = processForbiddenElements(processed, config);

        if (config.isAddDividerBetweenSections()) {
            processed = addSectionDividers(processed);
        }

        return processed;
    }

    private String processHeadings(String markdown, ArticleFormatConfig config) {
        Pattern headingPattern = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);
        Matcher matcher = headingPattern.matcher(markdown);
        StringBuffer sb = new StringBuffer();

        int sectionCount = 0;
        while (matcher.find()) {
            String hashes = matcher.group(1);
            String text = matcher.group(2).trim();
            int level = hashes.length();

            String replacement = switch (config.getHeadingStyle()) {
                case STANDARD -> hashes + " " + text;
                case NUMBERED -> {
                    if (level == 2) {
                        sectionCount++;
                        yield sectionCount + ". " + text;
                    } else if (level == 3) {
                        yield "  " + (sectionCount) + ".1 " + text;
                    } else {
                        yield hashes + " " + text;
                    }
                }
                case CHINESE_NUMBERED -> {
                    if (level == 2) {
                        sectionCount++;
                        yield "## " + toChineseNumber(sectionCount) + "、" + text;
                    } else {
                        yield hashes + " " + text;
                    }
                }
                case NO_HASH -> {
                    if (level == 2) {
                        yield "\n**" + text + "**\n";
                    } else if (level == 3) {
                        yield "\n***" + text + "***\n";
                    } else {
                        yield text;
                    }
                }
            };

            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    private String processCodeBlocks(String markdown, ArticleFormatConfig config) {
        if (!config.getPlatform().isSupportCodeBlock()) {
            Pattern codeBlockPattern = Pattern.compile("```(\\w+)?\\n([\\s\\S]*?)```");
            Matcher matcher = codeBlockPattern.matcher(markdown);
            StringBuffer sb = new StringBuffer();

            while (matcher.find()) {
                String code = matcher.group(2).trim();
                String replacement = switch (config.getCodeBlockStyle()) {
                    case "image" -> "[代码示例，请参考原文]";
                    case "indented" -> indentCode(code);
                    default -> "```\n" + code + "\n```";
                };
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            matcher.appendTail(sb);
            return sb.toString();
        }

        if ("indented".equals(config.getCodeBlockStyle())) {
            Pattern codeBlockPattern = Pattern.compile("```(\\w+)?\\n([\\s\\S]*?)```");
            Matcher matcher = codeBlockPattern.matcher(markdown);
            StringBuffer sb = new StringBuffer();

            while (matcher.find()) {
                String code = matcher.group(2).trim();
                matcher.appendReplacement(sb, Matcher.quoteReplacement(indentCode(code)));
            }
            matcher.appendTail(sb);
            return sb.toString();
        }

        return markdown;
    }

    private String processLists(String markdown, ArticleFormatConfig config) {
        if ("simple".equals(config.getListStyle())) {
            markdown = Pattern.compile("^- ", Pattern.MULTILINE).matcher(markdown).replaceAll("• ");
            markdown = Pattern.compile("^\\* ", Pattern.MULTILINE).matcher(markdown).replaceAll("• ");
        }
        return markdown;
    }

    private String processParagraphs(String markdown, ArticleFormatConfig config) {
        int maxLength = config.getMaxParagraphLength();
        if (maxLength <= 0) return markdown;

        String[] paragraphs = markdown.split("\\n\\n");
        StringBuilder result = new StringBuilder();

        for (String paragraph : paragraphs) {
            if (isHeadingOrCode(paragraph)) {
                result.append(paragraph).append("\n\n");
                continue;
            }

            if (paragraph.length() > maxLength) {
                List<String> splitParagraphs = splitParagraph(paragraph, maxLength);
                for (String p : splitParagraphs) {
                    result.append(p).append("\n\n");
                }
            } else {
                result.append(paragraph).append("\n\n");
            }
        }

        return result.toString().trim();
    }

    private String processForbiddenElements(String markdown, ArticleFormatConfig config) {
        List<String> forbidden = config.getForbiddenElements();
        if (forbidden == null || forbidden.isEmpty()) return markdown;

        String result = markdown;
        for (String element : forbidden) {
            result = switch (element) {
                case "table" -> removeTables(result);
                case "code" -> removeCodeBlocks(result);
                default -> result;
            };
        }
        return result;
    }

    private String addSectionDividers(String markdown) {
        Pattern h2Pattern = Pattern.compile("^## .+$", Pattern.MULTILINE);
        Matcher matcher = h2Pattern.matcher(markdown);
        StringBuffer sb = new StringBuffer();
        boolean firstMatch = true;

        while (matcher.find()) {
            if (!firstMatch) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement("\n---\n\n" + matcher.group()));
            } else {
                firstMatch = false;
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group()));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String renderStructure(ArticleStructure structure, ArticleFormatConfig config) {
        StringBuilder sb = new StringBuilder();

        if (structure.getTitle() != null) {
            sb.append("# ").append(structure.getTitle()).append("\n\n");
        }

        if (structure.getSummary() != null) {
            sb.append("> ").append(structure.getSummary()).append("\n\n");
        }

        if (structure.getCoverImage() != null) {
            sb.append("![封面](").append(structure.getCoverImage()).append(")\n\n");
        }

        for (ArticleStructure.Section section : structure.getSections()) {
            sb.append(renderSection(section, config));
        }

        if (structure.getConclusion() != null) {
            sb.append("## 总结\n\n").append(structure.getConclusion()).append("\n\n");
        }

        if (structure.getTags() != null && !structure.getTags().isEmpty()) {
            sb.append("**标签：** ");
            sb.append(String.join(", ", structure.getTags()));
            sb.append("\n");
        }

        return sb.toString();
    }

    private String renderSection(ArticleStructure.Section section, ArticleFormatConfig config) {
        StringBuilder sb = new StringBuilder();

        String headingPrefix = "#".repeat(section.getLevel());
        sb.append(headingPrefix).append(" ").append(section.getHeading()).append("\n\n");

        if (section.getImageUrl() != null) {
            sb.append("![").append(section.getImageCaption() != null ? section.getImageCaption() : section.getHeading())
                    .append("](").append(section.getImageUrl()).append(")\n\n");
        }

        if (section.getContent() != null) {
            sb.append(section.getContent()).append("\n\n");
        }

        if (section.getKeyPoints() != null && !section.getKeyPoints().isEmpty()) {
            sb.append("**关键点：**\n");
            for (String point : section.getKeyPoints()) {
                sb.append("- ").append(point).append("\n");
            }
            sb.append("\n");
        }

        if (section.getCodeExample() != null) {
            ArticleStructure.CodeExample code = section.getCodeExample();
            if (code.getDescription() != null) {
                sb.append(code.getDescription()).append("\n\n");
            }
            sb.append("```").append(code.getLanguage()).append("\n");
            sb.append(code.getCode()).append("\n");
            sb.append("```\n\n");
        }

        if (section.getSubSections() != null) {
            for (ArticleStructure.SubSection subSection : section.getSubSections()) {
                sb.append(renderSubSection(subSection, config));
            }
        }

        return sb.toString();
    }

    private String renderSubSection(ArticleStructure.SubSection subSection, ArticleFormatConfig config) {
        StringBuilder sb = new StringBuilder();

        String headingPrefix = "#".repeat(subSection.getLevel());
        sb.append(headingPrefix).append(" ").append(subSection.getHeading()).append("\n\n");

        if (subSection.getImageUrl() != null) {
            sb.append("![").append(subSection.getHeading())
                    .append("](").append(subSection.getImageUrl()).append(")\n\n");
        }

        if (subSection.getContent() != null) {
            sb.append(subSection.getContent()).append("\n\n");
        }

        if (subSection.getCodeExample() != null) {
            ArticleStructure.CodeExample code = subSection.getCodeExample();
            if (code.getDescription() != null) {
                sb.append(code.getDescription()).append("\n\n");
            }
            sb.append("```").append(code.getLanguage()).append("\n");
            sb.append(code.getCode()).append("\n");
            sb.append("```\n\n");
        }

        return sb.toString();
    }

    private String toChineseNumber(int num) {
        String[] chineseNumbers = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十"};
        if (num <= 10) return chineseNumbers[num];
        if (num < 20) return "十" + chineseNumbers[num - 10];
        if (num < 100) {
            int tens = num / 10;
            int units = num % 10;
            return chineseNumbers[tens] + "十" + (units > 0 ? chineseNumbers[units] : "");
        }
        return String.valueOf(num);
    }

    private String indentCode(String code) {
        StringBuilder sb = new StringBuilder();
        for (String line : code.split("\n")) {
            sb.append("    ").append(line).append("\n");
        }
        return sb.toString();
    }

    private boolean isHeadingOrCode(String text) {
        return text.startsWith("#") || text.startsWith("```") || text.startsWith("    ");
    }

    private List<String> splitParagraph(String paragraph, int maxLength) {
        List<String> result = new ArrayList<>();
        String[] sentences = paragraph.split("(?<=[。！？.!?])");
        StringBuilder current = new StringBuilder();

        for (String sentence : sentences) {
            if (current.length() + sentence.length() > maxLength && current.length() > 0) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            }
            current.append(sentence);
        }

        if (current.length() > 0) {
            result.add(current.toString().trim());
        }

        return result;
    }

    private String removeTables(String markdown) {
        Pattern tablePattern = Pattern.compile("\\|.*\\|\\n\\|[-:]+\\|.*\\|\\n(\\|.*\\|\\n)*");
        return tablePattern.matcher(markdown).replaceAll("[表格内容已移除，请参考原文]");
    }

    private String removeCodeBlocks(String markdown) {
        Pattern codeBlockPattern = Pattern.compile("```[\\s\\S]*?```");
        return codeBlockPattern.matcher(markdown).replaceAll("[代码示例已移除]");
    }

    public String toHtml(String markdown) {
        Node document = parser.parse(markdown);
        return htmlRenderer.render(document);
    }

    public String extractPlainText(String markdown) {
        String text = markdown;
        text = text.replaceAll("```[\\s\\S]*?```", "[代码]");
        text = text.replaceAll("`[^`]*`", "");
        text = text.replaceAll("!\\[.*?\\]\\(.*?\\)", "[图片]");
        text = text.replaceAll("\\[([^\\]]+)\\]\\([^)]+\\)", "$1");
        text = text.replaceAll("#+\\s", "");
        text = text.replaceAll("[*_]{1,3}([^*_]+)[*_]{1,3}", "$1");
        text = Pattern.compile("^[-*]\\s", Pattern.MULTILINE).matcher(text).replaceAll("");
        text = text.replaceAll("\\n{3,}", "\n\n");
        return text.trim();
    }

    public int countWords(String markdown) {
        String plainText = extractPlainText(markdown);
        return plainText.replaceAll("\\s+", "").length();
    }

    public int countImages(String markdown) {
        Pattern imgPattern = Pattern.compile("!\\[.*?\\]\\(.*?\\)");
        Matcher matcher = imgPattern.matcher(markdown);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }
}
