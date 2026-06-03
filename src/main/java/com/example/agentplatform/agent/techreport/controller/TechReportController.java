package com.example.agentplatform.agent.techreport.controller;

import com.example.agentplatform.agent.techreport.dto.TechReportRequestDTO;
import com.example.agentplatform.agent.techreport.dto.TechReportResultDTO;
import com.example.agentplatform.agent.techreport.service.TechReportService;
import com.example.agentplatform.common.dto.Result;
import com.example.agentplatform.common.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/agent-techreport")
@RequiredArgsConstructor
public class TechReportController {

    private final TechReportService techReportService;

    @PostMapping("/generate")
    public ResponseEntity<Result<TechReportResultDTO>> generateReport(
            @Valid @RequestBody TechReportRequestDTO request) {
        log.info("[技术汇报] 生成汇报方案，场景: {}, 受众: {}", request.getScene(), request.getAudience());

        if (request.getScene() == null || request.getScene().isBlank()) {
            throw new BusinessException("汇报场景不能为空");
        }
        if (request.getAudience() == null || request.getAudience().isBlank()) {
            throw new BusinessException("汇报受众不能为空");
        }

        TechReportResultDTO result = techReportService.generateFullReport(request);
        return ResponseEntity.ok(Result.success("技术汇报方案生成成功", result));
    }

    @PostMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> generateReportStream(
            @Valid @RequestBody TechReportRequestDTO request) {
        log.info("[技术汇报] 流式生成汇报方案，场景: {}", request.getScene());

        if (request.getScene() == null || request.getScene().isBlank()) {
            return Flux.error(new BusinessException("汇报场景不能为空"));
        }
        if (request.getAudience() == null || request.getAudience().isBlank()) {
            return Flux.error(new BusinessException("汇报受众不能为空"));
        }

        return techReportService.generateReportStream(request);
    }

    @PostMapping("/generate/outline")
    public ResponseEntity<Result<TechReportResultDTO>> generateOutline(
            @Valid @RequestBody TechReportRequestDTO request) {
        log.info("[技术汇报] 生成汇报大纲，场景: {}", request.getScene());

        if (request.getScene() == null || request.getScene().isBlank()) {
            throw new BusinessException("汇报场景不能为空");
        }

        TechReportResultDTO result = techReportService.generateOutline(request);
        return ResponseEntity.ok(Result.success("汇报大纲生成成功", result));
    }

    @PostMapping("/export/markdown")
    public ResponseEntity<Result<String>> exportMarkdown(
            @RequestBody TechReportResultDTO result) {
        log.info("[技术汇报] 导出Markdown格式");

        String markdown = techReportService.exportToMarkdown(result);
        return ResponseEntity.ok(Result.success("Markdown导出成功", markdown));
    }

    @PostMapping("/export/ppt-outline")
    public ResponseEntity<Result<String>> exportPptOutline(
            @RequestBody TechReportResultDTO result) {
        log.info("[技术汇报] 导出PPT大纲");

        String outline = techReportService.exportToPptOutline(result);
        return ResponseEntity.ok(Result.success("PPT大纲导出成功", outline));
    }

    @GetMapping("/export/markdown/download")
    public ResponseEntity<byte[]> downloadMarkdown(
            @RequestBody TechReportResultDTO result) {
        log.info("[技术汇报] 下载Markdown文件");

        String markdown = techReportService.exportToMarkdown(result);
        byte[] content = markdown.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=tech-report.md")
                .contentType(MediaType.parseMediaType("text/markdown; charset=UTF-8"))
                .contentLength(content.length)
                .body(content);
    }

    @GetMapping("/scenes")
    public ResponseEntity<Result<List<Map<String, String>>>> getSceneTemplates() {
        List<Map<String, String>> scenes = List.of(
                createScene("季度技术汇报", "向管理层汇报季度技术工作成果和下季度规划", "公司管理层", "15-20"),
                createScene("年度技术总结", "年度技术工作全面总结和来年战略规划", "公司全员/管理层", "20-25"),
                createScene("新项目立项汇报", "申请新项目立项，阐述技术方案和可行性", "项目评审委员会", "15-20"),
                createScene("技术方案评审", "具体技术方案的详细设计评审", "技术委员会/架构组", "10-15"),
                createScene("架构升级汇报", "系统架构升级改造方案汇报", "管理层+技术团队", "15-20"),
                createScene("问题复盘汇报", "线上故障或技术问题的复盘分析", "相关干系人", "10-15"),
                createScene("技术选型汇报", "关键技术选型决策汇报", "技术委员会", "10-15"),
                createScene("预算申请汇报", "技术部门年度预算申请汇报", "财务+管理层", "15-20")
        );

        return ResponseEntity.ok(Result.success(scenes));
    }

    @GetMapping("/audiences")
    public ResponseEntity<Result<List<Map<String, String>>>> getAudienceTypes() {
        List<Map<String, String>> audiences = List.of(
                Map.of("type", "company_management", "label", "公司管理层", "description", "侧重业务价值、ROI、风险管控"),
                Map.of("type", "technical_committee", "label", "技术委员会", "description", "侧重技术深度、架构设计、方案可行性"),
                Map.of("type", "development_team", "label", "研发团队", "description", "侧重技术细节、落地方案、最佳实践"),
                Map.of("type", "customer", "label", "客户", "description", "侧重业务价值、稳定性、成本效益"),
                Map.of("type", "investor", "label", "投资人", "description", "侧重战略价值、市场机会、竞争优势"),
                Map.of("type", "mixed", "label", "混合受众", "description", "需要平衡不同层次的关注点")
        );

        return ResponseEntity.ok(Result.success(audiences));
    }

    private Map<String, String> createScene(String name, String description, String defaultAudience, String slideCount) {
        Map<String, String> scene = new HashMap<>();
        scene.put("name", name);
        scene.put("description", description);
        scene.put("defaultAudience", defaultAudience);
        scene.put("slideCount", slideCount);
        return scene;
    }
}
