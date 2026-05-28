package com.example.agentplatform.tools.controller;

import com.example.agentplatform.common.dto.Result;
import com.example.agentplatform.tools.dto.ImageConvertRequestDTO;
import com.example.agentplatform.tools.dto.ImageConvertResultDTO;
import com.example.agentplatform.tools.dto.ImageFormatInfoDTO;
import com.example.agentplatform.tools.service.ImageConvertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/agent-tools/image")
@RequiredArgsConstructor
public class ImageToolController {

    private final ImageConvertService imageConvertService;

    @PostMapping("/convert")
    public ResponseEntity<Result<ImageConvertResultDTO>> convertImage(
            @RequestParam("file") MultipartFile file,
            @Valid @ModelAttribute ImageConvertRequestDTO request) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Result.badRequest("请选择要转换的图片文件"));
        }
        ImageConvertResultDTO result = imageConvertService.convert(file, request);
        return ResponseEntity.ok(Result.success("图片格式转换成功", result));
    }

    @PostMapping("/convert-download")
    public ResponseEntity<byte[]> convertAndDownload(
            @RequestParam("file") MultipartFile file,
            @Valid @ModelAttribute ImageConvertRequestDTO request) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        ImageConvertResultDTO result = imageConvertService.convert(file, request);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + result.getConvertedFileName() + "\"")
                .header("X-Original-Format", result.getOriginalFormat())
                .header("X-Target-Format", result.getTargetFormat())
                .header("X-Original-Size", String.valueOf(result.getOriginalSize()))
                .header("X-Converted-Size", String.valueOf(result.getConvertedSize()))
                .body(result.getRawImageData());
    }

    @GetMapping("/formats")
    public ResponseEntity<Result<List<ImageFormatInfoDTO>>> getSupportedFormats() {
        List<ImageFormatInfoDTO> formats = imageConvertService.getSupportedFormats();
        return ResponseEntity.ok(Result.success(formats));
    }
}
