package com.example.agentplatform.tools.controller;

import com.example.agentplatform.common.dto.Result;
import com.example.agentplatform.tools.dto.PhotoSizeDTO;
import com.example.agentplatform.tools.dto.PhotoStandardizationRequestDTO;
import com.example.agentplatform.tools.dto.PhotoStandardizationResultDTO;
import com.example.agentplatform.tools.service.PhotoStandardizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/agent-tools/photo")
@RequiredArgsConstructor
public class PhotoStandardizationController {
    
    private final PhotoStandardizationService photoStandardizationService;
    
    @PostMapping("/standardize")
    public ResponseEntity<Result<PhotoStandardizationResultDTO>> standardizePhoto(
            @RequestParam("file") MultipartFile file,
            @Valid @ModelAttribute PhotoStandardizationRequestDTO request) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Result.badRequest("请选择要处理的图片文件"));
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return ResponseEntity.badRequest().body(Result.badRequest("请选择图片文件"));
        }
        
        PhotoStandardizationResultDTO result = photoStandardizationService.standardize(file, request);
        return ResponseEntity.ok(Result.success("证件照处理成功", result));
    }
    
    @PostMapping("/standardize-download")
    public ResponseEntity<byte[]> standardizeAndDownload(
            @RequestParam("file") MultipartFile file,
            @Valid @ModelAttribute PhotoStandardizationRequestDTO request) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        PhotoStandardizationResultDTO result = photoStandardizationService.standardize(file, request);
        String base64Data = result.getImageDataBase64();
        String base64Content = base64Data.substring(base64Data.indexOf(",") + 1);
        byte[] imageBytes = Base64.getDecoder().decode(base64Content);
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + result.getConvertedFileName() + "\"")
                .header("X-Photo-Size", result.getPhotoSize())
                .header("X-Background-Color", result.getBackgroundColor())
                .header("X-DPI", result.getDpi())
                .header("X-Width", String.valueOf(result.getWidth()))
                .header("X-Height", String.valueOf(result.getHeight()))
                .body(imageBytes);
    }
    
    @GetMapping("/sizes")
    public ResponseEntity<Result<List<PhotoSizeDTO>>> getSupportedPhotoSizes() {
        List<PhotoSizeDTO> sizes = photoStandardizationService.getSupportedPhotoSizes();
        return ResponseEntity.ok(Result.success(sizes));
    }
    
    @GetMapping("/backgrounds")
    public ResponseEntity<Result<List<String>>> getSupportedBackgroundColors() {
        List<String> colors = photoStandardizationService.getSupportedBackgroundColors();
        return ResponseEntity.ok(Result.success(colors));
    }
}
