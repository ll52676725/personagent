package com.example.agentplatform.tools.common;

/**
 * 风险等级工具类
 * <p>提供风险等级相关的通用工具方法，包括风险等级排序和标签映射
 * <p>所有方法均为静态方法，无需实例化
 * 
 * @author System
 * @since 2025-01-01
 */
public class SeverityUtils {

    /**
     * 私有构造函数，防止实例化
     */
    private SeverityUtils() {
    }

    /**
     * 获取风险等级排序权重
     * <p>用于按风险等级排序，数值越大优先级越高
     * 
     * @param severity 风险等级字符串（high/medium/low）
     * @return 排序权重值，high=3, medium=2, low=1, 其他=0
     */
    public static int getSeverityOrder(String severity) {
        return switch (severity) {
            case "high" -> 3;
            case "medium" -> 2;
            case "low" -> 1;
            default -> 0;
        };
    }

    /**
     * 获取风险等级的中文标签
     * <p>将英文风险等级代码转换为中文显示文本
     * 
     * @param severity 风险等级字符串（high/medium/low）
     * @return 中文标签（高危/中危/低危/未知）
     */
    public static String getSeverityLabel(String severity) {
        return switch (severity) {
            case "high" -> "高危";
            case "medium" -> "中危";
            case "low" -> "低危";
            default -> "未知";
        };
    }
}
