package com.example.agentplatform.tools.service.impl;

import com.example.agentplatform.agent.article.service.GenerateService;
import com.example.agentplatform.common.exception.BusinessException;
import com.example.agentplatform.tools.dto.*;
import com.example.agentplatform.tools.service.RegexService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 正则表达式服务实现类，提供校验、AI生成、AI修正三大核心功能。
 * 优先使用经过验证的标准知识库确保一致性和准确性，AI生成后自动验证，失败时降级到知识库或本地规则。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegexServiceImpl implements RegexService {

    private final ObjectMapper objectMapper;
    private final GenerateService generateService;

    @Value("${agent.platform.ai.fallback-enabled:true}")
    private boolean fallbackEnabled;

    @Value("${agent.platform.ai.regex.knowledge-base-enabled:true}")
    private boolean knowledgeBaseEnabled;

    private static final List<RegexPatternEntry> KNOWLEDGE_BASE = new ArrayList<>();

    static {
        KNOWLEDGE_BASE.add(new RegexPatternEntry(
                "中国大陆手机号", "匹配中国大陆手机号码，11位数字，1开头",
                "^1[3-9]\\d{9}$",
                Arrays.asList("13812345678", "15900001111", "18677778888"),
                Arrays.asList("12345678901", "0381234567", "138123456789"),
                0.95
        ));
        KNOWLEDGE_BASE.add(new RegexPatternEntry(
                "台湾手机号", "匹配台湾手机号码，09开头共10位数字",
                "^09\\d{8}$",
                Arrays.asList("0912345678", "0987654321", "0900123456"),
                Arrays.asList("13812345678", "091234567", "09123456789"),
                0.95
        ));
        KNOWLEDGE_BASE.add(new RegexPatternEntry(
                "香港手机号", "匹配香港手机号码，8位数字，5/6/9开头",
                "^[569]\\d{7}$",
                Arrays.asList("51234567", "61234567", "91234567"),
                Arrays.asList("12345678", "41234567", "512345678"),
                0.95
        ));
        KNOWLEDGE_BASE.add(new RegexPatternEntry(
                "邮箱地址", "匹配标准电子邮件地址格式",
                "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
                Arrays.asList("test@example.com", "user.name@domain.org", "a123@test.cn"),
                Arrays.asList("test@", "@example.com", "test@.com"),
                0.9
        ));
        KNOWLEDGE_BASE.add(new RegexPatternEntry(
                "身份证号", "匹配中国大陆18位身份证号码",
                "^\\d{17}[\\dXx]$",
                Arrays.asList("110101199001011234", "31010119901231567X"),
                Arrays.asList("11010119900101123", "1101011990010112345"),
                0.95
        ));
        KNOWLEDGE_BASE.add(new RegexPatternEntry(
                "URL链接", "匹配HTTP/HTTPS URL链接",
                "^https?://[\\w\\-]+(\\.[\\w\\-]+)+[/#?]?.*$",
                Arrays.asList("https://www.example.com", "http://test.org/path?a=1"),
                Arrays.asList("www.example.com", "ftp://test.com"),
                0.85
        ));
        KNOWLEDGE_BASE.add(new RegexPatternEntry(
                "IPv4地址", "匹配标准IPv4地址格式",
                "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$",
                Arrays.asList("192.168.1.1", "10.0.0.1", "255.255.255.255"),
                Arrays.asList("256.1.1.1", "192.168.1", "192.168.1.1.1"),
                0.95
        ));
        KNOWLEDGE_BASE.add(new RegexPatternEntry(
                "中文字符", "匹配纯中文字符（Unicode中文范围）",
                "^[\\u4e00-\\u9fa5]+$",
                Arrays.asList("你好", "中文测试"),
                Arrays.asList("hello", "你好hello"),
                0.9
        ));
        KNOWLEDGE_BASE.add(new RegexPatternEntry(
                "数字", "匹配整数和小数（支持负数）",
                "^-?\\d+(\\.\\d+)?$",
                Arrays.asList("123", "-456", "3.14", "-0.5"),
                Arrays.asList("12a3", "1.2.3", "12."),
                0.9
        ));
        KNOWLEDGE_BASE.add(new RegexPatternEntry(
                "日期", "匹配YYYY-MM-DD或YYYY/MM/DD日期格式",
                "^\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}$",
                Arrays.asList("2024-01-15", "2024/1/15", "2024/12/31"),
                Arrays.asList("2024-13-01", "2024-01-32", "20240115"),
                0.85
        ));
        KNOWLEDGE_BASE.add(new RegexPatternEntry(
                "密码强度", "强密码：8-20位，包含大小写字母和数字",
                "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,20}$",
                Arrays.asList("Test1234", "Abc12345", "Hello123@"),
                Arrays.asList("test1234", "TEST1234", "Test12"),
                0.9
        ));
        KNOWLEDGE_BASE.add(new RegexPatternEntry(
                "HTML标签", "匹配HTML标签（提取标签内容）",
                "<([a-zA-Z][a-zA-Z0-9]*)\\b[^>]*>(.*?)</\\1>",
                Arrays.asList("<div>内容</div>", "<p>测试</p>"),
                Collections.emptyList(),
                0.8
        ));
        KNOWLEDGE_BASE.add(new RegexPatternEntry(
                "车牌号", "匹配中国大陆车牌号",
                "^[京津沪渝冀豫云辽黑湘皖鲁新苏浙赣鄂桂甘晋蒙陕吉闽贵粤青藏川宁琼使领][A-Z][A-Z0-9]{4,5}[A-Z0-9挂学警港澳]$",
                Arrays.asList("京A12345", "沪B88888", "粤Z1234港"),
                Arrays.asList("京12345", "京AA123456"),
                0.9
        ));
    }

    private static final String REGEX_GENERATE_SYSTEM_PROMPT = """
            你是一个专业的正则表达式生成专家。请根据用户的自然语言描述生成准确、一致的Java正则表达式。
            
            严格遵循以下规则：
            1. 生成的正则必须符合Java Pattern的语法规范，必须可以被Pattern.compile()成功编译
            2. 对于常见模式（手机号、邮箱、URL、身份证等），请使用行业标准的、经过验证的正则表达式
            3. 必须包含^开头和$结尾锚定符，确保完整匹配
            4. 提供的测试用例必须准确：匹配的示例必须能被正则匹配，不匹配的示例必须不能匹配
            5. 置信度(confidence)必须准确反映正则的可靠程度：
               - 0.95以上：经过严格验证的标准模式
               - 0.8-0.95：可靠但可能有边界情况
               - 0.6-0.8：基本可用但需要验证
               - 0.6以下：推测性结果
            
            必须返回严格的JSON格式，结构如下：
            {
              "pattern": "正则表达式字符串，必须可编译",
              "description": "简短中文描述",
              "explanation": "详细解释各部分含义",
              "testCases": ["匹配示例1", "匹配示例2", "不匹配示例1", "不匹配示例2"],
              "alternatives": [
                {"pattern": "替代正则1", "description": "说明1"},
                {"pattern": "替代正则2", "description": "说明2"}
              ],
              "confidence": 0.9
            }
            
            只返回JSON，不要包含markdown标记、代码块或其他文字说明。
            """;

    private static final String REGEX_FIX_SYSTEM_PROMPT = """
            你是一个正则表达式修复专家。请仔细分析用户提供的有问题的正则表达式，诊断错误原因并提供准确的修复方案。
            
            修复原则：
            1. 保持原始意图不变，只修复语法和逻辑错误
            2. 修复后的正则必须可以被Java Pattern.compile()成功编译
            3. 详细说明修复了什么问题
            4. 提供2-3条使用建议
            
            必须返回严格的JSON格式，结构如下：
            {
              "fixedPattern": "修复后的正则表达式",
              "fixDescription": "修复说明，描述具体做了什么修改",
              "explanation": "修复后正则各部分含义解释",
              "suggestions": ["建议1", "建议2", "建议3"],
              "valid": true
            }
            
            valid字段必须为布尔值，表示修复后的正则是否真的可以编译通过。
            只返回JSON，不要包含其他内容。
            """;

    /**
     * 对正则表达式进行语法校验和匹配测试。编译正则表达式并在测试字符串上执行匹配，
     * 返回匹配结果、捕获组等信息；若语法错误则返回错误详情。
     *
     * @param request 校验请求DTO，包含正则表达式、测试字符串和标志位
     * @return 校验结果DTO
     */
    @Override
    public RegexValidateResultDTO validate(RegexValidateRequestDTO request) {
        String pattern = request.getPattern();
        String testString = request.getTestString();
        int flags = request.getFlags() != null ? request.getFlags() : 0;

        try {
            Pattern compiled = Pattern.compile(pattern, flags);

            if (testString == null || testString.isEmpty()) {
                return RegexValidateResultDTO.builder()
                        .success(true)
                        .valid(true)
                        .pattern(pattern)
                        .description(describePattern(pattern, flags))
                        .matches(Collections.emptyList())
                        .matchCount(0)
                        .errors(Collections.emptyList())
                        .build();
            }

            Matcher matcher = compiled.matcher(testString);
            List<RegexValidateResultDTO.MatchResultDTO> matches = new ArrayList<>();
            while (matcher.find()) {
                List<String> groups = new ArrayList<>();
                for (int i = 0; i <= matcher.groupCount(); i++) {
                    groups.add(matcher.group(i));
                }
                matches.add(RegexValidateResultDTO.MatchResultDTO.builder()
                        .matchedText(matcher.group())
                        .startIndex(matcher.start())
                        .endIndex(matcher.end())
                        .groups(groups)
                        .build());
            }

            log.info("正则表达式校验成功，匹配数量: {}", matches.size());

            return RegexValidateResultDTO.builder()
                    .success(true)
                    .valid(true)
                    .pattern(pattern)
                    .description(describePattern(pattern, flags))
                    .matches(matches)
                    .matchCount(matches.size())
                    .groups(extractGroupNames(pattern))
                    .errors(Collections.emptyList())
                    .build();

        } catch (PatternSyntaxException e) {
            log.warn("正则表达式语法错误: {}", e.getMessage());
            return RegexValidateResultDTO.builder()
                    .success(false)
                    .valid(false)
                    .pattern(pattern)
                    .errors(List.of(
                            e.getMessage() != null ? e.getMessage() : "正则表达式语法错误",
                            "错误位置: 第" + e.getIndex() + "个字符",
                            "请检查正则语法或使用AI修正功能"
                    ))
                    .build();
        }
    }

    /**
     * 根据自然语言描述由AI生成正则表达式。优先匹配知识库中的标准模式确保一致性，
     * 未命中则调用AI生成并自动验证语法和测试用例准确性，失败时降级到知识库或本地规则。
     *
     * @param request 生成请求DTO，包含自然语言描述和可选的分类信息
     * @return 生成结果DTO
     */
    @Override
    public RegexGenerateResultDTO generate(RegexGenerateRequestDTO request) {
        String description = request.getDescription();
        String category = request.getCategory() != null ? request.getCategory() : "";

        if (knowledgeBaseEnabled) {
            RegexPatternEntry kbMatch = findInKnowledgeBase(description, category);
            if (kbMatch != null) {
                log.info("知识库匹配成功: {}", kbMatch.name);
                return buildFromKnowledgeBase(kbMatch);
            }
        }

        String kbHint = buildKnowledgeBaseHint(description);

        String userPrompt = String.format("""
                请生成满足以下要求的正则表达式：
                
                描述：%s
                %s
                %s
                
                请生成准确的正则表达式，确保：
                1. 正则可以被Java Pattern.compile()成功编译
                2. 测试用例必须准确无误
                3. 返回严格的JSON格式
                """,
                description,
                category.isEmpty() ? "" : "分类：" + category,
                kbHint.isEmpty() ? "" : "参考模式：\n" + kbHint);

        if (request.getTestString() != null && !request.getTestString().isEmpty()) {
            userPrompt += "\n用户提供的测试字符串（正则必须能匹配此字符串）：" + request.getTestString();
        }

        log.info("调用AI生成正则表达式，描述: {}", description);

        try {
            var result = generateService.generateWithPrompt(REGEX_GENERATE_SYSTEM_PROMPT, userPrompt, "regex-generate");
            String content = result.getContent();

            if (content == null || content.trim().isEmpty()) {
                log.warn("AI返回空内容，尝试使用知识库");
                return tryKnowledgeBaseFallback(description);
            }

            if ("fallback".equals(result.getModel()) || content.contains("降级模式")) {
                log.info("AI处于降级模式，使用知识库");
                return tryKnowledgeBaseFallback(description);
            }

            String jsonContent = extractJson(content);
            JsonNode root = objectMapper.readTree(jsonContent);

            String pattern = root.path("pattern").asText();
            String desc = root.path("description").asText();
            String explanation = root.path("explanation").asText();
            double confidence = root.path("confidence").asDouble(0.7);

            List<String> testCases = new ArrayList<>();
            JsonNode testCasesNode = root.path("testCases");
            if (testCasesNode.isArray()) {
                for (JsonNode tc : testCasesNode) {
                    testCases.add(tc.asText());
                }
            }

            List<RegexGenerateResultDTO.AlternativeDTO> alternatives = new ArrayList<>();
            JsonNode alternativesNode = root.path("alternatives");
            if (alternativesNode.isArray()) {
                for (JsonNode alt : alternativesNode) {
                    try {
                        String altPattern = alt.path("pattern").asText();
                        Pattern.compile(altPattern);
                        alternatives.add(RegexGenerateResultDTO.AlternativeDTO.builder()
                                .pattern(altPattern)
                                .description(alt.path("description").asText())
                                .build());
                    } catch (PatternSyntaxException e) {
                        log.warn("替代方案正则语法错误，跳过: {}", alt.path("pattern").asText());
                    }
                }
            }

            try {
                Pattern.compile(pattern);
            } catch (PatternSyntaxException e) {
                log.warn("AI生成的正则语法错误，尝试自动修复或使用知识库: {}", pattern);
                RegexGenerateResultDTO fixed = fixGeneratedPattern(pattern, description, testCases, result.getModel());
                if (fixed.getSuccess()) {
                    return fixed;
                }
                return tryKnowledgeBaseFallback(description);
            }

            if (request.getTestString() != null && !request.getTestString().isEmpty()) {
                try {
                    Pattern compiled = Pattern.compile(pattern);
                    if (!compiled.matcher(request.getTestString()).find()) {
                        log.warn("生成的正则无法匹配用户测试字符串，尝试使用知识库");
                        return tryKnowledgeBaseFallback(description);
                    }
                } catch (Exception e) {
                    log.warn("验证测试字符串失败");
                }
            }

            List<String> validatedTestCases = validateTestCases(pattern, testCases);

            log.info("AI生成正则表达式成功，模式: {}", pattern);

            return RegexGenerateResultDTO.builder()
                    .success(true)
                    .pattern(pattern)
                    .description(desc)
                    .explanation(explanation)
                    .testCases(validatedTestCases)
                    .confidence(confidence)
                    .aiModel(result.getModel())
                    .fallback(false)
                    .errors(Collections.emptyList())
                    .alternatives(alternatives)
                    .build();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI生成正则表达式失败", e);
            return tryKnowledgeBaseFallback(description);
        }
    }

    /**
     * 由AI分析并修正存在问题的正则表达式。若正则语法正确且无修正意图则直接返回，
     * 否则调用AI诊断错误并提供修复方案，自动验证修复后的正则语法有效性。
     *
     * @param request 修复请求DTO，包含原始正则、错误信息和修正意图
     * @return 修复结果DTO
     */
    @Override
    public RegexFixResultDTO fix(RegexFixRequestDTO request) {
        String pattern = request.getPattern();
        String intent = request.getIntent();
        String errorMessage = request.getErrorMessage();

        boolean isSyntaxError = false;
        try {
            Pattern.compile(pattern);
        } catch (PatternSyntaxException e) {
            isSyntaxError = true;
        }

        if (!isSyntaxError && (intent == null || intent.isEmpty())) {
            return RegexFixResultDTO.builder()
                    .success(true)
                    .originalPattern(pattern)
                    .fixedPattern(pattern)
                    .fixDescription("正则表达式语法正确，无需修复")
                    .explanation(describePattern(pattern, 0))
                    .valid(true)
                    .errors(Collections.emptyList())
                    .suggestions(List.of("正则表达式语法正确，可以正常使用"))
                    .build();
        }

        String userPrompt = String.format("""
                请修复以下正则表达式：
                
                原始正则：%s
                %s
                %s
                
                请分析问题并提供准确的修复方案，返回JSON格式结果。
                """,
                pattern,
                errorMessage != null && !errorMessage.isEmpty() ? "错误信息：" + errorMessage : "",
                intent != null && !intent.isEmpty() ? "用户意图：" + intent : "");

        if (request.getTestString() != null && !request.getTestString().isEmpty()) {
            userPrompt += "\n期望匹配的测试字符串：" + request.getTestString();
        }

        log.info("调用AI修复正则表达式，原始模式: {}", pattern);

        try {
            var result = generateService.generateWithPrompt(REGEX_FIX_SYSTEM_PROMPT, userPrompt, "regex-fix");
            String content = result.getContent();

            if (content == null || content.trim().isEmpty()) {
                throw new BusinessException("AI修复失败，未返回有效内容");
            }

            if ("fallback".equals(result.getModel()) || content.contains("降级模式")) {
                log.info("AI处于降级模式，使用本地修复策略");
                return fixFallback(pattern);
            }

            String jsonContent = extractJson(content);
            JsonNode root = objectMapper.readTree(jsonContent);

            String fixedPattern = root.path("fixedPattern").asText();
            String fixDescription = root.path("fixDescription").asText();
            String explanation = root.path("explanation").asText();
            boolean valid = root.path("valid").asBoolean(true);

            List<String> suggestions = new ArrayList<>();
            JsonNode suggestionsNode = root.path("suggestions");
            if (suggestionsNode.isArray()) {
                for (JsonNode s : suggestionsNode) {
                    suggestions.add(s.asText());
                }
            }

            try {
                Pattern.compile(fixedPattern);
                valid = true;
            } catch (PatternSyntaxException e) {
                valid = false;
                log.warn("AI修复后的正则仍有语法错误: {}", fixedPattern);
            }

            List<RegexValidateResultDTO.MatchResultDTO> testMatches = Collections.emptyList();
            if (request.getTestString() != null && !request.getTestString().isEmpty() && valid) {
                try {
                    Pattern compiled = Pattern.compile(fixedPattern);
                    Matcher matcher = compiled.matcher(request.getTestString());
                    testMatches = new ArrayList<>();
                    while (matcher.find()) {
                        List<String> groups = new ArrayList<>();
                        for (int i = 0; i <= matcher.groupCount(); i++) {
                            groups.add(matcher.group(i));
                        }
                        testMatches.add(RegexValidateResultDTO.MatchResultDTO.builder()
                                .matchedText(matcher.group())
                                .startIndex(matcher.start())
                                .endIndex(matcher.end())
                                .groups(groups)
                                .build());
                    }
                } catch (PatternSyntaxException e) {
                    log.warn("测试匹配失败: {}", e.getMessage());
                }
            }

            log.info("AI修复正则表达式成功，修复后模式: {}", fixedPattern);

            return RegexFixResultDTO.builder()
                    .success(true)
                    .originalPattern(pattern)
                    .fixedPattern(fixedPattern)
                    .fixDescription(fixDescription)
                    .explanation(explanation)
                    .valid(valid)
                    .testMatches(testMatches)
                    .aiModel(result.getModel())
                    .fallback(false)
                    .errors(valid ? Collections.emptyList() : List.of("修复后的正则仍存在语法错误"))
                    .suggestions(suggestions)
                    .build();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI修复正则表达式失败", e);
            if (fallbackEnabled) {
                return fixFallback(pattern);
            }
            throw new BusinessException("AI修复正则表达式失败: " + e.getMessage());
        }
    }

    /**
     * 在标准知识库中查找匹配的预定义正则模式。根据描述和分类关键词进行模糊匹配。
     *
     * @param description 自然语言描述
     * @param category    分类关键词
     * @return 匹配的知识库条目，未找到则返回null
     */
    private RegexPatternEntry findInKnowledgeBase(String description, String category) {
        String searchText = (description + " " + category).toLowerCase();

        RegexPatternEntry bestMatch = null;
        int bestScore = 0;

        for (RegexPatternEntry entry : KNOWLEDGE_BASE) {
            int score = 0;
            if (searchText.contains(entry.name.toLowerCase())) {
                score += 10 + entry.name.length();
            }
            for (String keyword : entry.keywords) {
                if (searchText.contains(keyword.toLowerCase())) {
                    score += keyword.length();
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestMatch = entry;
            }
        }

        return bestMatch;
    }

    /**
     * 根据描述构建知识库参考提示，作为AI生成的上下文参考，最多返回3条相关模式。
     *
     * @param description 自然语言描述
     * @return 知识库参考提示字符串
     */
    private String buildKnowledgeBaseHint(String description) {
        StringBuilder hint = new StringBuilder();
        String searchText = description.toLowerCase();
        int count = 0;
        for (RegexPatternEntry entry : KNOWLEDGE_BASE) {
            if (entry.keywords.stream().anyMatch(k -> searchText.contains(k.toLowerCase())) ||
                    searchText.contains(entry.name.toLowerCase())) {
                hint.append("- ").append(entry.name).append(": ").append(entry.pattern).append("\n");
                count++;
                if (count >= 3) break;
            }
        }
        return hint.toString();
    }

    /**
     * 从知识库条目构建生成结果DTO。
     *
     * @param entry 知识库条目
     * @return 构建完成的生成结果DTO
     */
    private RegexGenerateResultDTO buildFromKnowledgeBase(RegexPatternEntry entry) {
        return RegexGenerateResultDTO.builder()
                .success(true)
                .pattern(entry.pattern)
                .description(entry.description)
                .explanation("来自知识库的标准验证模式：" + entry.name)
                .testCases(entry.matchingExamples)
                .confidence(entry.confidence)
                .aiModel("knowledge-base")
                .fallback(false)
                .errors(Collections.emptyList())
                .alternatives(Collections.emptyList())
                .build();
    }

    /**
     * 尝试使用知识库作为降级方案，未命中则继续降级到本地规则。
     *
     * @param description 自然语言描述
     * @return 生成结果DTO
     */
    private RegexGenerateResultDTO tryKnowledgeBaseFallback(String description) {
        RegexPatternEntry kbMatch = findInKnowledgeBase(description, "");
        if (kbMatch != null) {
            return buildFromKnowledgeBase(kbMatch);
        }
        return generateFallback(description);
    }

    /**
     * 对AI生成的测试用例进行实际验证，标注每个用例是否匹配。
     *
     * @param pattern   正则表达式
     * @param testCases 测试用例列表
     * @return 标注了匹配结果的测试用例列表
     */
    private List<String> validateTestCases(String pattern, List<String> testCases) {
        if (testCases.isEmpty()) return testCases;
        try {
            Pattern compiled = Pattern.compile(pattern);
            List<String> validated = new ArrayList<>();
            for (String tc : testCases) {
                boolean matches = compiled.matcher(tc).matches();
                validated.add(tc + (matches ? " ✓" : " ✗"));
            }
            return validated;
        } catch (Exception e) {
            return testCases;
        }
    }

    /**
     * 本地规则降级生成策略，基于常见模式关键词匹配返回预定义正则。
     *
     * @param description 自然语言描述
     * @return 生成结果DTO
     */
    private RegexGenerateResultDTO generateFallback(String description) {
        Map<String, String> commonPatterns = new LinkedHashMap<>();
        commonPatterns.put("邮箱", "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
        commonPatterns.put("手机号", "^1[3-9]\\d{9}$");
        commonPatterns.put("身份证", "^\\d{17}[\\dXx]$");
        commonPatterns.put("URL", "^https?://[\\w\\-]+(\\.[\\w\\-]+)+[/#?]?.*$");
        commonPatterns.put("IP地址", "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$");
        commonPatterns.put("中文", "^[\\u4e00-\\u9fa5]+$");
        commonPatterns.put("数字", "^-?\\d+(\\.\\d+)?$");
        commonPatterns.put("日期", "^\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}$");
        commonPatterns.put("密码", "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@$!%*?&]{8,}$");
        commonPatterns.put("台湾", "^09\\d{8}$");
        commonPatterns.put("香港", "^[569]\\d{7}$");

        for (Map.Entry<String, String> entry : commonPatterns.entrySet()) {
            if (description.toLowerCase().contains(entry.getKey().toLowerCase())) {
                return RegexGenerateResultDTO.builder()
                        .success(true)
                        .pattern(entry.getValue())
                        .description("匹配" + entry.getKey() + "的正则表达式")
                        .explanation("基于本地规则库的简单匹配（降级模式）")
                        .testCases(Collections.emptyList())
                        .confidence(0.6)
                        .aiModel("fallback-rules")
                        .fallback(true)
                        .errors(Collections.emptyList())
                        .alternatives(Collections.emptyList())
                        .build();
            }
        }

        return RegexGenerateResultDTO.builder()
                .success(false)
                .pattern("")
                .description("无法生成正则表达式")
                .errors(List.of("AI服务不可用，且未匹配到标准模式", "请配置OpenAI API Key后重试"))
                .build();
    }

    /**
     * 本地规则降级修复策略，自动修正括号不匹配、无效转义等常见语法问题。
     *
     * @param pattern 原始正则表达式
     * @return 修复结果DTO
     */
    private RegexFixResultDTO fixFallback(String pattern) {
        String fixed = pattern;
        List<String> fixes = new ArrayList<>();

        fixed = fixed.replaceAll("\\\\([^\\\\nrtbfduxDsSsWw.*+?^${}()|\\[\\]])", "$1");
        if (!fixed.equals(pattern)) {
            fixes.add("移除了无效的转义字符");
        }

        String prev = fixed;
        fixed = fixed.replaceAll("\\[(\\^[^\\]]*)\\[(\\])", "[$1$2");
        if (!fixed.equals(prev)) {
            fixes.add("修复了字符类中的嵌套方括号");
        }

        int open = countChar(fixed, '(');
        int close = countChar(fixed, ')');
        while (close < open) {
            fixed += ")";
            close++;
            fixes.add("补充了缺失的右括号");
        }

        open = countChar(fixed, '[');
        close = countChar(fixed, ']');
        while (close < open) {
            fixed += "]";
            close++;
            fixes.add("补充了缺失的右方括号");
        }

        try {
            Pattern.compile(fixed);
            return RegexFixResultDTO.builder()
                    .success(true)
                    .originalPattern(pattern)
                    .fixedPattern(fixed)
                    .fixDescription(fixes.isEmpty() ? "本地规则修复" : "本地规则修复：" + String.join("；", fixes))
                    .explanation("基于本地规则的简单修复（降级模式）")
                    .valid(true)
                    .aiModel("fallback-rules")
                    .fallback(true)
                    .errors(Collections.emptyList())
                    .suggestions(List.of("建议配置AI服务以获得更精确的修复"))
                    .build();
        } catch (PatternSyntaxException e) {
            return RegexFixResultDTO.builder()
                    .success(false)
                    .originalPattern(pattern)
                    .fixedPattern(fixed)
                    .fixDescription("本地修复未能完全修复正则表达式")
                    .valid(false)
                    .aiModel("fallback-rules")
                    .fallback(true)
                    .errors(List.of("修复后仍存在语法错误: " + e.getMessage(), "建议配置AI服务以获得更精确的修复"))
                    .suggestions(Collections.emptyList())
                    .build();
        }
    }

    /**
     * 尝试自动修复AI生成但存在语法错误的正则，主要修正括号不匹配问题。
     *
     * @param pattern    原始正则表达式
     * @param description 描述信息
     * @param testCases  测试用例列表
     * @param model      AI模型标识
     * @return 修复后的生成结果DTO
     */
    private RegexGenerateResultDTO fixGeneratedPattern(String pattern, String description, List<String> testCases, String model) {
        String fixed = pattern;

        int open = countChar(fixed, '(');
        int close = countChar(fixed, ')');
        while (close < open) {
            fixed += ")";
            close++;
        }

        open = countChar(fixed, '[');
        close = countChar(fixed, ']');
        while (close < open) {
            fixed += "]";
            close++;
        }

        try {
            Pattern.compile(fixed);
            return RegexGenerateResultDTO.builder()
                    .success(true)
                    .pattern(fixed)
                    .description(description)
                    .explanation("AI生成的正则已自动修正括号匹配")
                    .testCases(testCases)
                    .confidence(0.6)
                    .aiModel(model + "+fix")
                    .fallback(false)
                    .errors(Collections.emptyList())
                    .alternatives(Collections.emptyList())
                    .build();
        } catch (PatternSyntaxException e) {
            return RegexGenerateResultDTO.builder()
                    .success(false)
                    .pattern(pattern)
                    .description(description)
                    .errors(List.of("AI生成的正则存在语法错误，自动修复失败", e.getMessage()))
                    .build();
        }
    }

    /**
     * 根据正则表达式特征和标志位生成中文描述信息。
     *
     * @param pattern 正则表达式字符串
     * @param flags   正则标志位
     * @return 中文描述信息
     */
    private String describePattern(String pattern, int flags) {
        StringBuilder desc = new StringBuilder();
        if ((flags & Pattern.CASE_INSENSITIVE) != 0) desc.append("忽略大小写；");
        if ((flags & Pattern.MULTILINE) != 0) desc.append("多行模式；");
        if ((flags & Pattern.DOTALL) != 0) desc.append("点号匹配换行；");

        if (pattern.startsWith("^")) desc.append("从开头匹配；");
        if (pattern.endsWith("$")) desc.append("匹配到结尾；");
        if (pattern.contains("\\d")) desc.append("包含数字匹配；");
        if (pattern.contains("\\w")) desc.append("包含单词字符匹配；");
        if (pattern.contains("\\s")) desc.append("包含空白字符匹配；");
        if (pattern.contains("[\\u4e00-\\u9fa5]")) desc.append("包含中文字符匹配；");

        return desc.length() > 0 ? desc.toString() : "自定义正则表达式";
    }

    /**
     * 从正则表达式中提取命名捕获组的名称列表。
     *
     * @param pattern 正则表达式字符串
     * @return 命名捕获组名称列表
     */
    private List<String> extractGroupNames(String pattern) {
        List<String> groups = new ArrayList<>();
        java.util.regex.Matcher nameMatcher = java.util.regex.Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>").matcher(pattern);
        while (nameMatcher.find()) {
            groups.add(nameMatcher.group(1));
        }
        return groups;
    }

    /**
     * 统计正则表达式中指定字符的未转义出现次数，忽略字符类内部和转义后的字符。
     *
     * @param str 正则表达式字符串
     * @param c   要统计的字符
     * @return 未转义的出现次数
     */
    private int countChar(String str, char c) {
        int count = 0;
        boolean inCharClass = false;
        boolean escape = false;
        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);
            if (escape) {
                escape = false;
                continue;
            }
            if (ch == '\\') {
                escape = true;
                continue;
            }
            if (!inCharClass && ch == c) count++;
            if (ch == '[') inCharClass = true;
            if (ch == ']') inCharClass = false;
        }
        return count;
    }

    /**
     * 从AI返回内容中提取JSON字符串，移除markdown代码块标记。
     *
     * @param content AI返回的原始内容
     * @return 提取出的JSON字符串
     */
    private String extractJson(String content) {
        String trimmed = content.trim();

        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        trimmed = trimmed.trim();

        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }

        return trimmed;
    }

    /**
     * 知识库正则模式条目，存储预定义的正则表达式及其元数据。
     */
    private static class RegexPatternEntry {
        String name;
        String description;
        String pattern;
        List<String> matchingExamples;
        List<String> nonMatchingExamples;
        double confidence;
        List<String> keywords;

        RegexPatternEntry(String name, String description, String pattern,
                          List<String> matchingExamples, List<String> nonMatchingExamples,
                          double confidence) {
            this.name = name;
            this.description = description;
            this.pattern = pattern;
            this.matchingExamples = matchingExamples;
            this.nonMatchingExamples = nonMatchingExamples;
            this.confidence = confidence;
            this.keywords = buildKeywords(name);
        }

    /**
     * 根据模式名称构建搜索关键词列表，用于知识库模糊匹配。
     *
     * @param name 模式名称
     * @return 搜索关键词列表
     */
        private List<String> buildKeywords(String name) {
            List<String> result = new ArrayList<>();
            result.add(name);
            if (name.contains("手机")) result.add("手机");
            if (name.contains("电话")) result.add("电话");
            if (name.contains("大陆")) result.add("大陆");
            if (name.contains("台湾")) result.add("台湾");
            if (name.contains("香港")) result.add("香港");
            if (name.contains("邮箱")) result.add("邮箱");
            if (name.contains("邮件")) result.add("邮件");
            if (name.contains("身份证")) result.add("身份");
            if (name.contains("URL")) {
                result.add("网址");
                result.add("链接");
            }
            if (name.contains("IP")) result.add("ip");
            if (name.contains("中文")) result.add("汉字");
            if (name.contains("数字")) result.add("数值");
            if (name.contains("日期")) result.add("时间");
            if (name.contains("密码")) result.add("口令");
            if (name.contains("HTML")) result.add("标签");
            if (name.contains("车牌")) result.add("车辆");
            return result;
        }
    }
}
