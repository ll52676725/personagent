package com.example.agentplatform.agent.knowledge.service;

import com.example.agentplatform.agent.knowledge.dto.MinerUParseResult;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;

/**
 * 文档解析服务接口
 *
 * 功能概述：
 * 该接口定义了文档解析的核心能力，支持从多种格式的文档中提取文本内容，
 * 为知识库提供丰富的文档导入能力。
 *
 * 支持的文档格式：
 * - 文档类：PDF, DOC, DOCX, RTF, TXT
 * - 表格类：XLS, XLSX, CSV
 * - 演示类：PPT, PPTX
 * - 标记语言：Markdown, HTML, XML, JSON
 * - 代码文件：Java, Python, JavaScript, TypeScript 等
 * - 图片类：PNG, JPG, JPEG 等（需OCR支持）
 *
 * 应用场景：
 * - 用户上传文档文件，自动提取内容存入知识库
 * - 批量文档导入，快速构建企业知识库
 * - 网页内容抓取后的文本清洗
 * - 精细化文档解析（MinerU）：提取表格、图片、公式等结构化数据
 */
public interface DocumentParserService {

    /**
     * 解析上传的文件并提取文本内容
     *
     * 处理流程：
     * 1. 验证文件非空
     * 2. 根据文件名后缀判断文件类型
     * 3. 调用相应的解析器提取文本
     * 4. 对提取的文本进行清洗和规范化
     *
     * @param file 上传的文件对象
     * @return 提取的纯文本内容，解析失败返回空字符串
     */
    String parseFile(MultipartFile file);

    /**
     * 从输入流解析文档内容
     *
     * 适用场景：
     * - 从网络流直接解析（如爬虫获取的文档）
     * - 从云存储（OSS/S3）读取后直接解析
     * - 批量文件处理时复用输入流
     *
     * @param inputStream 文档输入流
     * @param fileName    文件名（用于判断文件类型，选择合适的解析器）
     * @return 提取的纯文本内容，解析失败返回空字符串
     */
    String parseStream(InputStream inputStream, String fileName);

    /**
     * 清洗和规范化文本内容
     *
     * 清洗规则：
     * 1. 移除 HTML 标签
     * 2. 移除特殊字符和不可见字符
     * 3. 规范化空白字符（多个空格/制表符替换为单个空格）
     * 4. 移除过多的空行（连续3个以上换行替换为2个）
     * 5. 去除首尾空白
     *
     * 清洗目的：
     * - 提高向量化质量
     * - 减少无用字符的 Token 消耗
     * - 提升检索效果
     *
     * @param text 原始文本
     * @return 清洗后的文本
     */
    String cleanText(String text);

    /**
     * 判断文件是否支持解析
     *
     * 通过文件名后缀判断是否在支持的格式列表中。
     *
     * @param fileName 文件名
     * @return true 表示支持解析，false 表示不支持
     */
    boolean isSupported(String fileName);

    /**
     * 使用 MinerU 精细化解析文件，提取结构化数据
     *
     * 功能特性：
     * - 精准提取文档中的表格（支持HTML和Markdown格式）
     * - 识别并提取文档中的截图和图片
     * - 解析数学公式为LaTeX格式
     * - 保留文档层级结构，输出高质量Markdown
     * - 支持PDF、Word、Excel、PPT、图片等多种格式
     *
     * @param file 上传的文件对象
     * @return MinerUParseResult 包含文本、Markdown、JSON以及图片、表格、公式等结构化数据，
     *         解析失败或MinerU未启用时返回null
     */
    MinerUParseResult parseFileStructured(MultipartFile file);

    /**
     * 使用 MinerU 精细化解析URL，提取结构化数据
     *
     * @param url 文档URL地址
     * @return MinerUParseResult 结构化解析结果，解析失败或MinerU未启用时返回null
     */
    MinerUParseResult parseUrlStructured(String url);

    /**
     * 检查 MinerU 是否可用
     *
     * @return true 表示MinerU已启用且配置正确，false 表示不可用
     */
    boolean isMinerUAvailable();
}
