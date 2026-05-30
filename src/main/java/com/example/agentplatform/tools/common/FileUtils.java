package com.example.agentplatform.tools.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

/**
 * 文件工具类
 * <p>提供文件操作相关的通用工具方法，包括文件扩展名检测、文件名构建、
 * Base64编码、文件大小格式化等功能
 * <p>所有方法均为静态方法，无需实例化
 * 
 * @author System
 * @since 2025-01-01
 */
@Slf4j
public class FileUtils {

    /**
     * 私有构造函数，防止实例化
     */
    private FileUtils() {
    }

    /**
     * 检测文件扩展名
     * <p>从文件名中提取扩展名并转换为小写
     * 
     * @param fileName 原始文件名
     * @return 文件扩展名（小写），如果无法检测则返回 "unknown"
     */
    public static String detectFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "unknown";
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0) {
            return "unknown";
        }
        return fileName.substring(dotIndex + 1).toLowerCase();
    }

    /**
     * 构建转换后的文件名
     * <p>保留原始文件名的主体部分，替换为目标扩展名
     * 
     * @param originalFileName 原始文件名
     * @param targetExtension 目标扩展名（不含点号）
     * @return 转换后的文件名
     */
    public static String buildConvertedFileName(String originalFileName, String targetExtension) {
        if (originalFileName == null || originalFileName.isEmpty()) {
            return "converted." + targetExtension;
        }
        int dotIndex = originalFileName.lastIndexOf('.');
        String baseName = dotIndex > 0 ? originalFileName.substring(0, dotIndex) : originalFileName;
        return baseName + "." + targetExtension;
    }

    /**
     * 将字节数据转换为Base64 Data URL格式
     * <p>生成可直接用于HTML img标签的data URI格式
     * 
     * @param data 原始字节数据
     * @param mimeType MIME类型（如：image/png、image/jpeg）
     * @return Base64编码的Data URL字符串
     */
    public static String toBase64Data(byte[] data, String mimeType) {
        return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(data);
    }

    /**
     * 格式化文件大小为易读的字符串
     * <p>自动转换为 B、KB、MB、GB、TB 等单位
     * 
     * @param bytes 文件大小（字节数）
     * @return 格式化后的文件大小字符串（如："1.5 MB"）
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        if (bytes < 1024L * 1024 * 1024 * 1024) return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        return String.format("%.1f TB", bytes / (1024.0 * 1024 * 1024 * 1024));
    }

    /**
     * 读取文件内容为字符串
     * <p>使用UTF-8编码读取文本文件
     * 
     * @param filePath 文件路径
     * @return 文件内容字符串
     * @throws IOException 读取文件时发生IO异常
     */
    public static String readFile(String filePath) throws IOException {
        return Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
    }

    /**
     * 将字符串内容写入文件
     * <p>使用UTF-8编码写入，文件不存在则创建，存在则覆盖
     * 
     * @param filePath 文件路径
     * @param content 要写入的内容
     * @throws IOException 写入文件时发生IO异常
     */
    public static void writeFile(String filePath, String content) throws IOException {
        Files.writeString(Paths.get(filePath), content, StandardCharsets.UTF_8);
    }

    /**
     * 检查文件是否存在
     * 
     * @param filePath 文件路径
     * @return 文件存在返回true，否则返回false
     */
    public static boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }
}
