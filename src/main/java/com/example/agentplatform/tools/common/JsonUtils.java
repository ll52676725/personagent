package com.example.agentplatform.tools.common;

import com.example.agentplatform.common.exception.BusinessException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * JSON工具类
 * <p>提供JSON解析和序列化相关的通用工具方法，包括JSON提取、解析、
 * 序列化、美化输出等功能
 * <p>所有方法均为静态方法，无需实例化
 * 
 * @author System
 * @since 2025-01-01
 */
@Slf4j
public class JsonUtils {

    /**
     * 私有构造函数，防止实例化
     */
    private JsonUtils() {
    }

    /**
     * 从文本中提取JSON内容
     * <p>处理AI可能在JSON前后添加的说明文字，提取出有效的JSON对象或数组
     * 
     * @param content 原始文本内容，可能包含JSON和其他说明文字
     * @return 提取出的JSON字符串，如果未找到则返回原内容
     */
    public static String extractJson(String content) {
        if (content == null) return "{}";
        content = content.trim();

        int firstBrace = content.indexOf('{');
        int lastBrace = content.lastIndexOf('}');

        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return content.substring(firstBrace, lastBrace + 1);
        }

        int firstBracket = content.indexOf('[');
        int lastBracket = content.lastIndexOf(']');

        if (firstBracket >= 0 && lastBracket > firstBracket) {
            return content.substring(firstBracket, lastBracket + 1);
        }

        return content;
    }

    /**
     * 解析JSON字符串为指定类型的对象
     * 
     * @param <T> 目标对象类型
     * @param mapper Jackson ObjectMapper实例
     * @param json JSON字符串
     * @param clazz 目标类的Class对象
     * @return 解析后的对象
     * @throws BusinessException 解析失败时抛出业务异常
     */
    public static <T> T parseJson(ObjectMapper mapper, String json, Class<T> clazz) {
        try {
            return mapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("JSON解析失败", e);
            throw new BusinessException("JSON解析失败: " + e.getMessage());
        }
    }

    /**
     * 解析JSON字符串为指定泛型类型的对象
     * <p>用于解析复杂的泛型类型，如 List&lt;User&gt;
     * 
     * @param <T> 目标对象类型
     * @param mapper Jackson ObjectMapper实例
     * @param json JSON字符串
     * @param typeRef 类型引用，用于描述泛型类型
     * @return 解析后的对象
     * @throws BusinessException 解析失败时抛出业务异常
     */
    public static <T> T parseJson(ObjectMapper mapper, String json, TypeReference<T> typeRef) {
        try {
            return mapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            log.error("JSON解析失败", e);
            throw new BusinessException("JSON解析失败: " + e.getMessage());
        }
    }

    /**
     * 解析JSON字符串为JsonNode树模型
     * <p>用于灵活访问JSON结构，不需要预先定义Java类
     * 
     * @param mapper Jackson ObjectMapper实例
     * @param json JSON字符串
     * @return JsonNode对象，可以通过路径访问JSON字段
     * @throws BusinessException 解析失败时抛出业务异常
     */
    public static JsonNode parseTree(ObjectMapper mapper, String json) {
        try {
            return mapper.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("JSON解析失败", e);
            throw new BusinessException("JSON解析失败: " + e.getMessage());
        }
    }

    /**
     * 将对象序列化为JSON字符串
     * 
     * @param mapper Jackson ObjectMapper实例
     * @param object 要序列化的对象
     * @return JSON字符串
     * @throws BusinessException 序列化失败时抛出业务异常
     */
    public static String toJson(ObjectMapper mapper, Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("JSON序列化失败", e);
            throw new BusinessException("JSON序列化失败: " + e.getMessage());
        }
    }

    /**
     * 将对象序列化为美化格式的JSON字符串
     * <p>输出格式化的JSON，便于阅读和调试
     * 
     * @param mapper Jackson ObjectMapper实例
     * @param object 要序列化的对象
     * @return 美化格式的JSON字符串
     * @throws BusinessException 序列化失败时抛出业务异常
     */
    public static String toPrettyJson(ObjectMapper mapper, Object object) {
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("JSON序列化失败", e);
            throw new BusinessException("JSON序列化失败: " + e.getMessage());
        }
    }
}
