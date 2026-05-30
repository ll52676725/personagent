package com.example.agentplatform.tools.service.impl;

import com.example.agentplatform.common.exception.BusinessException;
import com.example.agentplatform.tools.common.FileUtils;
import com.example.agentplatform.tools.dto.FileConvertRequestDTO;
import com.example.agentplatform.tools.dto.FileConvertResultDTO;
import com.example.agentplatform.tools.dto.FileFormatInfoDTO;
import com.example.agentplatform.tools.service.FileConvertService;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.converter.PicturesManager;
import org.apache.poi.hwpf.converter.WordToHtmlConverter;
import org.apache.poi.hwpf.usermodel.PictureType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 文件格式转换服务实现类
 * 实现了多种文件格式之间的转换功能
 * 支持的格式包括：Word(doc/docx)、PDF、文本(txt)、HTML、Excel(xls/xlsx)、CSV等
 */
@Slf4j
@Service
public class FileConvertServiceImpl implements FileConvertService {

    /**
     * 文件格式与MIME类型的映射关系
     */
    private static final Map<String, String> FORMAT_MIME_MAP = new LinkedHashMap<>();

    /**
     * 文件格式描述信息映射
     */
    private static final Map<String, String> FORMAT_DESCRIPTION_MAP = new LinkedHashMap<>();

    /**
     * 支持读取的文件格式集合
     */
    private static final Set<String> READABLE_FORMATS = new HashSet<>();

    /**
     * 支持写入的文件格式集合
     */
    private static final Set<String> WRITABLE_FORMATS = new HashSet<>();

    static {
        // 初始化格式与MIME类型的映射
        FORMAT_MIME_MAP.put("pdf", "application/pdf");
        FORMAT_MIME_MAP.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        FORMAT_MIME_MAP.put("doc", "application/msword");
        FORMAT_MIME_MAP.put("txt", "text/plain");
        FORMAT_MIME_MAP.put("html", "text/html");
        FORMAT_MIME_MAP.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        FORMAT_MIME_MAP.put("xls", "application/vnd.ms-excel");
        FORMAT_MIME_MAP.put("csv", "text/csv");

        // 初始化格式描述信息
        FORMAT_DESCRIPTION_MAP.put("pdf", "PDF - 便携式文档格式");
        FORMAT_DESCRIPTION_MAP.put("docx", "DOCX - Microsoft Word 2007+ 文档");
        FORMAT_DESCRIPTION_MAP.put("doc", "DOC - Microsoft Word 97-2003 文档");
        FORMAT_DESCRIPTION_MAP.put("txt", "TXT - 纯文本文件");
        FORMAT_DESCRIPTION_MAP.put("html", "HTML - 超文本标记语言");
        FORMAT_DESCRIPTION_MAP.put("xlsx", "XLSX - Microsoft Excel 2007+ 电子表格");
        FORMAT_DESCRIPTION_MAP.put("xls", "XLS - Microsoft Excel 97-2003 电子表格");
        FORMAT_DESCRIPTION_MAP.put("csv", "CSV - 逗号分隔值文件");

        // 初始化支持读取的格式
        READABLE_FORMATS.add("pdf");
        READABLE_FORMATS.add("docx");
        READABLE_FORMATS.add("doc");
        READABLE_FORMATS.add("txt");
        READABLE_FORMATS.add("xlsx");
        READABLE_FORMATS.add("xls");
        READABLE_FORMATS.add("csv");

        // 初始化支持写入的格式
        WRITABLE_FORMATS.add("pdf");
        WRITABLE_FORMATS.add("txt");
        WRITABLE_FORMATS.add("html");
        WRITABLE_FORMATS.add("csv");
    }

    /**
     * 转换文件格式的核心方法
     *
     * @param file    上传的文件
     * @param request 转换请求参数
     * @return 转换结果DTO
     */
    @Override
    public FileConvertResultDTO convert(MultipartFile file, FileConvertRequestDTO request) {
        // 获取并规范化目标格式
        String targetFormat = request.getTargetFormat().toLowerCase().trim();
        validateTargetFormat(targetFormat);

        // 检测源文件格式
        String sourceFormat = FileUtils.detectFileExtension(file.getOriginalFilename());
        String originalFileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";

        // 执行文件转换
        byte[] convertedBytes = convertFile(file, sourceFormat, targetFormat);

        // 构建转换后的文件名
        String convertedFileName = FileUtils.buildConvertedFileName(originalFileName, targetFormat);

        // 构建Base64编码数据
        String base64Data = FileUtils.toBase64Data(convertedBytes, FORMAT_MIME_MAP.get(targetFormat));

        // 记录转换日志
        log.info("文件格式转换完成: {} -> {}, 原始大小: {} bytes, 转换后大小: {} bytes",
                sourceFormat, targetFormat, file.getSize(), convertedBytes.length);

        // 构建并返回转换结果
        return FileConvertResultDTO.builder()
                .originalFormat(sourceFormat)
                .targetFormat(targetFormat)
                .originalFileName(originalFileName)
                .convertedFileName(convertedFileName)
                .originalSize(file.getSize())
                .convertedSize((long) convertedBytes.length)
                .mimeType(FORMAT_MIME_MAP.get(targetFormat))
                .fileDataBase64(base64Data)
                .rawFileData(convertedBytes)
                .build();
    }

    /**
     * 获取支持的文件格式列表
     *
     * @return 格式信息列表
     */
    @Override
    public List<FileFormatInfoDTO> getSupportedFormats() {
        List<FileFormatInfoDTO> formatList = new ArrayList<>();

        for (Map.Entry<String, String> entry : FORMAT_MIME_MAP.entrySet()) {
            String format = entry.getKey();
            List<String> extensions = new ArrayList<>();
            extensions.add(format);

            // 处理格式别名
            if (format.equals("docx")) {
                extensions.add("doc");
            } else if (format.equals("xlsx")) {
                extensions.add("xls");
            }

            // 构建格式信息DTO
            formatList.add(FileFormatInfoDTO.builder()
                    .formatName(format)
                    .extensions(extensions)
                    .mimeType(entry.getValue())
                    .readable(READABLE_FORMATS.contains(format))
                    .writable(WRITABLE_FORMATS.contains(format))
                    .description(FORMAT_DESCRIPTION_MAP.getOrDefault(format, format.toUpperCase()))
                    .build());
        }

        return formatList;
    }

    /**
     * 验证目标格式是否支持
     *
     * @param targetFormat 目标格式
     */
    private void validateTargetFormat(String targetFormat) {
        if (!WRITABLE_FORMATS.contains(targetFormat)) {
            throw new BusinessException("不支持的目标格式: " + targetFormat);
        }
    }

    /**
     * 文件转换的核心调度方法
     * 根据源格式和目标格式选择对应的转换方法
     *
     * @param file         源文件
     * @param sourceFormat 源格式
     * @param targetFormat 目标格式
     * @return 转换后的字节数组
     */
    private byte[] convertFile(MultipartFile file, String sourceFormat, String targetFormat) {
        try {
            // Word文档转PDF
            if ((sourceFormat.equals("docx") || sourceFormat.equals("doc")) && targetFormat.equals("pdf")) {
                return wordToPdf(file, sourceFormat);
            }
            // Word文档转文本
            else if ((sourceFormat.equals("docx") || sourceFormat.equals("doc")) && targetFormat.equals("txt")) {
                return wordToText(file, sourceFormat);
            }
            // Word文档转HTML
            else if ((sourceFormat.equals("docx") || sourceFormat.equals("doc")) && targetFormat.equals("html")) {
                return wordToHtml(file, sourceFormat);
            }
            // PDF转文本
            else if (sourceFormat.equals("pdf") && targetFormat.equals("txt")) {
                return pdfToText(file);
            }
            // 文本转PDF
            else if (sourceFormat.equals("txt") && targetFormat.equals("pdf")) {
                return textToPdf(file);
            }
            // Excel转CSV
            else if ((sourceFormat.equals("xlsx") || sourceFormat.equals("xls")) && targetFormat.equals("csv")) {
                return excelToCsv(file, sourceFormat);
            }
            // 同格式直接返回（用于测试和格式验证）
            else if (sourceFormat.equals(targetFormat)) {
                return file.getBytes();
            }
            // 不支持的转换组合
            else {
                throw new BusinessException("不支持从 " + sourceFormat + " 转换到 " + targetFormat);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件转换失败", e);
            throw new BusinessException("文件转换失败: " + e.getMessage());
        }
    }

    /**
     * Word文档转PDF
     * 支持docx和doc格式
     *
     * @param file         Word文件
     * @param sourceFormat 源格式（docx或doc）
     * @return PDF字节数组
     */
    private byte[] wordToPdf(MultipartFile file, String sourceFormat) {
        try {
            // 先提取文本内容
            String text;
            if (sourceFormat.equals("docx")) {
                text = extractTextFromDocx(file);
            } else {
                text = extractTextFromDoc(file);
            }
            // 使用iText将文本转换为PDF
            return textToPdf(text);
        } catch (Exception e) {
            log.error("Word转PDF失败", e);
            throw new BusinessException("Word转PDF失败: " + e.getMessage());
        }
    }

    /**
     * Word文档转纯文本
     *
     * @param file         Word文件
     * @param sourceFormat 源格式
     * @return 文本字节数组
     */
    private byte[] wordToText(MultipartFile file, String sourceFormat) {
        try {
            String text;
            if (sourceFormat.equals("docx")) {
                text = extractTextFromDocx(file);
            } else {
                text = extractTextFromDoc(file);
            }
            return text.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Word转文本失败", e);
            throw new BusinessException("Word转文本失败: " + e.getMessage());
        }
    }

    /**
     * Word文档转HTML
     *
     * @param file         Word文件
     * @param sourceFormat 源格式
     * @return HTML字节数组
     */
    private byte[] wordToHtml(MultipartFile file, String sourceFormat) {
        try {
            if (sourceFormat.equals("docx")) {
                return docxToHtml(file);
            } else {
                return docToHtml(file);
            }
        } catch (Exception e) {
            log.error("Word转HTML失败", e);
            throw new BusinessException("Word转HTML失败: " + e.getMessage());
        }
    }

    /**
     * 从docx文件提取文本内容
     *
     * @param file docx文件
     * @return 文本内容
     */
    private String extractTextFromDocx(MultipartFile file) throws Exception {
        try (InputStream is = file.getInputStream();
             XWPFDocument document = new XWPFDocument(is)) {
            StringBuilder text = new StringBuilder();
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph paragraph : paragraphs) {
                text.append(paragraph.getText()).append("\n");
            }
            return text.toString();
        }
    }

    /**
     * 从doc文件提取文本内容
     *
     * @param file doc文件
     * @return 文本内容
     */
    private String extractTextFromDoc(MultipartFile file) throws Exception {
        try (InputStream is = file.getInputStream();
             HWPFDocument document = new HWPFDocument(is)) {
            return document.getDocumentText();
        }
    }

    /**
     * docx转HTML
     *
     * @param file docx文件
     * @return HTML字节数组
     */
    private byte[] docxToHtml(MultipartFile file) throws Exception {
        try (InputStream is = file.getInputStream();
             XWPFDocument document = new XWPFDocument(is)) {
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"></head><body>");

            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph paragraph : paragraphs) {
                html.append("<p>").append(paragraph.getText()).append("</p>");
            }

            html.append("</body></html>");
            return html.toString().getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * doc转HTML
     *
     * @param file doc文件
     * @return HTML字节数组
     */
    private byte[] docToHtml(MultipartFile file) throws Exception {
        try (InputStream is = file.getInputStream();
             HWPFDocument document = new HWPFDocument(is);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            org.w3c.dom.Document htmlDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            WordToHtmlConverter converter = new WordToHtmlConverter(htmlDocument);

            converter.setPicturesManager(new PicturesManager() {
                @Override
                public String savePicture(byte[] content, PictureType pictureType,
                                          String suggestedName, float widthInches, float heightInches) {
                    return suggestedName;
                }
            });

            converter.processDocument(document);
            org.w3c.dom.Document doc = converter.getDocument();

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "html");
            transformer.transform(new DOMSource(doc), new StreamResult(out));

            return out.toByteArray();
        }
    }

    /**
     * PDF转文本
     * 使用Apache PDFBox提取PDF文本内容
     *
     * @param file PDF文件
     * @return 文本字节数组
     */
    private byte[] pdfToText(MultipartFile file) {
        try (InputStream is = file.getInputStream();
             PDDocument document = PDDocument.load(is)) {

            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            return text.getBytes(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("PDF转文本失败", e);
            throw new BusinessException("PDF转文本失败: " + e.getMessage());
        }
    }

    /**
     * 文本文件转PDF
     *
     * @param file 文本文件
     * @return PDF字节数组
     */
    private byte[] textToPdf(MultipartFile file) throws Exception {
        String text = new String(file.getBytes(), StandardCharsets.UTF_8);
        return textToPdf(text);
    }

    /**
     * 文本字符串转PDF
     * 使用iText创建PDF文档
     *
     * @param text 文本内容
     * @return PDF字节数组
     */
    private byte[] textToPdf(String text) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            com.itextpdf.text.Document document = new com.itextpdf.text.Document();
            PdfWriter.getInstance(document, out);
            document.open();

            // 设置中文字体支持
            BaseFont bfChinese = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            Font fontChinese = new Font(bfChinese, 12, Font.NORMAL);

            // 按行添加文本
            String[] lines = text.split("\n");
            for (String line : lines) {
                if (line.trim().isEmpty()) {
                    document.add(new Paragraph(" "));
                } else {
                    document.add(new Paragraph(line, fontChinese));
                }
            }

            document.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            log.error("文本转PDF失败", e);
            throw new BusinessException("文本转PDF失败: " + e.getMessage());
        }
    }

    /**
     * Excel转CSV
     * 支持xlsx和xls格式
     *
     * @param file         Excel文件
     * @param sourceFormat 源格式
     * @return CSV字节数组
     */
    private byte[] excelToCsv(MultipartFile file, String sourceFormat) {
        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is);
             ByteArrayOutputStream out = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8)) {

            // 写入BOM以支持Excel正确识别UTF-8编码
            writer.write('\ufeff');

            // 获取第一个工作表
            Sheet sheet = workbook.getSheetAt(0);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            // 遍历行
            for (Row row : sheet) {
                List<String> values = new ArrayList<>();
                // 遍历单元格
                for (Cell cell : row) {
                    values.add(getCellValueAsString(cell, evaluator));
                }
                // 写入CSV行
                writer.write(String.join(",", values));
                writer.write("\n");
            }

            writer.flush();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Excel转CSV失败", e);
            throw new BusinessException("Excel转CSV失败: " + e.getMessage());
        }
    }

    /**
     * 获取单元格的字符串值
     * 处理不同类型的单元格值（数字、字符串、布尔、公式等）
     *
     * @param cell      单元格
     * @param evaluator 公式计算器
     * @return 字符串值
     */
    private String getCellValueAsString(Cell cell, FormulaEvaluator evaluator) {
        if (cell == null) {
            return "";
        }

        CellType cellType = cell.getCellType();

        // POI 4.x中，FORMULA需要单独处理
        if (cellType == CellType.FORMULA) {
            CellValue cellValue = evaluator.evaluate(cell);
            cellType = cellValue.getCellType();
            switch (cellType) {
                case NUMERIC:
                    double numValue = cellValue.getNumberValue();
                    if (numValue == Math.floor(numValue)) {
                        return String.valueOf((long) numValue);
                    }
                    return String.valueOf(numValue);
                case STRING:
                    return "\"" + cellValue.getStringValue().replace("\"", "\"\"") + "\"";
                case BOOLEAN:
                    return String.valueOf(cellValue.getBooleanValue());
                default:
                    return "";
            }
        }

        switch (cellType) {
            case STRING:
                return "\"" + cell.getStringCellValue().replace("\"", "\"\"") + "\"";
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                double value = cell.getNumericCellValue();
                if (value == Math.floor(value)) {
                    return String.valueOf((long) value);
                }
                return String.valueOf(value);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case BLANK:
                return "";
            default:
                return "";
        }
    }

}
