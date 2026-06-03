package com.example.agentplatform.agent.techreport.service.impl;

import com.example.agentplatform.agent.techreport.dto.ReportSlideDTO;
import com.example.agentplatform.agent.techreport.dto.TechReportRequestDTO;
import com.example.agentplatform.agent.techreport.dto.TechReportResultDTO;
import com.example.agentplatform.agent.techreport.service.TechReportService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.StreamingChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TechReportServiceImpl implements TechReportService {

    private final ChatClient chatClient;
    private final StreamingChatClient streamingChatClient;
    private final ObjectMapper objectMapper;

    @Value("${agent.platform.default-model:spring-ai}")
    private String modelName;

    @Value("${agent.platform.ai.fallback-enabled:true}")
    private boolean fallbackEnabled;

    private static final String ARCHITECT_REPORT_SYSTEM_PROMPT = """
        你是一位经验丰富的首席架构师（Chief Architect），拥有15年以上的技术架构经验。
        你擅长为不同场景设计高质量的技术汇报方案，能够站在战略高度、技术深度和业务价值三个维度进行汇报设计。
        
        作为架构师，你的汇报特点：
        1. **战略视角**：从公司战略和业务目标出发，而不是只讲技术细节
        2. **结构化思维**：使用MECE原则、金字塔原理进行内容组织
        3. **数据驱动**：用量化指标支撑观点，用ROI分析体现价值
        4. **风险意识**：客观识别风险并给出应对方案
        5. **执行导向**：给出清晰的路线图和可落地的行动计划
        6. **受众适配**：针对不同受众（高管/技术团队/客户）调整内容深度和侧重点
        
        汇报内容必须包含：
        - 封面页（标题、汇报人、日期）
        - 目录页
        - 执行摘要（Executive Summary）
        - 背景与问题分析
        - 技术方案详解（架构图、核心组件、关键技术）
        - 实施路线图
        - 收益与ROI分析
        - 风险与应对措施
        - 资源需求
        - Q&A准备
        """;

    private static final String FULL_REPORT_SYSTEM_PROMPT = """
        你是一位经验丰富的首席架构师（Chief Architect），擅长设计专业的技术汇报PPT方案。
        
        请根据用户提供的场景信息，生成一份完整的技术汇报方案，以结构化JSON格式返回。
        
        输出格式要求（严格JSON格式，不要Markdown代码块）：
        {
          "reportTitle": "汇报标题",
          "executiveSummary": "执行摘要，200-300字，概括核心价值和结论",
          "slides": [
            {
              "slideNumber": 1,
              "title": "封面标题",
              "type": "cover",
              "content": "副标题或汇报说明",
              "keyPoints": ["关键点1", "关键点2"],
              "speakerNotes": "演讲者备注，具体要说的话",
              "visualSuggestion": "视觉建议，如公司logo、背景图等",
              "durationMinutes": 1
            },
            {
              "slideNumber": 2,
              "title": "目录",
              "type": "toc",
              "content": "汇报内容概览",
              "keyPoints": ["章节1", "章节2", "章节3"],
              "speakerNotes": "汇报者备注...",
              "visualSuggestion": "视觉建议...",
              "durationMinutes": 1
            },
            {
              "slideNumber": 3,
              "title": "执行摘要",
              "type": "summary",
              "content": "核心结论摘要...",
              "keyPoints": ["结论1", "结论2", "结论3"],
              "speakerNotes": "汇报者备注...",
              "visualSuggestion": "视觉建议...",
              "durationMinutes": 3
            },
            ...更多幻灯片
          ],
          "qaPreparation": ["可能问题1及回答思路", "可能问题2及回答思路"],
          "presentationTips": ["演讲技巧1", "演讲技巧2"],
          "totalSlides": N,
          "estimatedDurationMinutes": N
        }
        
        幻灯片类型说明：
        - cover: 封面页
        - toc: 目录页
        - summary: 摘要/总结页
        - content: 内容页
        - architecture: 架构图页
        - roadmap: 路线图页
        - data: 数据/图表页
        - comparison: 对比分析页
        - risk: 风险页
        - resource: 资源需求页
        - qa: Q&A页
        
        作为架构师，请确保：
        1. PPT页数控制在15-25页之间（可根据场景调整）
        2. 每页重点突出，不超过3-5个关键点
        3. 演讲者备注要具体、可直接使用
        4. 视觉建议要专业、可操作
        5. Q&A准备要覆盖最可能被问到的问题
        6. 针对不同受众调整技术深度
        7. 体现业务价值和ROI分析
        """;

    @Override
    public TechReportResultDTO generateReport(TechReportRequestDTO request) {
        return generateFullReport(request);
    }

    @Override
    public TechReportResultDTO generateOutline(TechReportRequestDTO request) {
        String userPrompt = buildUserPrompt(request);
        String outlinePrompt = OUTLINE_SYSTEM_PROMPT + "\n\n用户需求：\n" + userPrompt;

        log.info("[技术汇报] 生成汇报大纲，场景: {}, 受众: {}", request.getScene(), request.getAudience());

        try {
            List<Message> messages = List.of(
                    new SystemMessage(ARCHITECT_REPORT_SYSTEM_PROMPT),
                    new UserMessage(outlinePrompt)
            );

            ChatResponse response = chatClient.call(new Prompt(messages));
            String content = response.getResult().getOutput().getContent();

            return parseOutlineResult(content, request);
        } catch (Exception e) {
            log.error("[技术汇报] 生成大纲失败: {}", e.getMessage(), e);
            if (fallbackEnabled) {
                return generateFallbackReport(request);
            }
            throw new RuntimeException("生成技术汇报大纲失败: " + e.getMessage(), e);
        }
    }

    @Override
    public TechReportResultDTO generateFullReport(TechReportRequestDTO request) {
        String userPrompt = buildUserPrompt(request);

        log.info("[技术汇报] 生成完整汇报方案，场景: {}, 受众: {}, 类型: {}",
                request.getScene(), request.getAudience(), request.getReportType());

        try {
            List<Message> messages = List.of(
                    new SystemMessage(FULL_REPORT_SYSTEM_PROMPT),
                    new UserMessage(userPrompt)
            );

            ChatResponse response = chatClient.call(new Prompt(messages));
            String content = response.getResult().getOutput().getContent();

            log.debug("[技术汇报] AI返回内容: {}", content);

            TechReportResultDTO result = parseJsonResult(content);
            result.setModel(modelName);
            result.setContentMarkdown(exportToMarkdown(result));

            return result;
        } catch (Exception e) {
            log.error("[技术汇报] 生成完整方案失败: {}", e.getMessage(), e);
            if (fallbackEnabled) {
                return generateFallbackReport(request);
            }
            throw new RuntimeException("生成技术汇报方案失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<String> generateReportStream(TechReportRequestDTO request) {
        String userPrompt = buildUserPrompt(request);

        log.info("[技术汇报] 流式生成汇报方案，场景: {}", request.getScene());

        List<Message> messages = List.of(
                new SystemMessage(FULL_REPORT_SYSTEM_PROMPT),
                new UserMessage(userPrompt)
        );

        return streamingChatClient.stream(new Prompt(messages))
                .map(response -> {
                    String content = response.getResult().getOutput().getContent();
                    return content != null ? content : "";
                })
                .filter(content -> !content.isEmpty());
    }

    @Override
    public String exportToMarkdown(TechReportResultDTO result) {
        StringBuilder sb = new StringBuilder();

        sb.append("# ").append(result.getReportTitle()).append("\n\n");

        sb.append("## 执行摘要\n\n").append(result.getExecutiveSummary()).append("\n\n");

        sb.append("## 汇报内容\n\n");

        for (ReportSlideDTO slide : result.getSlides()) {
            sb.append("### 第").append(slide.getSlideNumber()).append("页：")
                    .append(slide.getTitle()).append("  \n");
            sb.append("**类型**：").append(getSlideTypeLabel(slide.getType())).append("  \n");
            sb.append("**预计时长**：").append(slide.getDurationMinutes()).append("分钟  \n\n");

            if (slide.getContent() != null && !slide.getContent().isBlank()) {
                sb.append(slide.getContent()).append("\n\n");
            }

            if (slide.getKeyPoints() != null && !slide.getKeyPoints().isEmpty()) {
                sb.append("**关键点**：\n");
                for (String point : slide.getKeyPoints()) {
                    sb.append("- ").append(point).append("\n");
                }
                sb.append("\n");
            }

            if (slide.getSpeakerNotes() != null && !slide.getSpeakerNotes().isBlank()) {
                sb.append("**演讲者备注**：\n> ").append(slide.getSpeakerNotes()).append("\n\n");
            }

            if (slide.getVisualSuggestion() != null && !slide.getVisualSuggestion().isBlank()) {
                sb.append("**视觉建议**：").append(slide.getVisualSuggestion()).append("\n\n");
            }

            sb.append("---\n\n");
        }

        if (result.getQaPreparation() != null && !result.getQaPreparation().isEmpty()) {
            sb.append("## Q&A 准备\n\n");
            for (int i = 0; i < result.getQaPreparation().size(); i++) {
                sb.append((i + 1) + ". ").append(result.getQaPreparation().get(i)).append("\n");
            }
            sb.append("\n");
        }

        if (result.getPresentationTips() != null && !result.getPresentationTips().isEmpty()) {
            sb.append("## 演讲技巧\n\n");
            for (int i = 0; i < result.getPresentationTips().size(); i++) {
                sb.append((i + 1) + ". ").append(result.getPresentationTips().get(i)).append("\n");
            }
            sb.append("\n");
        }

        sb.append("## 汇总信息\n\n");
        sb.append("- 总页数：").append(result.getTotalSlides()).append("页\n");
        sb.append("- 预计时长：").append(result.getEstimatedDurationMinutes()).append("分钟\n");
        sb.append("- 使用模型：").append(result.getModel()).append("\n");

        return sb.toString();
    }

    @Override
    public String exportToPptOutline(TechReportResultDTO result) {
        StringBuilder sb = new StringBuilder();

        sb.append("PPT大纲 - ").append(result.getReportTitle()).append("\n");
        sb.append("=".repeat(50)).append("\n\n");

        sb.append("【执行摘要】\n").append(result.getExecutiveSummary()).append("\n\n");

        for (ReportSlideDTO slide : result.getSlides()) {
            sb.append(String.format("第%2d页 | %-20s | %d分钟\n",
                    slide.getSlideNumber(),
                    slide.getTitle(),
                    slide.getDurationMinutes()));

            if (slide.getKeyPoints() != null && !slide.getKeyPoints().isEmpty()) {
                for (String point : slide.getKeyPoints()) {
                    sb.append("  • ").append(point).append("\n");
                }
            }
            sb.append("\n");
        }

        sb.append("【预计总时长】: ").append(result.getEstimatedDurationMinutes()).append("分钟\n");
        sb.append("【总页数】: ").append(result.getTotalSlides()).append("页\n");

        return sb.toString();
    }

    private String buildUserPrompt(TechReportRequestDTO request) {
        StringBuilder sb = new StringBuilder();

        sb.append("请为我生成一份技术汇报方案，具体信息如下：\n\n");
        sb.append("【汇报场景】: ").append(request.getScene()).append("\n");
        sb.append("【汇报受众】: ").append(request.getAudience()).append("\n");

        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            sb.append("【场景描述】: ").append(request.getDescription()).append("\n");
        }

        if (request.getReportType() != null && !request.getReportType().isBlank()) {
            sb.append("【汇报类型】: ").append(request.getReportType()).append("\n");
        }

        if (request.getSlideCount() != null) {
            sb.append("【期望页数】: ").append(request.getSlideCount()).append("页左右\n");
        }

        if (request.getKeyPoints() != null && !request.getKeyPoints().isEmpty()) {
            sb.append("【必须包含的关键点】:\n");
            for (int i = 0; i < request.getKeyPoints().size(); i++) {
                sb.append((i + 1) + ". ").append(request.getKeyPoints().get(i)).append("\n");
            }
        }

        if (request.getIndustry() != null && !request.getIndustry().isBlank()) {
            sb.append("【所属行业】: ").append(request.getIndustry()).append("\n");
        }

        if (request.getCompanySize() != null && !request.getCompanySize().isBlank()) {
            sb.append("【公司规模】: ").append(request.getCompanySize()).append("\n");
        }

        if (request.getAdditionalInfo() != null && !request.getAdditionalInfo().isBlank()) {
            sb.append("【补充信息】: ").append(request.getAdditionalInfo()).append("\n");
        }

        sb.append("\n作为架构师，请针对「").append(request.getAudience()).append("」这个受众群体，");
        sb.append("调整汇报的技术深度和侧重点，确保汇报既有战略高度又有落地可行性。");

        return sb.toString();
    }

    private TechReportResultDTO parseJsonResult(String content) throws Exception {
        String jsonContent = content.trim();

        if (jsonContent.startsWith("```json")) {
            jsonContent = jsonContent.substring(7);
        }
        if (jsonContent.startsWith("```")) {
            jsonContent = jsonContent.substring(3);
        }
        if (jsonContent.endsWith("```")) {
            jsonContent = jsonContent.substring(0, jsonContent.length() - 3);
        }

        jsonContent = jsonContent.trim();

        log.debug("[技术汇报] 解析JSON内容: {}", jsonContent);

        Map<String, Object> map = objectMapper.readValue(jsonContent, new TypeReference<Map<String, Object>>() {});

        List<ReportSlideDTO> slides = new ArrayList<>();
        List<Map<String, Object>> slideMaps = (List<Map<String, Object>>) map.get("slides");
        if (slideMaps != null) {
            for (Map<String, Object> slideMap : slideMaps) {
                ReportSlideDTO slide = ReportSlideDTO.builder()
                        .slideNumber((Integer) slideMap.get("slideNumber"))
                        .title((String) slideMap.get("title"))
                        .type((String) slideMap.get("type"))
                        .content((String) slideMap.get("content"))
                        .keyPoints(slideMap.get("keyPoints") != null ?
                                (List<String>) slideMap.get("keyPoints") : List.of())
                        .speakerNotes((String) slideMap.get("speakerNotes"))
                        .visualSuggestion((String) slideMap.get("visualSuggestion"))
                        .durationMinutes(slideMap.get("durationMinutes") != null ?
                                (Integer) slideMap.get("durationMinutes") : 2)
                        .build();
                slides.add(slide);
            }
        }

        return TechReportResultDTO.builder()
                .reportTitle((String) map.get("reportTitle"))
                .executiveSummary((String) map.get("executiveSummary"))
                .slides(slides)
                .qaPreparation(map.get("qaPreparation") != null ?
                        (List<String>) map.get("qaPreparation") : List.of())
                .presentationTips(map.get("presentationTips") != null ?
                        (List<String>) map.get("presentationTips") : List.of())
                .totalSlides(map.get("totalSlides") != null ?
                        (Integer) map.get("totalSlides") : slides.size())
                .estimatedDurationMinutes(map.get("estimatedDurationMinutes") != null ?
                        (Integer) map.get("estimatedDurationMinutes") :
                        slides.stream().mapToInt(s -> s.getDurationMinutes() != null ? s.getDurationMinutes() : 2).sum())
                .build();
    }

    private TechReportResultDTO parseOutlineResult(String content, TechReportRequestDTO request) {
        return TechReportResultDTO.builder()
                .reportTitle(request.getScene() + " - 技术汇报大纲")
                .executiveSummary("根据需求生成的技术汇报大纲，包含完整的内容结构。")
                .slides(List.of(
                        ReportSlideDTO.builder()
                                .slideNumber(1)
                                .title("大纲内容")
                                .type("content")
                                .content(content)
                                .keyPoints(List.of())
                                .speakerNotes("")
                                .visualSuggestion("使用简洁的列表展示")
                                .durationMinutes(2)
                                .build()
                ))
                .qaPreparation(List.of())
                .presentationTips(List.of())
                .totalSlides(1)
                .estimatedDurationMinutes(2)
                .build();
    }

    private TechReportResultDTO generateFallbackReport(TechReportRequestDTO request) {
        log.warn("[技术汇报] 使用Fallback模式生成汇报方案");

        String title = request.getScene() + " 技术汇报方案";
        String execSummary = String.format(
                "本方案针对「%s」场景，面向「%s」进行技术汇报。方案涵盖背景分析、技术架构、实施路径、收益分析等核心内容，旨在清晰地传达技术方案的业务价值和可行性。",
                request.getScene(), request.getAudience()
        );

        List<ReportSlideDTO> slides = generateFallbackSlides(request);

        return TechReportResultDTO.builder()
                .reportTitle(title)
                .executiveSummary(execSummary)
                .slides(slides)
                .qaPreparation(generateFallbackQA(request))
                .presentationTips(generateFallbackTips())
                .totalSlides(slides.size())
                .estimatedDurationMinutes(slides.stream().mapToInt(s -> s.getDurationMinutes() != null ? s.getDurationMinutes() : 2).sum())
                .model("fallback-local")
                .build();
    }

    private List<ReportSlideDTO> generateFallbackSlides(TechReportRequestDTO request) {
        List<ReportSlideDTO> slides = new ArrayList<>();

        slides.add(ReportSlideDTO.builder()
                .slideNumber(1)
                .title(request.getScene() + " 技术汇报")
                .type("cover")
                .content("汇报人：架构师 | 日期：202X年XX月")
                .keyPoints(List.of("专业技术汇报", "架构师视角"))
                .speakerNotes("大家好，今天我将从架构师的角度，为大家汇报" + request.getScene() + "的技术方案。")
                .visualSuggestion("使用公司模板，加入logo，简洁大气")
                .durationMinutes(1)
                .build());

        slides.add(ReportSlideDTO.builder()
                .slideNumber(2)
                .title("目录")
                .type("toc")
                .content("本次汇报的核心内容")
                .keyPoints(List.of("背景与问题分析", "技术方案架构", "实施路线图", "收益与ROI", "风险与应对", "资源需求"))
                .speakerNotes("我的汇报将分为六个部分，首先看背景与问题分析。")
                .visualSuggestion("清晰的目录结构，高亮当前章节")
                .durationMinutes(1)
                .build());

        slides.add(ReportSlideDTO.builder()
                .slideNumber(3)
                .title("执行摘要 (Executive Summary)")
                .type("summary")
                .content("核心结论和价值概览")
                .keyPoints(List.of(
                        "业务价值：提升效率30%，降低成本20%",
                        "技术方案：采用微服务架构+云原生技术栈",
                        "实施周期：3个月完成核心功能上线",
                        "ROI：预计6个月收回投资"
                ))
                .speakerNotes("在深入细节之前，先给大家看核心结论：我们的方案能够带来显著的业务价值，技术上成熟可靠，预计6个月可收回投资。")
                .visualSuggestion("四象限图或关键数字卡片展示")
                .durationMinutes(3)
                .build());

        slides.add(ReportSlideDTO.builder()
                .slideNumber(4)
                .title("背景与问题分析")
                .type("content")
                .content("为什么我们需要做这件事")
                .keyPoints(List.of(
                        "业务挑战：当前系统面临性能瓶颈",
                        "技术痛点：架构老旧，维护成本高",
                        "市场机遇：数字化转型窗口期",
                        "竞争态势：竞品已开始布局"
                ))
                .speakerNotes("首先看背景。当前我们面临几个核心问题...这些问题如果不解决，将严重影响我们的市场竞争力。")
                .visualSuggestion("问题-影响分析矩阵图")
                .durationMinutes(3)
                .build());

        slides.add(ReportSlideDTO.builder()
                .slideNumber(5)
                .title("总体架构设计")
                .type("architecture")
                .content("云原生微服务架构")
                .keyPoints(List.of(
                        "接入层：API Gateway + 负载均衡",
                        "服务层：领域驱动设计，微服务拆分",
                        "数据层：读写分离 + 分布式缓存",
                        "基础设施：Kubernetes + DevOps流水线"
                ))
                .speakerNotes("基于这些分析，我们设计了云原生微服务架构。整体分为四层...这样的架构能够支撑未来3-5年的业务发展。")
                .visualSuggestion("分层架构图，使用不同颜色标识各层")
                .durationMinutes(4)
                .build());

        slides.add(ReportSlideDTO.builder()
                .slideNumber(6)
                .title("核心技术选型")
                .type("comparison")
                .content("技术选型的考量与决策")
                .keyPoints(List.of(
                        "开发语言：Java 21 + Spring Boot 3.2",
                        "数据库：MySQL 8.0 + Redis 7.0",
                        "消息队列：Kafka（高吞吐量场景）",
                        "服务网格：Istio（流量治理需求）"
                ))
                .speakerNotes("在技术选型上，我们做了充分的对比分析。例如在消息队列上，我们对比了RabbitMQ和Kafka...最终选择Kafka是因为...")
                .visualSuggestion("技术选型对比表，列出方案、优势、适用场景")
                .durationMinutes(3)
                .build());

        slides.add(ReportSlideDTO.builder()
                .slideNumber(7)
                .title("实施路线图")
                .type("roadmap")
                .content("分阶段落地计划")
                .keyPoints(List.of(
                        "Phase 1（第1个月）：基础设施搭建 + 核心服务开发",
                        "Phase 2（第2个月）：业务功能开发 + 联调测试",
                        "Phase 3（第3个月）：灰度发布 + 性能优化",
                        "Phase 4（第4-6个月）：全量上线 + 运维体系完善"
                ))
                .speakerNotes("我们将分四个阶段推进实施...每个阶段都有明确的里程碑和交付物，确保可控。")
                .visualSuggestion("甘特图或时间轴，标注关键里程碑")
                .durationMinutes(3)
                .build());

        slides.add(ReportSlideDTO.builder()
                .slideNumber(8)
                .title("收益与ROI分析")
                .type("data")
                .content("量化的业务价值")
                .keyPoints(List.of(
                        "直接收益：年节约成本50万元",
                        "效率提升：开发效率提升30%，上线周期缩短50%",
                        "用户体验：系统响应时间从3s降至500ms",
                        "投资回收期：6个月"
                ))
                .speakerNotes("大家最关心的收益方面，我们做了详细的测算...6个月就能收回投资，之后每年都有持续的收益。")
                .visualSuggestion("ROI分析图表，成本收益对比柱状图")
                .durationMinutes(3)
                .build());

        slides.add(ReportSlideDTO.builder()
                .slideNumber(9)
                .title("风险识别与应对")
                .type("risk")
                .content("客观评估风险，提前准备预案")
                .keyPoints(List.of(
                        "技术风险：服务拆分粒度不当 → 应对：迭代式拆分，灰度验证",
                        "进度风险：人员缺口 → 应对：提前储备，核心模块外包支持",
                        "数据风险：迁移数据一致性 → 应对：双写校验，回滚方案",
                        "运维风险：监控体系不完善 → 应对：APM全链路监控，告警规则全覆盖"
                ))
                .speakerNotes("当然，任何方案都有风险。我们识别了四个主要风险，并且每个风险都有对应的应对措施...")
                .visualSuggestion("风险矩阵图，按影响/可能性四象限展示")
                .durationMinutes(3)
                .build());

        slides.add(ReportSlideDTO.builder()
                .slideNumber(10)
                .title("资源需求")
                .type("resource")
                .content("需要的支持与投入")
                .keyPoints(List.of(
                        "人力资源：后端3人，前端2人，测试1人，运维1人",
                        "预算需求：云资源20万/年，软件许可10万",
                        "时间窗口：3个月核心功能上线",
                        "决策支持：需要管理层在优先级上给予支持"
                ))
                .speakerNotes("要落地这个方案，我们需要这些资源支持...希望管理层能够协调资源，确保项目顺利推进。")
                .visualSuggestion("资源需求表，分类清晰")
                .durationMinutes(2)
                .build());

        slides.add(ReportSlideDTO.builder()
                .slideNumber(11)
                .title("总结与下一步")
                .type("summary")
                .content("核心要点回顾")
                .keyPoints(List.of(
                        "价值明确：ROI 6个月，持续收益显著",
                        "方案可行：技术成熟，风险可控",
                        "路线清晰：分阶段实施，快速交付价值",
                        "下一步：本周内评审立项，下周启动"
                ))
                .speakerNotes("总结一下，这个方案价值明确、技术可行、路线清晰。建议本周内完成评审立项，下周就可以启动。")
                .visualSuggestion("要点总结卡片，突出下一步行动")
                .durationMinutes(2)
                .build());

        slides.add(ReportSlideDTO.builder()
                .slideNumber(12)
                .title("Q&A")
                .type("qa")
                .content("感谢聆听，欢迎提问")
                .keyPoints(List.of("技术问题", "资源问题", "进度问题", "风险问题"))
                .speakerNotes("以上就是我的汇报，大家有什么问题？")
                .visualSuggestion("简洁的Q&A页面，联系方式")
                .durationMinutes(10)
                .build());

        return slides;
    }

    private List<String> generateFallbackQA(TechReportRequestDTO request) {
        return List.of(
                "Q: 这个方案的技术门槛如何？团队能hold住吗？A: 我们选择的都是主流成熟技术，团队有相关经验。同时我们会安排技术培训和专家支持，确保平滑过渡。",
                "Q: 为什么选择这个技术栈而不是其他方案？A: 我们做了全面的技术调研，从成熟度、社区支持、人才招聘、长期维护成本等多个维度评估，当前方案是综合最优解。",
                "Q: 万一项目延期怎么办？A: 我们有明确的里程碑管控，每周做进度评估。同时准备了Plan B，如果关键路径延期，可以调整范围，先交付核心价值。",
                "Q: 数据迁移的风险如何控制？A: 我们采用渐进式迁移策略：先双写，再校验，最后切流。每一步都有回滚方案，确保不影响业务连续性。",
                "Q: 成本估算准确吗？会不会超预算？A: 我们的估算是基于历史数据和行业标准，预留了20%的缓冲。同时采用云原生按需付费模式，可以灵活控制成本。"
        );
    }

    private List<String> generateFallbackTips() {
        return List.of(
                "开场30秒抓住注意力：用一个核心数据或问题开场，让听众立刻明白汇报的价值",
                "善用数据讲故事：每个观点都要有数据支撑，用对比和趋势图让数据说话",
                "控制每页时长：内容页2-3分钟，架构图页3-4分钟，不要超过5分钟",
                "预判问题，提前准备：把最可能被问到的问题答案提前准备好，甚至可以主动提及",
                "眼神交流与肢体语言：照顾到会场每个人，不要只盯着屏幕或一个人",
                "结尾强而有力：总结3个核心点，明确下一步行动，让听众记住关键信息",
                "PPT原则：一屏一主题，字少图多，用视觉化方式呈现复杂内容"
        );
    }

    private String getSlideTypeLabel(String type) {
        Map<String, String> typeLabels = new HashMap<>();
        typeLabels.put("cover", "封面");
        typeLabels.put("toc", "目录");
        typeLabels.put("summary", "摘要");
        typeLabels.put("content", "内容");
        typeLabels.put("architecture", "架构");
        typeLabels.put("roadmap", "路线图");
        typeLabels.put("data", "数据");
        typeLabels.put("comparison", "对比");
        typeLabels.put("risk", "风险");
        typeLabels.put("resource", "资源");
        typeLabels.put("qa", "问答");
        return typeLabels.getOrDefault(type, type);
    }

    private static final String OUTLINE_SYSTEM_PROMPT = """
        请生成一份技术汇报的大纲结构，包含以下部分：
        1. 封面
        2. 目录
        3. 执行摘要
        4. 背景与问题分析
        5. 技术方案详解
        6. 实施路线图
        7. 收益分析
        8. 风险与应对
        9. 资源需求
        10. 总结与下一步
        11. Q&A
        
        请用中文，结构化地输出每个部分的核心内容要点。
        """;
}
