package com.example.agentplatform.tools.service;

import com.example.agentplatform.tools.dto.ImageModerationRequestDTO;
import com.example.agentplatform.tools.dto.ImageModerationResultDTO;
import com.example.agentplatform.tools.dto.ModerationCategoryDetailDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 图片内容检测服务类
 * <p>提供图片内容安全检测功能，包括涉黄、涉政、涉爆、其他违规等多个维度
 * <p>核心功能：
 * <ul>
 *   <li>图片内容审核 - 使用多模态AI模型分析图片内容</li>
 *   <li>涉黄检测 - 检测色情、低俗、性感暴露等内容</li>
 *   <li>涉政检测 - 检测政治敏感人物、敏感事件、违禁标志等</li>
 *   <li>涉爆检测 - 检测暴力、血腥、恐怖、爆炸物、武器等内容</li>
 *   <li>其他违规检测 - 检测赌博、毒品、烟酒广告等内容</li>
 *   <li>AI智能分析 - 基于Spring AI调用大语言模型进行内容理解</li>
 *   <li>降级策略 - 当AI服务不可用时自动切换到本地规则引擎</li>
 * </ul>
 * 
 * @author System
 * @since 2025-06-02
 * @see com.example.agentplatform.tools.controller.ImageModerationController
 * @see ImageModerationResultDTO
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageModerationService {

    /**
     * 统一AI服务，用于调用大语言模型进行智能分析
     */
    private final ToolAIService toolAIService;

    /**
     * JSON对象映射器，用于序列化和反序列化数据
     */
    private final ObjectMapper objectMapper;

    /**
     * 免责声明文本，提示用户检测结果仅供参考
     */
    private static final String DISCLAIMER = """
        免责声明：
        本工具的图片内容检测结果仅供参考，不能作为最终审核依据。
        AI检测可能存在误判和漏判，建议对可疑内容进行人工复核。
        本工具不对检测结果的准确性、完整性、可靠性做任何保证。
        对于因使用本工具检测结果导致的任何问题，本工具不承担任何责任。
        请遵守相关法律法规，不要上传和传播违规内容。
        """;

    /**
     * 检测分类与中文标签映射表
     * <p>key为分类编码，value为前端显示的中文标签
     */
    private static final Map<String, String> CATEGORY_LABEL_MAP = new LinkedHashMap<>();

    /**
     * 风险等级排序权重映射表
     * <p>用于计算总体风险等级，数值越大风险越高
     */
    private static final Map<String, Integer> RISK_LEVEL_ORDER = new HashMap<>();

    /**
     * 静态初始化块，初始化分类映射表和风险等级权重
     * <p>在类加载时执行一次，配置所有分类的中文标签
     */
    static {
        CATEGORY_LABEL_MAP.put("pornography", "涉黄");
        CATEGORY_LABEL_MAP.put("political", "涉政");
        CATEGORY_LABEL_MAP.put("violence", "涉爆");
        CATEGORY_LABEL_MAP.put("other", "其他违规");

        RISK_LEVEL_ORDER.put("safe", 0);
        RISK_LEVEL_ORDER.put("low", 1);
        RISK_LEVEL_ORDER.put("medium", 2);
        RISK_LEVEL_ORDER.put("high", 3);
    }

    /**
     * AI图片内容检测系统提示词
     * <p>定义AI角色、任务、检测维度、风险判定标准和输出格式规范
     * <p>要求AI以专业内容安全审核专家的身份进行分析，返回严格的JSON格式
     */
    private static final String AI_MODERATION_SYSTEM_PROMPT = """
        你是一个专业的内容安全审核专家，拥有10年以上的图片内容审核经验。
        你精通各类违规内容的识别标准，能够准确判断图片中的涉黄、涉政、涉暴等违规内容。
        
        你的任务：
        1. 仔细分析用户上传的图片内容，从多个维度进行安全检测
        2. 对每个检测维度给出明确的判定结果和置信度
        3. 标注检测到的违规标签和具体描述
        4. 给出总体审核结论和处理建议
        
        检测维度：
        1. 涉黄检测（pornography）：
           - 色情内容：裸体、性行为、色情暗示
           - 低俗内容：衣着过于暴露、性感挑逗姿势
           - 成人用品、色情广告
           
        2. 涉政检测（political）：
           - 政治敏感人物形象
           - 敏感事件、敏感标志、违禁符号
           - 不当政治言论、标语、横幅
           - 分裂国家、破坏民族团结的内容
           
        3. 涉爆/暴力检测（violence）：
           - 血腥暴力：伤口、血迹、尸体
           - 恐怖内容：恐怖分子、恐怖组织标志
           - 武器弹药：枪支、刀具、爆炸物
           - 暴力行为：殴打、虐待、自残
           - 危险场景：火灾、爆炸、车祸
           
        4. 其他违规检测（other）：
           - 赌博内容：赌场、赌具、赌博广告
           - 毒品内容：毒品、吸毒工具、贩毒场景
           - 烟酒广告：香烟、酒类促销广告
           - 其他违法违规内容
        
        风险等级判定标准：
        - high（高危）：明显违规内容，必须拦截
        - medium（中危）：疑似违规内容，需要人工复审
        - low（低危）：轻微敏感，建议关注
        - safe（安全）：正常内容，审核通过
        
        置信度说明：
        - 0-50：不确定，建议复审
        - 51-80：比较确定
        - 81-100：非常确定
        
        输出要求：
        请严格按照以下JSON格式返回，不要有任何其他文字说明：
        {
          "success": true,
          "conclusion": "pass/review/block",
          "conclusionLabel": "通过/待复审/拦截",
          "hasViolation": false,
          "overallRiskLevel": "safe/low/medium/high",
          "maxConfidence": 0,
          "pornography": {
            "category": "pornography",
            "categoryLabel": "涉黄",
            "violated": false,
            "confidence": 0,
            "riskLevel": "safe",
            "labels": [],
            "description": ""
          },
          "political": {
            "category": "political",
            "categoryLabel": "涉政",
            "violated": false,
            "confidence": 0,
            "riskLevel": "safe",
            "labels": [],
            "description": ""
          },
          "violence": {
            "category": "violence",
            "categoryLabel": "涉爆",
            "violated": false,
            "confidence": 0,
            "riskLevel": "safe",
            "labels": [],
            "description": ""
          },
          "other": {
            "category": "other",
            "categoryLabel": "其他违规",
            "violated": false,
            "confidence": 0,
            "riskLevel": "safe",
            "labels": [],
            "description": ""
          },
          "allViolationLabels": [],
          "suggestion": "审核建议",
          "auditNote": "详细说明"
        }
        
        注意事项：
        - 对于正常的生活照片、风景照片、证件照片等，判定为安全
        - 对于艺术作品、医疗教育图片中的裸露，应结合上下文判断
        - 对于玩具枪、仿真武器，要与真枪区别对待
        - 对于新闻报道中的暴力场景，应考虑是否为合理使用
        - 如果无法确定，置信度设为较低值，建议人工复审
        """;

    /**
     * 执行图片内容检测
     * <p>首先验证图片数据，然后调用AI进行内容分析，最后整理检测结果返回
     * <p>当AI服务不可用时，自动切换到降级模式（本地规则引擎）
     * 
     * @param request 检测请求DTO，包含图片数据和检测配置
     * @return 检测结果DTO，包含各分类检测详情和总体结论
     */
    public ImageModerationResultDTO moderateImage(ImageModerationRequestDTO request) {
        long startTime = System.currentTimeMillis();
        String taskId = UUID.randomUUID().toString();
        log.info("【图片内容检测】开始执行图片内容检测, 任务ID: {}, 文件名: {}, 文件大小: {}KB",
                taskId, request.getFileName(), request.getFileSize() != null ? request.getFileSize() / 1024 : 0);

        try {
            log.debug("【图片内容检测】验证图片数据");
            int[] dimensions = validateAndGetImageDimensions(request.getImageBase64());
            int width = dimensions[0];
            int height = dimensions[1];
            log.debug("【图片内容检测】图片验证通过, 尺寸: {}x{}", width, height);

            log.debug("【图片内容检测】调用AI进行内容分析...");
            ToolAIService.AIResponse<ImageModerationResultDTO> response = analyzeWithAI(request);

            ImageModerationResultDTO result = response.getData();
            result.setTaskId(taskId);
            result.setFileName(request.getFileName());
            result.setFileSize(request.getFileSize());
            result.setMimeType(request.getMimeType());
            result.setWidth(width);
            result.setHeight(height);
            result.setModel(response.getModel());
            result.setTokens(response.getTokens());
            result.setFallback(response.isFallback());

            long duration = System.currentTimeMillis() - startTime;
            result.setDetectionDurationMs(duration);
            result.setDetectionTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            result.setDisclaimer(DISCLAIMER);

            log.info("【图片内容检测】检测完成, 任务ID: {}, 耗时: {}ms, 结论: {}, 是否违规: {}, 最高置信度: {}",
                    taskId, duration, result.getConclusionLabel(), result.getHasViolation(), result.getMaxConfidence());

            if (result.getHasViolation()) {
                log.warn("【图片内容检测】检测到违规内容, 任务ID: {}, 违规标签: {}, 总体风险: {}",
                        taskId, result.getAllViolationLabels(), result.getOverallRiskLevel());
            }

            return result;

        } catch (IllegalArgumentException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("【图片内容检测】图片数据无效, 任务ID: {}, 耗时: {}ms, 错误: {}", taskId, duration, e.getMessage());
            return buildErrorResult(taskId, request, "图片数据无效: " + e.getMessage(), duration);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("【图片内容检测】检测失败, 任务ID: {}, 耗时: {}ms, 错误: {}", taskId, duration, e.getMessage(), e);
            return buildErrorResult(taskId, request, "检测失败: " + e.getMessage(), duration);
        }
    }

    /**
     * 调用AI进行图片内容分析
     * <p>使用多模态AI接口，将图片作为Media对象发送，AI模型可真正"看到"图片内容
     * <p>配置降级策略，AI失败时使用本地规则引擎生成结果
     * 
     * @param request 检测请求DTO
     * @return AI分析响应包装对象
     */
    private ToolAIService.AIResponse<ImageModerationResultDTO> analyzeWithAI(ImageModerationRequestDTO request) {
        String userPrompt = buildUserPrompt(request);

        return toolAIService.analyzeWithImage(
                AI_MODERATION_SYSTEM_PROMPT,
                userPrompt,
                request.getImageBase64(),
                request.getMimeType(),
                root -> parseAIResponse(root, request),
                () -> fallbackImageModeration(request)
        );
    }

    /**
     * 构建用户提示词
     * <p>仅包含文字描述和检测配置，不再将Base64数据嵌入文本
     * <p>图片数据通过多模态Media对象单独传递，AI模型可直接"看到"图片
     * 
     * @param request 检测请求DTO
     * @return 格式化的用户提示词
     */
    private String buildUserPrompt(ImageModerationRequestDTO request) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请分析附带的图片内容，执行内容安全检测。\n\n");
        prompt.append("图片信息：\n");
        prompt.append("- 文件名：").append(request.getFileName()).append("\n");
        prompt.append("- 文件类型：").append(request.getMimeType()).append("\n");
        if (request.getFileSize() != null) {
            prompt.append("- 文件大小：").append(request.getFileSize() / 1024).append(" KB\n");
        }

        prompt.append("\n检测配置：\n");
        prompt.append("- 涉黄检测：").append(request.getDetectPornography() ? "开启" : "关闭").append("\n");
        prompt.append("- 涉政检测：").append(request.getDetectPolitical() ? "开启" : "关闭").append("\n");
        prompt.append("- 涉爆检测：").append(request.getDetectViolence() ? "开启" : "关闭").append("\n");
        prompt.append("- 其他违规检测：").append(request.getDetectOther() ? "开启" : "关闭").append("\n");
        prompt.append("- 检测敏感度：").append(request.getSensitivityThreshold()).append("\n");
        prompt.append("\n请按照指定的JSON格式返回检测结果。");

        return prompt.toString();
    }

    /**
     * 解析AI检测响应
     * <p>从AI返回的JsonNode中解析为检测结果DTO
     * 
     * @param root AI返回的JsonNode
     * @param request 原始检测请求
     * @return 解析后的检测结果DTO
     */
    private ImageModerationResultDTO parseAIResponse(JsonNode root, ImageModerationRequestDTO request) {
        log.debug("【图片内容检测】开始解析AI响应");

        ModerationCategoryDetailDTO pornResult = parseCategoryResult(root.path("pornography"), "pornography", request.getDetectPornography());
        ModerationCategoryDetailDTO politicalResult = parseCategoryResult(root.path("political"), "political", request.getDetectPolitical());
        ModerationCategoryDetailDTO violenceResult = parseCategoryResult(root.path("violence"), "violence", request.getDetectViolence());
        ModerationCategoryDetailDTO otherResult = parseCategoryResult(root.path("other"), "other", request.getDetectOther());

        List<ModerationCategoryDetailDTO> allResults = Arrays.asList(pornResult, politicalResult, violenceResult, otherResult);

        boolean hasViolation = allResults.stream().anyMatch(r -> r.getViolated());
        int maxConfidence = allResults.stream()
                .mapToInt(ModerationCategoryDetailDTO::getConfidence)
                .max()
                .orElse(0);

        String overallRiskLevel = calculateOverallRiskLevel(allResults);
        List<String> allLabels = allResults.stream()
                .filter(r -> r.getViolated() && r.getLabels() != null)
                .flatMap(r -> r.getLabels().stream())
                .distinct()
                .collect(Collectors.toList());

        String conclusion = root.path("conclusion").asText("review");
        String conclusionLabel = root.path("conclusionLabel").asText("待复审");

        ImageModerationResultDTO result = ImageModerationResultDTO.builder()
                .success(root.path("success").asBoolean(true))
                .conclusion(conclusion)
                .conclusionLabel(conclusionLabel)
                .hasViolation(hasViolation)
                .overallRiskLevel(overallRiskLevel)
                .maxConfidence(maxConfidence)
                .pornographyResult(pornResult)
                .politicalResult(politicalResult)
                .violenceResult(violenceResult)
                .otherResult(otherResult)
                .allViolationLabels(allLabels)
                .suggestion(root.path("suggestion").asText(""))
                .auditNote(root.path("auditNote").asText(""))
                .build();

        log.debug("【图片内容检测】AI响应解析完成, 检测到违规: {}, 违规标签数: {}", hasViolation, allLabels.size());
        return result;
    }

    /**
     * 解析单个分类的检测结果
     * <p>从JsonNode中提取单个分类的检测详情
     * 
     * @param node 分类对应的JsonNode
     * @param category 分类编码
     * @param enabled 是否启用该分类检测
     * @return 分类检测详情DTO
     */
    private ModerationCategoryDetailDTO parseCategoryResult(JsonNode node, String category, Boolean enabled) {
        if (enabled == null || !enabled) {
            return ModerationCategoryDetailDTO.builder()
                    .category(category)
                    .categoryLabel(CATEGORY_LABEL_MAP.getOrDefault(category, category))
                    .violated(false)
                    .confidence(0)
                    .riskLevel("safe")
                    .labels(Collections.emptyList())
                    .description("未启用该分类检测")
                    .build();
        }

        List<String> labels = new ArrayList<>();
        JsonNode labelsNode = node.path("labels");
        if (labelsNode.isArray()) {
            for (JsonNode labelNode : labelsNode) {
                labels.add(labelNode.asText());
            }
        }

        return ModerationCategoryDetailDTO.builder()
                .category(node.path("category").asText(category))
                .categoryLabel(node.path("categoryLabel").asText(CATEGORY_LABEL_MAP.getOrDefault(category, category)))
                .violated(node.path("violated").asBoolean(false))
                .confidence(node.path("confidence").asInt(0))
                .riskLevel(node.path("riskLevel").asText("safe"))
                .labels(labels)
                .description(node.path("description").asText(""))
                .build();
    }

    /**
     * 计算总体风险等级
     * <p>综合所有分类的检测结果，取最高风险等级作为总体风险等级
     * 
     * @param results 各分类检测结果列表
     * @return 总体风险等级
     */
    private String calculateOverallRiskLevel(List<ModerationCategoryDetailDTO> results) {
        int maxLevel = 0;
        for (ModerationCategoryDetailDTO result : results) {
            if (result.getViolated()) {
                int level = RISK_LEVEL_ORDER.getOrDefault(result.getRiskLevel(), 0);
                maxLevel = Math.max(maxLevel, level);
            }
        }

        for (Map.Entry<String, Integer> entry : RISK_LEVEL_ORDER.entrySet()) {
            if (entry.getValue() == maxLevel) {
                return entry.getKey();
            }
        }
        return "safe";
    }

    /**
     * 降级模式：本地规则引擎进行图片内容检测
     * <p>当AI服务不可用时，使用简单的规则和文件名关键词进行检测
     * <p>保证核心功能可用，提升系统健壮性
     * 
     * @param request 检测请求DTO
     * @return 模拟的检测结果
     */
    private ImageModerationResultDTO fallbackImageModeration(ImageModerationRequestDTO request) {
        log.warn("【图片内容检测】降级模式：使用本地规则引擎进行图片内容检测");

        String fileName = request.getFileName() != null ? request.getFileName().toLowerCase() : "";

        Map<String, List<String>> keywordMap = new HashMap<>();
        keywordMap.put("pornography", Arrays.asList("porn", "sex", "nude", "色情", "裸", "性感", "成人"));
        keywordMap.put("political", Arrays.asList("politic", "敏感", "违禁", "旗帜", "徽章"));
        keywordMap.put("violence", Arrays.asList("violence", "blood", "gun", "刀", "枪", "爆炸", "血腥", "暴力"));
        keywordMap.put("other", Arrays.asList("gamble", "drug", "烟", "酒", "赌博", "毒品"));

        ModerationCategoryDetailDTO pornResult = detectByKeywords(fileName, "pornography", keywordMap.get("pornography"), request.getDetectPornography());
        ModerationCategoryDetailDTO politicalResult = detectByKeywords(fileName, "political", keywordMap.get("political"), request.getDetectPolitical());
        ModerationCategoryDetailDTO violenceResult = detectByKeywords(fileName, "violence", keywordMap.get("violence"), request.getDetectViolence());
        ModerationCategoryDetailDTO otherResult = detectByKeywords(fileName, "other", keywordMap.get("other"), request.getDetectOther());

        List<ModerationCategoryDetailDTO> allResults = Arrays.asList(pornResult, politicalResult, violenceResult, otherResult);
        boolean hasViolation = allResults.stream().anyMatch(r -> r.getViolated());
        int maxConfidence = allResults.stream().mapToInt(ModerationCategoryDetailDTO::getConfidence).max().orElse(0);
        String overallRiskLevel = calculateOverallRiskLevel(allResults);

        List<String> allLabels = allResults.stream()
                .filter(r -> r.getViolated() && r.getLabels() != null)
                .flatMap(r -> r.getLabels().stream())
                .distinct()
                .collect(Collectors.toList());

        String conclusion = hasViolation ? "review" : "pass";
        String conclusionLabel = hasViolation ? "待复审" : "通过";
        String suggestion = hasViolation
                ? "检测到疑似违规内容，建议进行人工复核确认"
                : "未检测到明显违规内容，建议结合实际场景判断";
        String auditNote = hasViolation
                ? "文件名包含敏感关键词，降级模式下检测结果仅供参考，请人工审核图片实际内容"
                : "降级模式下基于文件名关键词检测，未发现明显敏感关键词";

        return ImageModerationResultDTO.builder()
                .success(true)
                .conclusion(conclusion)
                .conclusionLabel(conclusionLabel)
                .hasViolation(hasViolation)
                .overallRiskLevel(overallRiskLevel)
                .maxConfidence(maxConfidence)
                .pornographyResult(pornResult)
                .politicalResult(politicalResult)
                .violenceResult(violenceResult)
                .otherResult(otherResult)
                .allViolationLabels(allLabels)
                .suggestion(suggestion)
                .auditNote(auditNote)
                .build();
    }

    /**
     * 基于文件名关键词进行违规检测
     * <p>降级模式下使用，检查文件名是否包含敏感关键词
     * 
     * @param fileName 文件名（小写）
     * @param category 分类编码
     * @param keywords 关键词列表
     * @param enabled 是否启用该分类
     * @return 分类检测详情DTO
     */
    private ModerationCategoryDetailDTO detectByKeywords(String fileName, String category, List<String> keywords, Boolean enabled) {
        if (enabled == null || !enabled) {
            return ModerationCategoryDetailDTO.builder()
                    .category(category)
                    .categoryLabel(CATEGORY_LABEL_MAP.getOrDefault(category, category))
                    .violated(false)
                    .confidence(0)
                    .riskLevel("safe")
                    .labels(Collections.emptyList())
                    .description("未启用该分类检测")
                    .build();
        }

        List<String> matchedKeywords = keywords.stream()
                .filter(fileName::contains)
                .collect(Collectors.toList());

        boolean violated = !matchedKeywords.isEmpty();
        int confidence = violated ? Math.min(50 + matchedKeywords.size() * 10, 80) : 0;
        String riskLevel = violated ? (confidence >= 70 ? "medium" : "low") : "safe";
        String description = violated
                ? "文件名包含敏感关键词：" + String.join("、", matchedKeywords)
                : "未检测到敏感关键词";

        return ModerationCategoryDetailDTO.builder()
                .category(category)
                .categoryLabel(CATEGORY_LABEL_MAP.getOrDefault(category, category))
                .violated(violated)
                .confidence(confidence)
                .riskLevel(riskLevel)
                .labels(matchedKeywords)
                .description(description)
                .build();
    }

    /**
     * 验证图片数据并获取图片尺寸
     * <p>尝试解析Base64图片数据，验证其是否为有效图片
     * 
     * @param imageBase64 图片Base64编码数据
     * @return 图片尺寸数组，[宽度, 高度]
     * @throws IllegalArgumentException 如果图片数据无效
     */
    private int[] validateAndGetImageDimensions(String imageBase64) {
        if (imageBase64 == null || imageBase64.isEmpty()) {
            throw new IllegalArgumentException("图片数据不能为空");
        }

        try {
            byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
            ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
            BufferedImage image = ImageIO.read(bis);

            if (image == null) {
                throw new IllegalArgumentException("无法解析图片数据，请确保图片格式正确");
            }

            return new int[]{image.getWidth(), image.getHeight()};
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Base64解码失败: " + e.getMessage());
        } catch (Exception e) {
            throw new IllegalArgumentException("图片验证失败: " + e.getMessage());
        }
    }

    /**
     * 构建错误结果
     * <p>当检测过程中出现异常时，返回标准化的错误结果
     * 
     * @param taskId 任务ID
     * @param request 原始请求
     * @param errorMessage 错误信息
     * @param duration 检测耗时
     * @return 错误结果DTO
     */
    private ImageModerationResultDTO buildErrorResult(String taskId, ImageModerationRequestDTO request, String errorMessage, long duration) {
        ModerationCategoryDetailDTO emptyResult = ModerationCategoryDetailDTO.builder()
                .violated(false)
                .confidence(0)
                .riskLevel("safe")
                .labels(Collections.emptyList())
                .description("检测失败")
                .build();

        return ImageModerationResultDTO.builder()
                .taskId(taskId)
                .fileName(request.getFileName())
                .fileSize(request.getFileSize())
                .mimeType(request.getMimeType())
                .success(false)
                .conclusion("review")
                .conclusionLabel("待复审")
                .hasViolation(false)
                .overallRiskLevel("safe")
                .maxConfidence(0)
                .pornographyResult(emptyResult)
                .politicalResult(emptyResult)
                .violenceResult(emptyResult)
                .otherResult(emptyResult)
                .allViolationLabels(Collections.emptyList())
                .suggestion("检测过程出现错误，请重试或进行人工审核")
                .auditNote("错误信息：" + errorMessage)
                .detectionDurationMs(duration)
                .detectionTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .disclaimer(DISCLAIMER)
                .fallback(false)
                .build();
    }
}
