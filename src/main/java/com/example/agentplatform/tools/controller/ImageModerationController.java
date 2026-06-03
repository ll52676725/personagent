package com.example.agentplatform.tools.controller;

import com.example.agentplatform.common.dto.Result;
import com.example.agentplatform.tools.dto.ImageModerationRequestDTO;
import com.example.agentplatform.tools.dto.ImageModerationResultDTO;
import com.example.agentplatform.tools.service.ImageModerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

/**
 * 图片内容检测REST API控制器
 * <p>提供图片内容安全检测的前端访问接口，包括涉黄、涉政、涉爆、其他违规等多个检测维度
 * <p>所有接口路径统一前缀：/api/v1/agent-tools/image-moderation
 * <p>主要功能：
 * <ul>
 *   <li>图片内容检测：上传图片进行多维度内容安全审核</li>
 *   <li>检测报告下载：支持下载检测报告为文本文件</li>
 * </ul>
 * 
 * @author System
 * @since 2025-06-02
 * @see ImageModerationService
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agent-tools/image-moderation")
@RequiredArgsConstructor
public class ImageModerationController {

    /**
     * 图片内容检测核心服务，提供所有业务逻辑实现
     */
    private final ImageModerationService imageModerationService;

    /**
     * 执行图片内容检测
     * <p>POST /api/v1/agent-tools/image-moderation/detect
     * <p>检测维度包括：涉黄、涉政、涉爆、其他违规（赌博、毒品、烟酒等）
     * <p>使用多模态AI模型分析图片内容，当AI服务不可用时自动降级为本地规则引擎
     * 
     * @param request 检测请求DTO，包含图片Base64数据和检测配置
     * @return 检测结果DTO，包含各分类检测详情、总体结论、处理建议等
     */
    @PostMapping("/detect")
    public ResponseEntity<Result<ImageModerationResultDTO>> detectImageContent(
            @RequestBody ImageModerationRequestDTO request) {

        long startTime = System.currentTimeMillis();
        log.info("【图片内容检测】API: 开始执行图片内容检测, 文件名: {}, 文件大小: {}KB",
                request.getFileName(), request.getFileSize() != null ? request.getFileSize() / 1024 : 0);

        try {
            if (request.getImageBase64() == null || request.getImageBase64().isEmpty()) {
                log.warn("【图片内容检测】API: 图片数据为空, 耗时: {}ms", System.currentTimeMillis() - startTime);
                return ResponseEntity.badRequest()
                        .body(Result.badRequest("图片数据不能为空，请上传有效的图片文件"));
            }

            String base64Data = request.getImageBase64();
            if (base64Data.startsWith("data:image")) {
                int commaIndex = base64Data.indexOf(',');
                if (commaIndex > 0) {
                    base64Data = base64Data.substring(commaIndex + 1);
                    request.setImageBase64(base64Data);
                    log.debug("【图片内容检测】API: 已移除Base64数据前缀");
                }
            }

            if (request.getFileName() == null || request.getFileName().isEmpty()) {
                request.setFileName("unknown_image");
            }
            if (request.getMimeType() == null || request.getMimeType().isEmpty()) {
                request.setMimeType("image/jpeg");
            }

            ImageModerationResultDTO result = imageModerationService.moderateImage(request);

            long duration = System.currentTimeMillis() - startTime;
            log.info("【图片内容检测】API: 检测完成, 耗时: {}ms, 结论: {}, 是否违规: {}, " +
                            "总体风险: {}, 模型: {}, Token消耗: {}",
                    duration, result.getConclusionLabel(), result.getHasViolation(),
                    result.getOverallRiskLevel(), result.getModel(), result.getTokens());

            if (result.getHasViolation()) {
                log.warn("【图片内容检测】API: 检测到违规内容, 违规标签: {}, 最高置信度: {}",
                        result.getAllViolationLabels(), result.getMaxConfidence());
            }

            return ResponseEntity.ok(Result.success("图片内容检测完成", result));

        } catch (IllegalArgumentException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.warn("【图片内容检测】API: 请求参数错误, 耗时: {}ms, 错误: {}", duration, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Result.badRequest("参数错误: " + e.getMessage()));
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("【图片内容检测】API: 检测失败, 耗时: {}ms, 错误: {}", duration, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Result.serverError("图片内容检测失败: " + e.getMessage()));
        }
    }

    /**
     * 下载检测报告
     * <p>POST /api/v1/agent-tools/image-moderation/download-report
     * <p>将检测结果生成为文本报告文件，支持浏览器直接下载
     * <p>报告内容包括：检测基本信息、总体结论、各分类检测详情、违规标签、处理建议等
     * 
     * @param result 检测结果DTO，用于生成报告内容
     * @return 报告文件字节流，包含Content-Disposition头信息
     */
    @PostMapping("/download-report")
    public ResponseEntity<byte[]> downloadReport(
            @RequestBody ImageModerationResultDTO result) {

        long startTime = System.currentTimeMillis();
        log.info("【图片内容检测】API: 开始下载检测报告, 任务ID: {}", result.getTaskId());

        try {
            String reportContent = generateReportContent(result);
            byte[] content = reportContent.getBytes(StandardCharsets.UTF_8);

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "图片内容检测报告_" + timestamp + ".txt";
            String encodedFileName = Base64.getEncoder().encodeToString(fileName.getBytes(StandardCharsets.UTF_8));

            long duration = System.currentTimeMillis() - startTime;
            log.info("【图片内容检测】API: 检测报告下载成功, 耗时: {}ms, 文件名: {}, 文件大小: {}字节",
                    duration, fileName, content.length);

            return ResponseEntity.ok()
                    .header("Content-Type", "text/plain; charset=utf-8")
                    .header("Content-Disposition",
                            "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + encodedFileName)
                    .body(content);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("【图片内容检测】API: 下载检测报告失败, 耗时: {}ms, 错误: {}", duration, e.getMessage(), e);
            return ResponseEntity.status(500).build();
        }
    }

    /**
     * 生成检测报告内容
     * <p>将检测结果转换为格式化的文本报告，包含完整的检测信息
     * 
     * @param result 检测结果DTO
     * @return 格式化的报告文本
     */
    private String generateReportContent(ImageModerationResultDTO result) {
        StringBuilder sb = new StringBuilder();
        sb.append("================================================").append("\n");
        sb.append("              图片内容安全检测报告").append("\n");
        sb.append("================================================").append("\n\n");

        sb.append("【基本信息】").append("\n");
        sb.append("------------------------------------------------").append("\n");
        sb.append("任务ID：").append(result.getTaskId()).append("\n");
        sb.append("文件名：").append(result.getFileName()).append("\n");
        if (result.getFileSize() != null) {
            sb.append("文件大小：").append(formatFileSize(result.getFileSize())).append("\n");
        }
        if (result.getMimeType() != null) {
            sb.append("文件类型：").append(result.getMimeType()).append("\n");
        }
        if (result.getWidth() != null && result.getHeight() != null) {
            sb.append("图片尺寸：").append(result.getWidth()).append(" × ").append(result.getHeight()).append(" 像素").append("\n");
        }
        sb.append("检测时间：").append(result.getDetectionTime()).append("\n");
        sb.append("检测耗时：").append(result.getDetectionDurationMs() != null ?
                (result.getDetectionDurationMs() / 1000.0) : "0.0").append(" 秒").append("\n");
        if (result.getModel() != null) {
            sb.append("检测模型：").append(result.getModel()).append("\n");
        }
        if (result.getFallback() != null && result.getFallback()) {
            sb.append("检测模式：降级模式（本地规则引擎）").append("\n");
        }
        sb.append("\n");

        sb.append("【检测结论】").append("\n");
        sb.append("------------------------------------------------").append("\n");
        sb.append("总体结论：").append(result.getConclusionLabel()).append(" (").append(result.getConclusion()).append(")").append("\n");
        sb.append("是否违规：").append(result.getHasViolation() ? "是" : "否").append("\n");
        sb.append("风险等级：").append(getRiskLevelText(result.getOverallRiskLevel())).append("\n");
        sb.append("最高置信度：").append(result.getMaxConfidence()).append("%").append("\n");
        if (result.getAllViolationLabels() != null && !result.getAllViolationLabels().isEmpty()) {
            sb.append("违规标签：").append(String.join("、", result.getAllViolationLabels())).append("\n");
        }
        sb.append("\n");

        sb.append("【审核建议】").append("\n");
        sb.append("------------------------------------------------").append("\n");
        sb.append(result.getSuggestion()).append("\n\n");

        if (result.getAuditNote() != null && !result.getAuditNote().isEmpty()) {
            sb.append("【备注说明】").append("\n");
            sb.append("------------------------------------------------").append("\n");
            sb.append(result.getAuditNote()).append("\n\n");
        }

        sb.append("【分类检测详情】").append("\n");
        sb.append("------------------------------------------------").append("\n");
        appendCategoryDetail(sb, "1. 涉黄检测", result.getPornographyResult());
        appendCategoryDetail(sb, "2. 涉政检测", result.getPoliticalResult());
        appendCategoryDetail(sb, "3. 涉爆检测", result.getViolenceResult());
        appendCategoryDetail(sb, "4. 其他违规检测", result.getOtherResult());
        sb.append("\n");

        if (result.getDisclaimer() != null && !result.getDisclaimer().isEmpty()) {
            sb.append("【免责声明】").append("\n");
            sb.append("------------------------------------------------").append("\n");
            sb.append(result.getDisclaimer()).append("\n\n");
        }

        sb.append("================================================").append("\n");
        sb.append("                  报告结束").append("\n");
        sb.append("================================================").append("\n");

        return sb.toString();
    }

    /**
     * 追加单个分类的检测详情到报告
     * 
     * @param sb 报告内容构建器
     * @param title 分类标题
     * @param detail 分类检测详情DTO
     */
    private void appendCategoryDetail(StringBuilder sb, String title,
                                      com.example.agentplatform.tools.dto.ModerationCategoryDetailDTO detail) {
        if (detail == null) return;

        sb.append(title).append("\n");
        sb.append("  检测状态：").append(detail.getViolated() ? "⚠️ 检测到违规" : "✅ 正常").append("\n");
        sb.append("  置信度：").append(detail.getConfidence()).append("%").append("\n");
        sb.append("  风险等级：").append(getRiskLevelText(detail.getRiskLevel())).append("\n");
        if (detail.getLabels() != null && !detail.getLabels().isEmpty()) {
            sb.append("  违规标签：").append(String.join("、", detail.getLabels())).append("\n");
        }
        if (detail.getDescription() != null && !detail.getDescription().isEmpty()) {
            sb.append("  检测说明：").append(detail.getDescription()).append("\n");
        }
        sb.append("\n");
    }

    /**
     * 获取风险等级的中文描述
     * 
     * @param level 风险等级编码
     * @return 风险等级中文描述
     */
    private String getRiskLevelText(String level) {
        if (level == null) return "未知";
        return switch (level) {
            case "high" -> "🔴 高危";
            case "medium" -> "🟠 中危";
            case "low" -> "🟡 低危";
            case "safe" -> "🟢 安全";
            default -> "未知";
        };
    }

    /**
     * 格式化文件大小显示
     * 
     * @param bytes 文件大小（字节）
     * @return 格式化的文件大小字符串
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024));
    }
}
