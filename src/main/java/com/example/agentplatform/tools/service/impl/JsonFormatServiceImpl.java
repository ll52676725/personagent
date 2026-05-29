package com.example.agentplatform.tools.service.impl;

import com.example.agentplatform.agent.article.service.GenerateService;
import com.example.agentplatform.common.exception.BusinessException;
import com.example.agentplatform.tools.dto.JsonErrorDetailDTO;
import com.example.agentplatform.tools.dto.JsonFormatRequestDTO;
import com.example.agentplatform.tools.dto.JsonFormatResultDTO;
import com.example.agentplatform.tools.service.JsonFormatService;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSON格式化服务实现类
 * 提供JSON格式化、压缩、校验和AI修复功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JsonFormatServiceImpl implements JsonFormatService {

    /**
     * Jackson ObjectMapper，用于JSON解析和序列化
     */
    private final ObjectMapper objectMapper;

    /**
     * AI生成服务，用于调用LLM修复JSON
     */
    private final GenerateService generateService;

    /**
     * 是否启用AI修复降级模式
     */
    @Value("${agent.platform.ai.fallback-enabled:true}")
    private boolean fallbackEnabled;

    /**
     * AI修复JSON的系统提示词
     * 指导LLM如何正确修复JSON格式错误
     */
    private static final String JSON_FIX_SYSTEM_PROMPT = """
        你是一个专业的JSON格式修复专家，擅长修复各种JSON语法错误。
        
        修复规则：
        1. 保持原始数据的语义和结构不变，只修复语法错误
        2. 常见错误类型及修复方法：
           - 缺少引号：为未加引号的键名和字符串值添加双引号
           - 多余逗号：删除对象或数组最后一个元素后的逗号
           - 单引号：将单引号替换为双引号
           - 注释：删除//和/* */格式的注释
           - 键名重复：保留最后一个出现的键值对
           - 未闭合的括号/方括号：补充缺失的闭合符号
           - 控制字符：转义或删除无效的控制字符
           - 大写布尔值/Null：将TRUE/FALSE/NULL转换为小写
        3. 只返回修复后的JSON内容，不要包含任何其他说明文字
        4. 如果无法修复，返回一个空对象 {}
        5. 确保修复后的JSON是严格有效的标准JSON格式
        """;

    /**
     * JSON错误类型正则表达式映射
     * 用于识别Jackson抛出的错误信息并分类
     */
    private static final Map<Pattern, String> ERROR_TYPE_PATTERNS = new LinkedHashMap<>();

    static {
        ERROR_TYPE_PATTERNS.put(Pattern.compile("Unexpected character.*was expecting.*"), "语法错误");
        ERROR_TYPE_PATTERNS.put(Pattern.compile("Unrecognized character escape"), "无效转义字符");
        ERROR_TYPE_PATTERNS.put(Pattern.compile("Unterminated string"), "字符串未闭合");
        ERROR_TYPE_PATTERNS.put(Pattern.compile("Unexpected end-of-input"), "JSON未完整");
        ERROR_TYPE_PATTERNS.put(Pattern.compile("Duplicate field"), "重复键名");
        ERROR_TYPE_PATTERNS.put(Pattern.compile("Unexpected comma"), "多余逗号");
        ERROR_TYPE_PATTERNS.put(Pattern.compile("Missing.*colon"), "缺少冒号");
        ERROR_TYPE_PATTERNS.put(Pattern.compile("Invalid UTF-8"), "编码错误");
    }

    @Override
    public JsonFormatResultDTO format(JsonFormatRequestDTO request) {
        String content = request.getContent().trim();
        int indentSize = request.getIndentSize() != null ? request.getIndentSize() : 2;
        boolean sortKeys = request.getSortKeys() != null && request.getSortKeys();

        try {
            // 创建专用的ObjectMapper副本，根据请求参数配置
            ObjectMapper mapper = objectMapper.copy();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, sortKeys);
            mapper.getFactory().setCharacterEscapes(null);

            // 解析JSON
            JsonNode rootNode = mapper.readTree(content);

            // 根据缩进空格数生成格式化后的JSON
            String formattedJson = formatJsonWithIndent(rootNode, mapper, indentSize);

            // 生成压缩格式
            String compactJson = mapper.writeValueAsString(rootNode);

            // 获取JSON类型
            String jsonType = getJsonType(rootNode);

            // 获取统计信息
            String statistics = generateStatistics(rootNode);

            log.info("JSON格式化成功，类型: {}, 缩进: {}空格", jsonType, indentSize);

            return JsonFormatResultDTO.builder()
                    .success(true)
                    .formattedJson(formattedJson)
                    .compactJson(compactJson)
                    .indentSize(indentSize)
                    .jsonType(jsonType)
                    .statistics(statistics)
                    .errors(Collections.emptyList())
                    .build();

        } catch (JsonParseException e) {
            log.warn("JSON格式化失败，语法错误: {}", e.getMessage());
            List<JsonErrorDetailDTO> errors = parseJsonError(e, content);
            return JsonFormatResultDTO.builder()
                    .success(false)
                    .errors(errors)
                    .indentSize(indentSize)
                    .build();
        } catch (Exception e) {
            log.error("JSON格式化失败，未知错误", e);
            return JsonFormatResultDTO.builder()
                    .success(false)
                    .errors(List.of(JsonErrorDetailDTO.builder()
                            .errorType("未知错误")
                            .message(e.getMessage())
                            .suggestion("请检查JSON内容是否完整，或使用AI修复功能尝试修复")
                            .build()))
                    .indentSize(indentSize)
                    .build();
        }
    }

    @Override
    public JsonFormatResultDTO compact(JsonFormatRequestDTO request) {
        String content = request.getContent().trim();

        try {
            JsonNode rootNode = objectMapper.readTree(content);
            String compactJson = objectMapper.writeValueAsString(rootNode);

            log.info("JSON压缩成功，压缩前: {}字符，压缩后: {}字符", content.length(), compactJson.length());

            return JsonFormatResultDTO.builder()
                    .success(true)
                    .compactJson(compactJson)
                    .formattedJson(null)
                    .jsonType(getJsonType(rootNode))
                    .statistics(generateStatistics(rootNode))
                    .errors(Collections.emptyList())
                    .build();

        } catch (JsonParseException e) {
            log.warn("JSON压缩失败，语法错误: {}", e.getMessage());
            List<JsonErrorDetailDTO> errors = parseJsonError(e, content);
            return JsonFormatResultDTO.builder()
                    .success(false)
                    .errors(errors)
                    .build();
        } catch (Exception e) {
            log.error("JSON压缩失败，未知错误", e);
            return JsonFormatResultDTO.builder()
                    .success(false)
                    .errors(List.of(JsonErrorDetailDTO.builder()
                            .errorType("未知错误")
                            .message(e.getMessage())
                            .suggestion("请检查JSON内容是否完整")
                            .build()))
                    .build();
        }
    }

    @Override
    public JsonFormatResultDTO validate(JsonFormatRequestDTO request) {
        String content = request.getContent().trim();

        try {
            JsonNode rootNode = objectMapper.readTree(content);
            log.info("JSON校验成功");

            return JsonFormatResultDTO.builder()
                    .success(true)
                    .jsonType(getJsonType(rootNode))
                    .statistics(generateStatistics(rootNode))
                    .errors(Collections.emptyList())
                    .build();

        } catch (JsonParseException e) {
            log.warn("JSON校验失败: {}", e.getMessage());
            List<JsonErrorDetailDTO> errors = parseJsonError(e, content);
            return JsonFormatResultDTO.builder()
                    .success(false)
                    .errors(errors)
                    .build();
        } catch (Exception e) {
            log.error("JSON校验失败，未知错误", e);
            return JsonFormatResultDTO.builder()
                    .success(false)
                    .errors(List.of(JsonErrorDetailDTO.builder()
                            .errorType("未知错误")
                            .message(e.getMessage())
                            .suggestion("请检查JSON内容是否完整")
                            .build()))
                    .build();
        }
    }

    @Override
    public JsonFormatResultDTO fixWithAI(JsonFormatRequestDTO request) {
        String content = request.getContent().trim();

        try {
            // 先尝试简单的本地修复
            String simpleFixed = trySimpleFix(content);
            if (simpleFixed != null) {
                try {
                    JsonNode testNode = objectMapper.readTree(simpleFixed);
                    log.info("JSON本地简单修复成功");
                    return buildFixResult(simpleFixed, content, "已执行本地简单修复：自动处理了常见格式问题", request);
                } catch (Exception e) {
                    log.debug("简单修复未成功，继续使用AI修复: {}", e.getMessage());
                }
            }

            // 调用LLM进行修复
            String userPrompt = String.format("""
                请修复以下JSON中的语法错误，只返回修复后的JSON内容：
                    
                原始JSON内容：
                ```json
                %s
                ```
                    
                请确保修复后的JSON是完全合法的标准JSON格式。
                """, content);

            log.info("调用AI修复JSON，原始长度: {}字符", content.length());

            var result = generateService.generateWithPrompt(JSON_FIX_SYSTEM_PROMPT, userPrompt, "json-fix");
            String fixedJson = result.getContent();

            if (fixedJson == null || fixedJson.trim().isEmpty()) {
                throw new BusinessException("AI修复失败，未返回有效内容");
            }

            // 检查是否是降级模式返回的内容（不是有效的JSON）
            if ("fallback".equals(result.getModel()) || fixedJson.contains("降级模式")) {
                log.info("AI处于降级模式，使用本地修复策略");
                // 降级模式：尝试增强版的本地修复
                return tryEnhancedLocalFix(content, request);
            }

            log.info("AI返回内容: {}", fixedJson.substring(0, Math.min(200, fixedJson.length())));

            // 清理AI返回的内容，移除可能的markdown代码块标记
            fixedJson = cleanAiResponse(fixedJson);

            // 验证修复后的JSON是否有效
            JsonNode testNode = objectMapper.readTree(fixedJson);

            // 生成修复说明
            String fixDescription = generateFixDescription(content, fixedJson);

            log.info("AI修复JSON成功，使用模型: {}", result.getModel());

            return buildFixResult(fixedJson, content, fixDescription, request);

        } catch (Exception e) {
            log.error("AI修复JSON失败", e);

            // 无论是否启用降级模式，都尝试本地修复作为最后手段
            try {
                return tryEnhancedLocalFix(content, request);
            } catch (Exception ex) {
                log.warn("增强版本地修复也失败: {}", ex.getMessage());
            }

            return JsonFormatResultDTO.builder()
                    .success(false)
                    .aiFixSuccess(false)
                    .aiFixDescription("AI修复失败：" + e.getMessage())
                    .errors(List.of(JsonErrorDetailDTO.builder()
                            .errorType("AI修复失败")
                            .message(e.getMessage())
                            .suggestion("请检查JSON内容是否有过多错误，或手动修复后重试")
                            .build()))
                    .build();
        }
    }

    /**
     * 尝试增强版的本地JSON修复
     * 综合使用多种修复策略，处理更复杂的格式错误
     *
     * @param content 原始JSON内容
     * @param request 请求参数
     * @return 修复结果DTO
     */
    private JsonFormatResultDTO tryEnhancedLocalFix(String content, JsonFormatRequestDTO request) throws JsonProcessingException {
        log.info("尝试增强版本地JSON修复");

        // 修复策略1：简单修复
        String fixed = trySimpleFix(content);
        if (fixed != null) {
            try {
                objectMapper.readTree(fixed);
                log.info("增强版本地修复成功（策略1：简单修复）");
                return buildFixResult(fixed, content, "本地智能修复成功（策略1：简单修复）", request);
            } catch (Exception ignored) {
            }
        }

        // 修复策略2：尝试修复未闭合的字符串
        fixed = fixUnclosedStrings(content);
        if (fixed != null) {
            try {
                objectMapper.readTree(fixed);
                log.info("增强版本地修复成功（策略2：修复未闭合字符串）");
                return buildFixResult(fixed, content, "本地智能修复成功（策略2：修复未闭合字符串）", request);
            } catch (Exception ignored) {
            }
        }

        // 修复策略3：移除无效内容直到有效JSON
        fixed = tryExtractValidJson(content);
        if (fixed != null) {
            try {
                objectMapper.readTree(fixed);
                log.info("增强版本地修复成功（策略3：提取有效JSON）");
                return buildFixResult(fixed, content, "本地智能修复成功（策略3：提取有效JSON片段）", request);
            } catch (Exception ignored) {
            }
        }

        throw new BusinessException("所有本地修复策略均失败");
    }

    /**
     * 尝试修复未闭合的字符串
     *
     * @param content 原始JSON内容
     * @return 修复后的JSON
     */
    private String fixUnclosedStrings(String content) {
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        boolean escape = false;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            if (escape) {
                result.append(c);
                escape = false;
                continue;
            }

            if (c == '\\') {
                result.append(c);
                escape = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
            }

            result.append(c);
        }

        // 如果字符串未闭合，添加闭合引号
        if (inString) {
            result.append('"');
        }

        return result.toString();
    }

    /**
     * 尝试从内容中提取有效的JSON片段
     *
     * @param content 原始内容
     * @return 有效的JSON片段
     */
    private String tryExtractValidJson(String content) {
        // 尝试从开头的{或[开始
        int start = Math.max(content.indexOf('{'), content.indexOf('['));
        if (start < 0) return null;

        String candidate = content.substring(start);

        // 尝试找到匹配的闭合
        char open = candidate.charAt(0);
        char close = (open == '{') ? '}' : ']';

        int count = 0;
        boolean inString = false;
        boolean escape = false;

        for (int i = 0; i < candidate.length(); i++) {
            char c = candidate.charAt(i);

            if (escape) {
                escape = false;
                continue;
            }

            if (c == '\\') {
                escape = true;
                continue;
            }

            if (c == '"') {
                inString = !inString;
                continue;
            }

            if (!inString) {
                if (c == open) count++;
                if (c == close) count--;
                if (count == 0) {
                    return candidate.substring(0, i + 1);
                }
            }
        }

        return null;
    }

    /**
     * 构建修复成功的结果对象
     *
     * @param fixedJson      修复后的JSON
     * @param originalJson   原始JSON
     * @param fixDescription 修复说明
     * @param request        请求参数
     * @return 格式化结果DTO
     */
    private JsonFormatResultDTO buildFixResult(String fixedJson, String originalJson, String fixDescription,
                                               JsonFormatRequestDTO request) throws JsonProcessingException {
        ObjectMapper mapper = objectMapper.copy();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        int indentSize = request.getIndentSize() != null ? request.getIndentSize() : 2;
        JsonNode rootNode = mapper.readTree(fixedJson);

        String formattedJson = formatJsonWithIndent(rootNode, mapper, indentSize);
        String compactJson = objectMapper.writeValueAsString(rootNode);

        return JsonFormatResultDTO.builder()
                .success(true)
                .aiFixSuccess(true)
                .aiFixedJson(formattedJson)
                .formattedJson(formattedJson)
                .compactJson(compactJson)
                .aiFixDescription(fixDescription)
                .jsonType(getJsonType(rootNode))
                .statistics(generateStatistics(rootNode))
                .indentSize(indentSize)
                .errors(Collections.emptyList())
                .build();
    }

    /**
     * 解析JSON解析错误，生成详细的错误信息
     *
     * @param e       JSON解析异常
     * @param content 原始JSON内容
     * @return 错误详情列表
     */
    private List<JsonErrorDetailDTO> parseJsonError(JsonParseException e, String content) {
        List<JsonErrorDetailDTO> errors = new ArrayList<>();

        JsonLocation location = e.getLocation();
        String message = e.getOriginalMessage();

        // 识别错误类型
        String errorType = "语法错误";
        for (Map.Entry<Pattern, String> entry : ERROR_TYPE_PATTERNS.entrySet()) {
            if (entry.getKey().matcher(message).find()) {
                errorType = entry.getValue();
                break;
            }
        }

        // 提取错误位置附近的上下文
        String errorContext = extractErrorContext(content, location);

        // 生成修复建议
        String suggestion = generateSuggestion(errorType, message);

        errors.add(JsonErrorDetailDTO.builder()
                .errorType(errorType)
                .lineNumber(location.getLineNr())
                .columnNumber(location.getColumnNr())
                .errorContext(errorContext)
                .message(message)
                .suggestion(suggestion)
                .build());

        return errors;
    }

    /**
     * 提取错误位置附近的上下文内容
     *
     * @param content  原始JSON内容
     * @param location 错误位置
     * @return 错误位置附近的内容片段
     */
    private String extractErrorContext(String content, JsonLocation location) {
        try {
            long offset = location.getCharOffset();
            int start = (int) Math.max(0, offset - 20);
            int end = (int) Math.min(content.length(), offset + 20);

            String context = content.substring(start, end);

            // 添加位置标记
            int errorPos = (int) (offset - start);
            if (errorPos >= 0 && errorPos < context.length()) {
                StringBuilder marked = new StringBuilder();
                marked.append(context, 0, errorPos);
                marked.append(" ►");
                marked.append(context.charAt(errorPos));
                marked.append("◄ ");
                if (errorPos + 1 < context.length()) {
                    marked.append(context.substring(errorPos + 1));
                }
                return marked.toString();
            }

            return context;
        } catch (Exception e) {
            return content.substring(0, Math.min(content.length(), 50));
        }
    }

    /**
     * 根据错误类型生成修复建议
     *
     * @param errorType 错误类型
     * @param message   错误信息
     * @return 修复建议
     */
    private String generateSuggestion(String errorType, String message) {
        return switch (errorType) {
            case "语法错误" -> "检查错误位置附近的语法，确保使用正确的JSON语法。常见问题包括：缺少逗号、引号不匹配、括号不闭合等。";
            case "无效转义字符" -> "JSON中只能使用特定的转义序列：\\n、\\r、\\t、\\b、\\f、\\\"、\\\\、\\/。请移除或修正无效的转义字符。";
            case "字符串未闭合" -> "检查错误位置附近的字符串，确保所有字符串都有闭合的双引号。";
            case "JSON未完整" -> "JSON内容不完整，请检查是否有缺失的闭合括号、方括号或引号。";
            case "重复键名" -> "JSON不允许重复的键名，请删除重复的键或重命名。";
            case "多余逗号" -> "删除对象或数组最后一个元素后面的逗号。";
            case "缺少冒号" -> "在键名和值之间添加冒号。";
            case "编码错误" -> "请确保JSON内容使用UTF-8编码，移除或转义无效字符。";
            default -> "请使用AI修复功能尝试自动修复，或仔细检查JSON内容。";
        };
    }

    /**
     * 使用指定的缩进空格数格式化JSON
     *
     * @param rootNode   JSON根节点
     * @param mapper     ObjectMapper实例
     * @param indentSize 缩进空格数
     * @return 格式化后的JSON字符串
     */
    private String formatJsonWithIndent(JsonNode rootNode, ObjectMapper mapper, int indentSize) throws JsonProcessingException {
        // 先使用默认缩进格式化
        String defaultIndent = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);

        // 如果不是2空格缩进，进行替换
        if (indentSize != 2) {
            String newIndent = " ".repeat(indentSize);
            // 替换每行开头的2空格缩进
            Pattern pattern = Pattern.compile("^(  )+", Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(defaultIndent);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                int level = matcher.group().length() / 2;
                matcher.appendReplacement(sb, newIndent.repeat(level));
            }
            matcher.appendTail(sb);
            return sb.toString();
        }

        return defaultIndent;
    }

    /**
     * 获取JSON数据的类型
     *
     * @param node JSON节点
     * @return 类型描述
     */
    private String getJsonType(JsonNode node) {
        if (node.isObject()) return "object（对象）";
        if (node.isArray()) return "array（数组）";
        if (node.isTextual()) return "string（字符串）";
        if (node.isNumber()) return "number（数字）";
        if (node.isBoolean()) return "boolean（布尔值）";
        if (node.isNull()) return "null（空值）";
        return "unknown（未知）";
    }

    /**
     * 生成JSON数据的统计信息
     *
     * @param rootNode JSON根节点
     * @return 统计信息字符串
     */
    private String generateStatistics(JsonNode rootNode) {
        StringBuilder stats = new StringBuilder();

        if (rootNode.isObject()) {
            int fieldCount = rootNode.size();
            stats.append("键值对数量: ").append(fieldCount);
            int nestedObjects = 0;
            int nestedArrays = 0;
            for (JsonNode child : rootNode) {
                if (child.isObject()) nestedObjects++;
                if (child.isArray()) nestedArrays++;
            }
            stats.append("，嵌套对象: ").append(nestedObjects);
            stats.append("，嵌套数组: ").append(nestedArrays);
        } else if (rootNode.isArray()) {
            int length = rootNode.size();
            stats.append("数组长度: ").append(length);
            if (length > 0) {
                JsonNode first = rootNode.get(0);
                stats.append("，元素类型: ").append(first.getNodeType().toString().toLowerCase());
            }
        } else {
            stats.append("值类型: ").append(rootNode.getNodeType().toString().toLowerCase());
            if (rootNode.isTextual()) {
                stats.append("，长度: ").append(rootNode.asText().length());
            } else if (rootNode.isNumber()) {
                stats.append("，值: ").append(rootNode.asText());
            }
        }

        return stats.toString();
    }

    /**
     * 尝试简单的本地JSON修复
     * 处理一些常见的、可以通过简单规则修复的问题
     *
     * @param content 原始JSON内容
     * @return 修复后的JSON，如果无法修复返回null
     */
    private String trySimpleFix(String content) {
        String fixed = content;

        try {
            // 1. 修复未闭合的字符串（缺失引号）
            fixed = fixUnclosedStrings(fixed);

            // 2. 将单引号替换为双引号
            fixed = fixed.replaceAll("'", "\"");

            // 3. 删除多余的逗号（对象或数组最后一个元素后的逗号）
            fixed = fixed.replaceAll(",\\s*([}\\]])", "$1");

            // 4. 为未加引号的键名添加双引号（简单处理，可能不完全）
            Pattern unquotedKey = Pattern.compile("([{,]\\s*)([a-zA-Z_][a-zA-Z0-9_]*)\\s*:");
            Matcher matcher = unquotedKey.matcher(fixed);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                matcher.appendReplacement(sb, matcher.group(1) + "\"" + matcher.group(2) + "\":");
            }
            matcher.appendTail(sb);
            fixed = sb.toString();

            // 5. 删除//注释（支持多行）
            fixed = fixed.replaceAll("//[^\\n]*", "");

            // 6. 尝试修复未闭合的括号
            int openBraces = countChar(fixed, '{');
            int closeBraces = countChar(fixed, '}');
            int openBrackets = countChar(fixed, '[');
            int closeBrackets = countChar(fixed, ']');

            while (closeBraces < openBraces) {
                fixed += "}";
                closeBraces++;
            }
            while (closeBrackets < openBrackets) {
                fixed += "]";
                closeBrackets++;
            }

            // 7. 修复大写的TRUE/FALSE/NULL
            fixed = fixed.replaceAll("\\bTRUE\\b", "true")
                    .replaceAll("\\bFALSE\\b", "false")
                    .replaceAll("\\bNULL\\b", "null");

            // 8. 再次检查并修复字符串闭合（前面的操作可能引入新问题）
            fixed = fixUnclosedStrings(fixed);

            return fixed;
        } catch (Exception e) {
            log.warn("简单JSON修复失败", e);
            return null;
        }
    }

    /**
     * 统计字符串中指定字符的出现次数
     *
     * @param str 字符串
     * @param c   目标字符
     * @return 出现次数
     */
    private int countChar(String str, char c) {
        int count = 0;
        for (char ch : str.toCharArray()) {
            if (ch == c) count++;
        }
        return count;
    }

    /**
     * 清理AI返回的内容，移除可能的markdown代码块标记
     *
     * @param response AI返回的原始内容
     * @return 清理后的JSON内容
     */
    private String cleanAiResponse(String response) {
        String cleaned = response.trim();

        // 移除markdown代码块
        cleaned = cleaned.replaceAll("^```json\\s*", "")
                .replaceAll("^```\\s*", "")
                .replaceAll("\\s*```$", "");

        return cleaned.trim();
    }

    /**
     * 生成修复说明，描述对JSON做了哪些修改
     *
     * @param original 原始JSON
     * @param fixed    修复后的JSON
     * @return 修复说明
     */
    private String generateFixDescription(String original, String fixed) {
        List<String> changes = new ArrayList<>();

        // 检查是否替换了单引号
        if (original.contains("'") && !fixed.contains("'")) {
            changes.add("将单引号替换为双引号");
        }

        // 检查是否删除了多余逗号
        int originalCommas = countChar(original, ',');
        int fixedCommas = countChar(fixed, ',');
        if (originalCommas > fixedCommas) {
            changes.add("删除了多余的逗号");
        }

        // 检查是否添加了引号
        if (original.matches(".*[{,]\\s*[a-zA-Z_][a-zA-Z0-9_]*\\s*:.*")) {
            changes.add("为键名添加了双引号");
        }

        // 检查是否修复了大小写
        if (original.matches(".*\\b(TRUE|FALSE|NULL)\\b.*")) {
            changes.add("修正了关键字的大小写");
        }

        // 检查是否修复了括号
        int originalBraces = countChar(original, '{') - countChar(original, '}');
        int fixedBraces = countChar(fixed, '{') - countChar(fixed, '}');
        if (originalBraces != 0 && fixedBraces == 0) {
            changes.add("补充了缺失的大括号");
        }

        int originalBrackets = countChar(original, '[') - countChar(original, ']');
        int fixedBrackets = countChar(fixed, '[') - countChar(fixed, ']');
        if (originalBrackets != 0 && fixedBrackets == 0) {
            changes.add("补充了缺失的方括号");
        }

        if (changes.isEmpty()) {
            changes.add("修复了JSON语法错误");
        }

        return "AI已自动修复JSON格式问题：" + String.join("；", changes) + "。修复后的JSON已通过语法校验。";
    }
}
