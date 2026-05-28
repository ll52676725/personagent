package com.example.agentplatform.agent.knowledge.service.impl;

import com.example.agentplatform.agent.knowledge.service.DocumentParserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.ContentHandler;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 文档解析服务实现类
 *
 * 技术选型：
 * 使用 Apache Tika 作为核心解析引擎，这是 Apache 基金会的开源文档解析库。
 * Tika 能够自动检测文档类型并调用相应的解析器，支持上千种文件格式。
 *
 * Apache Tika 核心组件：
 * 1. AutoDetectParser - 自动检测文档类型并选择合适的解析器
 * 2. Tika - 简化的门面类，提供便捷的解析方法
 * 3. BodyContentHandler - 提取文档正文内容的 SAX 处理器
 * 4. Metadata - 存储文档元数据（作者、创建时间、页数等）
 *
 * 支持格式（部分）：
 * - 文档：PDF, DOC, DOCX, ODT, RTF, TXT
 * - 表格：XLS, XLSX, ODS, CSV
 * - 演示：PPT, PPTX, ODP
 * - 标记语言：HTML, XML, Markdown, JSON
 * - 代码文件：Java, Python, JavaScript, TypeScript, Go, Rust 等
 * - 图片（OCR）：JPG, PNG 等（需安装 Tesseract OCR）
 * - 邮件：EML, MSG
 * - 压缩包：ZIP, RAR, TAR.GZ（递归解析内部文件）
 *
 * 依赖说明：
 * 需要在 pom.xml 中添加 Apache Tika 依赖：
 * <pre>
 * {@code
 * <dependency>
 *     <groupId>org.apache.tika</groupId>
 *     <artifactId>tika-core</artifactId>
 *     <version>2.9.1</version>
 * </dependency>
 * <dependency>
 *     <groupId>org.apache.tika</groupId>
 *     <artifactId>tika-parsers-standard-package</artifactId>
 *     <version>2.9.1</version>
 * </dependency>
 * }
 * </pre>
 */
@Slf4j
@Service
public class DocumentParserServiceImpl implements DocumentParserService {

    private final Tika tika;
    private final AutoDetectParser parser;

    private static final int MAX_STRING_LENGTH = 10 * 1024 * 1024;

    private static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList(
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".txt", ".md", ".markdown", ".html", ".htm", ".rtf",
            ".csv", ".json", ".xml", ".java", ".py", ".js", ".ts"
    );

    /**
     * 构造函数：初始化 Tika 解析器
     *
     * Tika 对象是线程安全的，可以在整个应用中复用。
     * AutoDetectParser 会自动加载所有可用的解析器。
     */
    public DocumentParserServiceImpl() {
        this.tika = new Tika();
        this.parser = new AutoDetectParser();
    }

    /**
     * 解析上传的文件并提取文本内容
     *
     * 处理流程：
     * 1. 校验文件非空
     * 2. 获取文件输入流
     * 3. 调用 parseStream 进行实际解析
     * 4. 异常捕获和日志记录
     *
     * @param file 上传的文件对象
     * @return 提取的纯文本内容，解析失败返回空字符串
     */
    @Override
    public String parseFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("文件为空，无法解析");
            return "";
        }
        try {
            return parseStream(file.getInputStream(), file.getOriginalFilename());
        } catch (Exception e) {
            log.error("解析文件失败: {}", file.getOriginalFilename(), e);
            return "";
        }
    }

    /**
     * 从输入流解析文档内容
     *
     * Tika 解析核心流程：
     * 1. 自动检测 MIME 类型（通过文件扩展名或魔数）
     * 2. 根据 MIME 类型选择对应的解析器
     * 3. 解析器读取输入流，提取文本内容和元数据
     * 4. ContentHandler 接收 SAX 事件，构建文本字符串
     *
     * 关键组件说明：
     * - Metadata：存储文档元数据，如作者、标题、创建日期等
     * - BodyContentHandler：只提取文档正文，忽略格式和样式
     * - ParseContext：提供解析上下文配置
     *
     * @param inputStream 文档输入流
     * @param fileName    文件名（用于辅助判断类型）
     * @return 提取并清洗后的纯文本内容
     */
    @Override
    public String parseStream(InputStream inputStream, String fileName) {
        try {
            Metadata metadata = new Metadata();
            if (fileName != null) {
                metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, fileName);
            }

            ContentHandler handler = new BodyContentHandler(MAX_STRING_LENGTH);
            ParseContext context = new ParseContext();

            parser.parse(inputStream, handler, metadata, context);

            String rawText = handler.toString();
            return cleanText(rawText);

        } catch (Exception e) {
            log.error("解析文档失败: {}", fileName, e);
            return "";
        }
    }

    /**
     * 清洗和规范化文本内容
     *
     * 清洗流水线：
     * 1. removeHtmlTags() - 移除 HTML 标签
     * 2. removeSpecialCharacters() - 移除特殊字符
     * 3. normalizeWhitespace() - 规范化空白字符
     * 4. removeExcessiveNewlines() - 移除过多空行
     *
     * 设计原则：
     * - 保留中英文文本、常用标点、数字
     * - 移除控制字符、不可见字符
     * - 保持可读性，不破坏段落结构
     *
     * @param text 原始文本
     * @return 清洗后的文本
     */
    @Override
    public String cleanText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String cleaned = text;

        cleaned = removeHtmlTags(cleaned);
        cleaned = removeSpecialCharacters(cleaned);
        cleaned = normalizeWhitespace(cleaned);
        cleaned = removeExcessiveNewlines(cleaned);

        return cleaned.trim();
    }

    /**
     * 判断文件是否支持解析
     *
     * 通过文件名后缀进行快速判断。
     * 如需更精确的检测，可使用 Tika.detect() 进行 MIME 类型检测。
     *
     * @param fileName 文件名
     * @return true 表示支持，false 表示不支持
     */
    @Override
    public boolean isSupported(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lowerName = fileName.toLowerCase();
        return SUPPORTED_EXTENSIONS.stream()
                .anyMatch(lowerName::endsWith);
    }

    /**
     * 移除 HTML 标签
     *
     * 使用 Jsoup 库进行 HTML 解析，比正则表达式更可靠。
     * Jsoup 会处理嵌套标签、自闭合标签等复杂情况。
     *
     * 降级策略：如果 Jsoup 解析失败，使用正则表达式作为备选方案。
     *
     * @param text 可能包含 HTML 标签的文本
     * @return 纯文本内容
     */
    private String removeHtmlTags(String text) {
        try {
            return Jsoup.parse(text).text();
        } catch (Exception e) {
            return text.replaceAll("<[^>]+>", " ");
        }
    }

    /**
     * 移除特殊字符
     *
     * 保留的字符集：
     * - 中英文大小写字母（Character.isLetterOrDigit）
     * - 空白字符（空格、换行、制表符等）
     * - 常用英文标点：.,!?;:-_()[]{}'"
     * - 常用中文标点：。，！？：；—（）【】
     *
     * 移除的字符：
     * - 控制字符（\u0000-\u001F, \u007F-\u009F）
     * - 特殊符号：★☆◆◇■□●○▲△▼▽等
     * - 表情符号（Emoji）
     * - 其他不可打印字符
     *
     * @param text 原始文本
     * @return 清理后的文本
     */
    private String removeSpecialCharacters(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (Character.isLetterOrDigit(c) || Character.isWhitespace(c)
                    || c == '.' || c == ',' || c == '!' || c == '?'
                    || c == ':' || c == ';' || c == '-' || c == '_'
                    || c == '(' || c == ')' || c == '[' || c == ']'
                    || c == '{' || c == '}' || c == '"' || c == '\''
                    || c == '。' || c == '，' || c == '！' || c == '？'
                    || c == '：' || c == '；' || c == '—' || c == '（'
                    || c == '）' || c == '【' || c == '】') {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 规范化空白字符
     *
     * 将连续的空格或制表符替换为单个空格。
     * 注意：不替换换行符，以保持段落结构。
     *
     * 正则说明：
     * - [ \\t]+ 匹配一个或多个空格或水平制表符
     *
     * @param text 原始文本
     * @return 规范化后的文本
     */
    private String normalizeWhitespace(String text) {
        return text.replaceAll("[ \\t]+", " ");
    }

    /**
     * 移除过多的空行
     *
     * 将连续3个或更多的换行符替换为2个换行符，
     * 既保持段落分隔，又避免过多空白。
     *
     * 正则说明：
     * - \\n{3,} 匹配3个或更多连续的换行符
     *
     * @param text 原始文本
     * @return 清理后的文本
     */
    private String removeExcessiveNewlines(String text) {
        return text.replaceAll("\\n{3,}", "\n\n");
    }
}
