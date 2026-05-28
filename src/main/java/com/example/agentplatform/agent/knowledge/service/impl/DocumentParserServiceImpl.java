package com.example.agentplatform.agent.knowledge.service.impl;

import com.example.agentplatform.agent.knowledge.config.MinerUProperties;
import com.example.agentplatform.agent.knowledge.dto.MinerUParseResult;
import com.example.agentplatform.agent.knowledge.service.DocumentParserService;
import com.example.agentplatform.agent.knowledge.service.MinerUApiClient;
import lombok.RequiredArgsConstructor;
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

import jakarta.annotation.PostConstruct;

/**
 * 文档解析服务实现类
 *
 * 技术选型：
 * - 默认使用 Apache Tika 作为基础解析引擎，支持上千种文件格式
 * - 启用 MinerU 时，优先使用 MinerU 进行精细化解析，提取表格、图片、公式等结构化数据
 * - MinerU 解析失败或未启用时，自动降级到 Apache Tika 解析
 *
 * Apache Tika 核心组件：
 * 1. AutoDetectParser - 自动检测文档类型并选择合适的解析器
 * 2. Tika - 简化的门面类，提供便捷的解析方法
 * 3. BodyContentHandler - 提取文档正文内容的 SAX 处理器
 * 4. Metadata - 存储文档元数据（作者、创建时间、页数等）
 *
 * MinerU 核心能力：
 * 1. 精准识别文档布局和层级结构
 * 2. 提取表格为HTML/Markdown格式
 * 3. 识别并提取文档中的图片和截图
 * 4. 解析数学公式为LaTeX格式
 * 5. 输出高质量的Markdown内容
 *
 * 支持格式（部分）：
 * - 文档：PDF, DOC, DOCX, ODT, RTF, TXT
 * - 表格：XLS, XLSX, ODS, CSV
 * - 演示：PPT, PPTX, ODP
 * - 图片（MinerU OCR）：PNG, JPG, JPEG, WEBP, GIF, BMP
 * - 标记语言：HTML, XML, Markdown, JSON
 * - 代码文件：Java, Python, JavaScript, TypeScript, Go, Rust 等
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
    private final MinerUApiClient minerUApiClient;
    private final MinerUProperties minerUProperties;

    private static final int MAX_STRING_LENGTH = 10 * 1024 * 1024;

    private static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList(
            ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".txt", ".md", ".markdown", ".html", ".htm", ".rtf",
            ".csv", ".json", ".xml", ".java", ".py", ".js", ".ts",
            ".png", ".jpg", ".jpeg", ".gif", ".webp", ".bmp"
    );

    /**
     * 构造函数：初始化 Tika 解析器和 MinerU 客户端
     *
     * Tika 对象是线程安全的，可以在整个应用中复用。
     * AutoDetectParser 会自动加载所有可用的解析器。
     *
     * @param minerUApiClient MinerU API客户端
     * @param minerUProperties MinerU配置属性
     */
    public DocumentParserServiceImpl(MinerUApiClient minerUApiClient, MinerUProperties minerUProperties) {
        this.tika = new Tika();
        this.parser = new AutoDetectParser();
        this.minerUApiClient = minerUApiClient;
        this.minerUProperties = minerUProperties;
    }

    @PostConstruct
    public void init() {
        log.info("文档解析服务初始化完成，MinerU状态: {}",
                isMinerUAvailable() ? "已启用" : "未启用");
        if (minerUProperties.isEnabled()) {
            log.info("MinerU配置: apiBaseUrl={}, modelVersion={}, extractImages={}, extractTables={}, extractFormulas={}",
                    minerUProperties.getApiBaseUrl(),
                    minerUProperties.getModelVersion(),
                    minerUProperties.isExtractImages(),
                    minerUProperties.isExtractTables(),
                    minerUProperties.isExtractFormulas());
        }
    }

    /**
     * 解析上传的文件并提取文本内容
     *
     * 智能降级策略：
     * 1. 优先尝试使用MinerU进行精细化解析
     * 2. 如果MinerU可用且解析成功，使用MinerU的文本结果
     * 3. 如果MinerU不可用或解析失败，回退到Apache Tika解析
     *
     * @param file 上传的文件对象
     * @return 提取的纯文本内容，解析失败返回空字符串
     */
    @Override
    public String parseFile(MultipartFile file) {
        if (isMinerUAvailable()) {
            MinerUParseResult structuredResult = parseFileStructured(file);
            if (structuredResult != null) {
                String text = structuredResult.getTextContent();
                if (text != null && !text.isBlank()) {
                    return cleanText(text);
                }
            }
        }
        return parseFileWithTika(file);
    }

    /**
     * 使用Apache Tika解析文件（降级方案）
     */
    private String parseFileWithTika(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("文件为空，无法解析");
            return "";
        }
        try {
            return parseStream(file.getInputStream(), file.getOriginalFilename());
        } catch (Throwable t) {
            log.error("解析文件失败: {}", file.getOriginalFilename(), t);
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

        } catch (Throwable t) {
            log.error("解析文档失败: {}", fileName, t);
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

    /**
     * 使用 MinerU 精细化解析文件，提取结构化数据
     *
     * 处理流程：
     * 1. 检查MinerU是否启用
     * 2. 调用MinerU API上传文件并创建解析任务
     * 3. 轮询任务状态直到完成
     * 4. 下载解析结果（Markdown + JSON）
     * 5. 提取结构化数据（图片、表格、公式等）
     *
     * @param file 上传的文件对象
     * @return MinerUParseResult 包含结构化解析结果，失败或未启用时返回null
     */
    @Override
    public MinerUParseResult parseFileStructured(MultipartFile file) {
        if (!isMinerUAvailable()) {
            log.warn("MinerU is not available, structured parsing skipped for file: {}", file.getOriginalFilename());
            return null;
        }

        try {
            log.info("开始使用 MinerU 精细化解析文件: {}, 大小: {} bytes",
                    file.getOriginalFilename(), file.getSize());

            MinerUParseResult result = minerUApiClient.parseFile(file);

            if (result != null) {
                log.info("MinerU 解析成功: 文件={}, 文本长度={}, 图片数={}, 表格数={}, 公式数={}",
                        file.getOriginalFilename(),
                        result.getTextContent() != null ? result.getTextContent().length() : 0,
                        result.getImages() != null ? result.getImages().size() : 0,
                        result.getTables() != null ? result.getTables().size() : 0,
                        result.getFormulas() != null ? result.getFormulas().size() : 0);
            } else {
                log.warn("MinerU 解析返回空结果，文件: {}", file.getOriginalFilename());
            }

            return result;

        } catch (Exception e) {
            log.error("MinerU 结构化解析失败: {}", file.getOriginalFilename(), e);
            return null;
        }
    }

    /**
     * 使用 MinerU 精细化解析URL，提取结构化数据
     *
     * @param url 文档URL地址
     * @return MinerUParseResult 结构化解析结果，失败或未启用时返回null
     */
    @Override
    public MinerUParseResult parseUrlStructured(String url) {
        if (!isMinerUAvailable()) {
            log.warn("MinerU is not available, structured parsing skipped for url: {}", url);
            return null;
        }

        try {
            log.info("开始使用 MinerU 精细化解析URL: {}", url);

            MinerUParseResult result = minerUApiClient.parseUrl(url);

            if (result != null) {
                log.info("MinerU URL解析成功: URL={}, 文本长度={}, 图片数={}, 表格数={}, 公式数={}",
                        url,
                        result.getTextContent() != null ? result.getTextContent().length() : 0,
                        result.getImages() != null ? result.getImages().size() : 0,
                        result.getTables() != null ? result.getTables().size() : 0,
                        result.getFormulas() != null ? result.getFormulas().size() : 0);
            }

            return result;

        } catch (Exception e) {
            log.error("MinerU URL结构化解析失败: {}", url, e);
            return null;
        }
    }

    /**
     * 检查 MinerU 是否可用
     *
     * 检查条件：
     * 1. MinerU 功能已启用
     * 2. API Token 不是默认占位符（Agent轻量API无需Token时可留空）
     *
     * @return true 表示可用，false 表示不可用
     */
    @Override
    public boolean isMinerUAvailable() {
        if (!minerUProperties.isEnabled()) {
            return false;
        }
        String token = minerUProperties.getApiToken();
        return token == null || token.isBlank() || !token.equalsIgnoreCase("your-mineru-api-token");
    }
}
