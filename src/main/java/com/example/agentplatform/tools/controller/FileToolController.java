package com.example.agentplatform.tools.controller;

import com.example.agentplatform.common.dto.Result;
import com.example.agentplatform.tools.dto.FileConvertRequestDTO;
import com.example.agentplatform.tools.dto.FileConvertResultDTO;
import com.example.agentplatform.tools.dto.FileFormatInfoDTO;
import com.example.agentplatform.tools.service.FileConvertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件格式转换工具控制器
 * 提供文件格式转换相关的REST API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agent-tools/file")
@RequiredArgsConstructor
public class FileToolController {

    /**
     * 文件转换服务
     */
    private final FileConvertService fileConvertService;

    /**
     * 转换文件格式并返回结果信息
     *
     * @param file    上传的文件
     * @param request 转换请求参数
     * @return 转换结果，包含文件信息和Base64编码数据
     */
    @PostMapping("/convert")
    public ResponseEntity<Result<FileConvertResultDTO>> convertFile(
            @RequestParam("file") MultipartFile file,
            @Valid @ModelAttribute FileConvertRequestDTO request) {
        // 检查文件是否为空
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Result.badRequest("请选择要转换的文件"));
        }

        // 执行文件转换
        FileConvertResultDTO result = fileConvertService.convert(file, request);
        return ResponseEntity.ok(Result.success("文件格式转换成功", result));
    }

    /**
     * 转换文件格式并直接下载转换后的文件
     *
     * @param file    上传的文件
     * @param request 转换请求参数
     * @return 转换后的文件字节流
     */
    @PostMapping("/convert-download")
    public ResponseEntity<byte[]> convertAndDownload(
            @RequestParam("file") MultipartFile file,
            @Valid @ModelAttribute FileConvertRequestDTO request) {
        // 检查文件是否为空
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // 执行文件转换
        FileConvertResultDTO result = fileConvertService.convert(file, request);

        // 构建下载响应
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + result.getConvertedFileName() + "\"")
                .header("X-Original-Format", result.getOriginalFormat())
                .header("X-Target-Format", result.getTargetFormat())
                .header("X-Original-Size", String.valueOf(result.getOriginalSize()))
                .header("X-Converted-Size", String.valueOf(result.getConvertedSize()))
                .body(result.getRawFileData());
    }

    /**
     * 获取支持的文件格式列表
     *
     * @return 支持的格式信息列表
     */
    @GetMapping("/formats")
    public ResponseEntity<Result<List<FileFormatInfoDTO>>> getSupportedFormats() {
        List<FileFormatInfoDTO> formats = fileConvertService.getSupportedFormats();
        return ResponseEntity.ok(Result.success(formats));
    }
}
