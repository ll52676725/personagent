package com.example.agentplatform.tools.service.impl;

import com.example.agentplatform.agent.article.service.GenerateService;
import com.example.agentplatform.tools.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * RegexServiceImpl 单元测试
 * <p>
 * 覆盖校验、知识库匹配、降级策略等核心逻辑，
 * AI相关功能通过Mock GenerateService进行测试。
 */
@ExtendWith(MockitoExtension.class)
class RegexServiceImplTest {

    @Mock
    private GenerateService generateService;

    private RegexServiceImpl regexService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        regexService = new RegexServiceImpl(objectMapper, generateService);
        ReflectionTestUtils.setField(regexService, "fallbackEnabled", true);
        ReflectionTestUtils.setField(regexService, "knowledgeBaseEnabled", true);
    }

    // ==================== 校验测试 ====================

    @Nested
    @DisplayName("validate - 正则表达式校验")
    class ValidateTests {

        @Test
        @DisplayName("有效正则 + 无测试字符串 → 返回语法有效")
        void validate_validPatternNoTestString_returnsValid() {
            RegexValidateRequestDTO request = new RegexValidateRequestDTO();
            request.setPattern("^\\d+$");
            request.setTestString(null);

            RegexValidateResultDTO result = regexService.validate(request);

            assertTrue(result.getSuccess());
            assertTrue(result.getValid());
            assertEquals(0, result.getMatchCount());
            assertTrue(result.getMatches().isEmpty());
        }

        @Test
        @DisplayName("有效正则 + 匹配的测试字符串 → 返回匹配结果")
        void validate_validPatternWithMatch_returnsMatches() {
            RegexValidateRequestDTO request = new RegexValidateRequestDTO();
            request.setPattern("\\d+");
            request.setTestString("abc123def456");

            RegexValidateResultDTO result = regexService.validate(request);

            assertTrue(result.getSuccess());
            assertTrue(result.getValid());
            assertEquals(2, result.getMatchCount());
            assertEquals("123", result.getMatches().get(0).getMatchedText());
            assertEquals("456", result.getMatches().get(1).getMatchedText());
            assertEquals(3, result.getMatches().get(0).getStartIndex());
            assertEquals(6, result.getMatches().get(0).getEndIndex());
        }

        @Test
        @DisplayName("有效正则 + 不匹配的测试字符串 → 匹配数量为0")
        void validate_validPatternNoMatch_returnsZeroMatches() {
            RegexValidateRequestDTO request = new RegexValidateRequestDTO();
            request.setPattern("^\\d+$");
            request.setTestString("abc");

            RegexValidateResultDTO result = regexService.validate(request);

            assertTrue(result.getSuccess());
            assertTrue(result.getValid());
            assertEquals(0, result.getMatchCount());
        }

        @Test
        @DisplayName("无效正则语法 → 返回语法错误")
        void validate_invalidPatternSyntax_returnsError() {
            RegexValidateRequestDTO request = new RegexValidateRequestDTO();
            request.setPattern("[invalid");

            RegexValidateResultDTO result = regexService.validate(request);

            assertFalse(result.getSuccess());
            assertFalse(result.getValid());
            assertNotNull(result.getErrors());
            assertFalse(result.getErrors().isEmpty());
        }

        @Test
        @DisplayName("正则匹配包含捕获组 → 正确提取组信息")
        void validate_patternWithGroups_extractsGroupInfo() {
            RegexValidateRequestDTO request = new RegexValidateRequestDTO();
            request.setPattern("(\\d+)-(\\d+)");
            request.setTestString("123-456");

            RegexValidateResultDTO result = regexService.validate(request);

            assertTrue(result.getValid());
            assertEquals(1, result.getMatchCount());
            assertEquals(3, result.getMatches().get(0).getGroups().size());
            assertEquals("123-456", result.getMatches().get(0).getGroups().get(0));
            assertEquals("123", result.getMatches().get(0).getGroups().get(1));
            assertEquals("456", result.getMatches().get(0).getGroups().get(2));
        }

        @Test
        @DisplayName("命名捕获组 → 正确提取组名称")
        void validate_namedGroups_extractsGroupNames() {
            RegexValidateRequestDTO request = new RegexValidateRequestDTO();
            request.setPattern("(?<year>\\d{4})-(?<month>\\d{2})");
            request.setTestString("2024-01");

            RegexValidateResultDTO result = regexService.validate(request);

            assertTrue(result.getValid());
            assertNotNull(result.getGroups());
            assertTrue(result.getGroups().contains("year"));
            assertTrue(result.getGroups().contains("month"));
        }

        @Test
        @DisplayName("带flags的正则 → 正确处理标志位")
        void validate_withFlags_appliesFlagsCorrectly() {
            RegexValidateRequestDTO request = new RegexValidateRequestDTO();
            request.setPattern("^abc$");
            request.setTestString("ABC");
            request.setFlags(2); // Pattern.CASE_INSENSITIVE

            RegexValidateResultDTO result = regexService.validate(request);

            assertTrue(result.getValid());
            assertEquals(1, result.getMatchCount());
        }
    }

    // ==================== 知识库生成测试 ====================

    @Nested
    @DisplayName("generate - 知识库匹配生成")
    class GenerateKnowledgeBaseTests {

        @Test
        @DisplayName("匹配中国大陆手机号 → 返回知识库标准模式")
        void generate_chinaMainlandPhone_returnsKnowledgeBasePattern() {
            RegexGenerateRequestDTO request = new RegexGenerateRequestDTO();
            request.setDescription("中国大陆手机号");

            RegexGenerateResultDTO result = regexService.generate(request);

            assertTrue(result.getSuccess());
            assertEquals("^1[3-9]\\d{9}$", result.getPattern());
            assertEquals("knowledge-base", result.getAiModel());
            assertFalse(result.getFallback());
            assertTrue(result.getConfidence() >= 0.9);
        }

        @Test
        @DisplayName("匹配台湾手机号 → 返回正确的台湾手机号正则")
        void generate_taiwanPhone_returnsCorrectPattern() {
            RegexGenerateRequestDTO request = new RegexGenerateRequestDTO();
            request.setDescription("台湾手机号");

            RegexGenerateResultDTO result = regexService.generate(request);

            assertTrue(result.getSuccess());
            assertEquals("^09\\d{8}$", result.getPattern());
            assertEquals("knowledge-base", result.getAiModel());

            Pattern compiled = Pattern.compile(result.getPattern());
            assertTrue(compiled.matcher("0912345678").matches());
            assertFalse(compiled.matcher("13812345678").matches());
            assertFalse(compiled.matcher("091234567").matches());
            assertFalse(compiled.matcher("09123456789").matches());
        }

        @Test
        @DisplayName("匹配香港手机号 → 返回正确的香港手机号正则")
        void generate_hongKongPhone_returnsCorrectPattern() {
            RegexGenerateRequestDTO request = new RegexGenerateRequestDTO();
            request.setDescription("香港手机号");

            RegexGenerateResultDTO result = regexService.generate(request);

            assertTrue(result.getSuccess());
            assertEquals("^[569]\\d{7}$", result.getPattern());

            Pattern compiled = Pattern.compile(result.getPattern());
            assertTrue(compiled.matcher("91234567").matches());
            assertTrue(compiled.matcher("51234567").matches());
            assertFalse(compiled.matcher("12345678").matches());
        }

        @Test
        @DisplayName("匹配邮箱 → 返回知识库标准模式")
        void generate_email_returnsKnowledgeBasePattern() {
            RegexGenerateRequestDTO request = new RegexGenerateRequestDTO();
            request.setDescription("邮箱地址");

            RegexGenerateResultDTO result = regexService.generate(request);

            assertTrue(result.getSuccess());
            assertNotNull(result.getPattern());
            assertTrue(result.getPattern().contains("@"));
            assertEquals("knowledge-base", result.getAiModel());
        }

        @Test
        @DisplayName("知识库禁用 → 不使用知识库，尝试调用AI")
        void generate_knowledgeBaseDisabled_callsAIInstead() throws Exception {
            ReflectionTestUtils.setField(regexService, "knowledgeBaseEnabled", false);

            RegexGenerateRequestDTO request = new RegexGenerateRequestDTO();
            request.setDescription("中国大陆手机号");

            var aiResult = com.example.agentplatform.agent.article.dto.GenerateResult.builder()
                    .content("{\"pattern\":\"^1[3-9]\\\\d{9}$\",\"description\":\"中国大陆手机号\",\"explanation\":\"1开头11位\",\"testCases\":[\"13812345678\"],\"confidence\":0.9}")
                    .model("gpt-4")
                    .build();

            when(generateService.generateWithPrompt(anyString(), anyString(), anyString()))
                    .thenReturn(aiResult);

            RegexGenerateResultDTO result = regexService.generate(request);

            assertTrue(result.getSuccess());
            assertNotNull(result.getPattern());
        }

        @Test
        @DisplayName("知识库模式包含测试用例")
        void generate_knowledgeBasePattern_containsTestCases() {
            RegexGenerateRequestDTO request = new RegexGenerateRequestDTO();
            request.setDescription("身份证号");

            RegexGenerateResultDTO result = regexService.generate(request);

            assertTrue(result.getSuccess());
            assertNotNull(result.getTestCases());
            assertFalse(result.getTestCases().isEmpty());
        }
    }

    // ==================== 降级策略测试 ====================

    @Nested
    @DisplayName("generate - 降级策略")
    class GenerateFallbackTests {

        @Test
        @DisplayName("AI失败 + 知识库不匹配 + 描述含台湾 → 知识库直接命中台湾模式")
        void generate_aiFallsBack_taiwanLocalRule() {
            RegexGenerateRequestDTO request = new RegexGenerateRequestDTO();
            request.setDescription("台湾手机号");

            RegexGenerateResultDTO result = regexService.generate(request);

            assertTrue(result.getSuccess());
            assertEquals("^09\\d{8}$", result.getPattern());
            assertEquals("knowledge-base", result.getAiModel());
        }

        @Test
        @DisplayName("AI失败 + 知识库不匹配 + 无已知关键词 → 返回失败")
        void generate_aiFails_noKnowledgeBaseMatch_returnsFailure() {
            RegexGenerateRequestDTO request = new RegexGenerateRequestDTO();
            request.setDescription("某种未知的特殊格式xyz");

            when(generateService.generateWithPrompt(anyString(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("AI unavailable"));

            RegexGenerateResultDTO result = regexService.generate(request);

            assertFalse(result.getSuccess());
            assertNotNull(result.getErrors());
            assertFalse(result.getErrors().isEmpty());
        }
    }

    // ==================== 修正测试 ====================

    @Nested
    @DisplayName("fix - 正则表达式修正")
    class FixTests {

        @Test
        @DisplayName("语法正确 + 无修正意图 → 返回无需修复")
        void fix_validPatternNoIntent_returnsNoFixNeeded() {
            RegexFixRequestDTO request = new RegexFixRequestDTO();
            request.setPattern("^\\d+$");
            request.setIntent(null);

            RegexFixResultDTO result = regexService.fix(request);

            assertTrue(result.getSuccess());
            assertTrue(result.getValid());
            assertEquals(request.getPattern(), result.getFixedPattern());
        }

        @Test
        @DisplayName("语法正确 + 有修正意图 → 调用AI修正")
        void fix_validPatternWithIntent_callsAI() throws Exception {
            RegexFixRequestDTO request = new RegexFixRequestDTO();
            request.setPattern("^\\d$");
            request.setIntent("需要匹配1到3位数字");

            var generateResult = com.example.agentplatform.agent.article.dto.GenerateResult.builder()
                    .content("{\"fixedPattern\":\"^\\\\d{1,3}$\",\"fixDescription\":\"修改量词\",\"explanation\":\"匹配1-3位数字\",\"suggestions\":[\"建议1\"],\"valid\":true}")
                    .model("gpt-4")
                    .build();

            when(generateService.generateWithPrompt(anyString(), anyString(), anyString()))
                    .thenReturn(generateResult);

            RegexFixResultDTO result = regexService.fix(request);

            assertTrue(result.getSuccess());
            assertEquals("^\\d{1,3}$", result.getFixedPattern());
            assertTrue(result.getValid());
        }

        @Test
        @DisplayName("语法错误 + AI不可用 → 本地降级修复括号")
        void fix_syntaxError_aiUnavailable_fallbackFixesBrackets() {
            RegexFixRequestDTO request = new RegexFixRequestDTO();
            request.setPattern("(abc");

            when(generateService.generateWithPrompt(anyString(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("AI unavailable"));

            RegexFixResultDTO result = regexService.fix(request);

            assertTrue(result.getSuccess());
            assertTrue(result.getValid());
            assertTrue(result.getFallback());
            assertEquals("(abc)", result.getFixedPattern());
        }

        @Test
        @DisplayName("降级修复 → 补充缺失的右方括号")
        void fix_fallbackFixes_missingClosingBracket() {
            RegexFixRequestDTO request = new RegexFixRequestDTO();
            request.setPattern("[a-z");

            when(generateService.generateWithPrompt(anyString(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("AI unavailable"));

            RegexFixResultDTO result = regexService.fix(request);

            assertTrue(result.getValid());
            assertEquals("[a-z]", result.getFixedPattern());
        }
    }

    // ==================== 知识库模式验证测试 ====================

    @Nested
    @DisplayName("知识库模式 - 实际正则验证")
    class KnowledgeBasePatternVerificationTests {

        @Test
        @DisplayName("中国大陆手机号正则 - 匹配示例验证")
        void verify_chinaPhonePattern_matchingExamples() {
            RegexGenerateRequestDTO request = new RegexGenerateRequestDTO();
            request.setDescription("中国大陆手机号");
            RegexGenerateResultDTO result = regexService.generate(request);

            Pattern pattern = Pattern.compile(result.getPattern());
            assertTrue(pattern.matcher("13812345678").matches());
            assertTrue(pattern.matcher("19999999999").matches());
            assertFalse(pattern.matcher("12345678901").matches());
            assertFalse(pattern.matcher("1381234567").matches());
        }

        @Test
        @DisplayName("台湾手机号正则 - 完整验证")
        void verify_taiwanPhonePattern_fullValidation() {
            RegexGenerateRequestDTO request = new RegexGenerateRequestDTO();
            request.setDescription("台湾手机号");
            RegexGenerateResultDTO result = regexService.generate(request);

            Pattern pattern = Pattern.compile(result.getPattern());
            assertTrue(pattern.matcher("0912345678").matches(), "09开头10位应匹配");
            assertTrue(pattern.matcher("0987654321").matches(), "09开头10位应匹配");
            assertFalse(pattern.matcher("912345678").matches(), "非09开头不应匹配");
            assertFalse(pattern.matcher("091234567").matches(), "9位不应匹配");
            assertFalse(pattern.matcher("09123456789").matches(), "11位不应匹配");
        }

        @Test
        @DisplayName("邮箱正则 - 完整验证")
        void verify_emailPattern_fullValidation() {
            RegexGenerateRequestDTO request = new RegexGenerateRequestDTO();
            request.setDescription("邮箱地址");
            RegexGenerateResultDTO result = regexService.generate(request);

            Pattern pattern = Pattern.compile(result.getPattern());
            assertTrue(pattern.matcher("test@example.com").matches());
            assertTrue(pattern.matcher("user.name@domain.org").matches());
            assertFalse(pattern.matcher("test@").matches());
            assertFalse(pattern.matcher("@example.com").matches());
        }

        @Test
        @DisplayName("IPv4正则 - 完整验证")
        void verify_ipv4Pattern_fullValidation() {
            RegexGenerateRequestDTO request = new RegexGenerateRequestDTO();
            request.setDescription("IP地址");
            RegexGenerateResultDTO result = regexService.generate(request);

            Pattern pattern = Pattern.compile(result.getPattern());
            assertTrue(pattern.matcher("192.168.1.1").matches());
            assertTrue(pattern.matcher("0.0.0.0").matches());
            assertTrue(pattern.matcher("255.255.255.255").matches());
            assertFalse(pattern.matcher("256.1.1.1").matches());
        }

        @Test
        @DisplayName("身份证号正则 - 完整验证")
        void verify_idCardPattern_fullValidation() {
            RegexGenerateRequestDTO request = new RegexGenerateRequestDTO();
            request.setDescription("身份证号");
            RegexGenerateResultDTO result = regexService.generate(request);

            Pattern pattern = Pattern.compile(result.getPattern());
            assertTrue(pattern.matcher("110101199001011234").matches());
            assertTrue(pattern.matcher("31010119901231567X").matches());
            assertTrue(pattern.matcher("31010119901231567x").matches());
            assertFalse(pattern.matcher("11010119900101123").matches());
        }

        @Test
        @DisplayName("数字正则 - 完整验证")
        void verify_numberPattern_fullValidation() {
            RegexGenerateRequestDTO request = new RegexGenerateRequestDTO();
            request.setDescription("数字");
            RegexGenerateResultDTO result = regexService.generate(request);

            Pattern pattern = Pattern.compile(result.getPattern());
            assertTrue(pattern.matcher("123").matches());
            assertTrue(pattern.matcher("-456").matches());
            assertTrue(pattern.matcher("3.14").matches());
            assertFalse(pattern.matcher("12a3").matches());
        }

        @Test
        @DisplayName("车牌号正则 - 完整验证")
        void verify_licensePlatePattern_fullValidation() {
            RegexGenerateRequestDTO request = new RegexGenerateRequestDTO();
            request.setDescription("车牌号");
            RegexGenerateResultDTO result = regexService.generate(request);

            Pattern pattern = Pattern.compile(result.getPattern());
            assertTrue(pattern.matcher("京A12345").matches());
            assertTrue(pattern.matcher("沪B88888").matches());
            assertFalse(pattern.matcher("京12345").matches());
        }
    }
}
