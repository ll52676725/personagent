package com.example.agentplatform.tools.controller;

import com.example.agentplatform.common.dto.Result;
import com.example.agentplatform.tools.dto.AudioFormatInfoDTO;
import com.example.agentplatform.tools.dto.SpeechToTextRequestDTO;
import com.example.agentplatform.tools.dto.SpeechToTextResultDTO;
import com.example.agentplatform.tools.service.SpeechToTextService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/agent-tools/speech")
@RequiredArgsConstructor
public class SpeechToolController {

    private final SpeechToTextService speechToTextService;

    @PostMapping("/transcribe")
    public ResponseEntity<Result<SpeechToTextResultDTO>> transcribe(
            @RequestParam("file") MultipartFile file,
            @Valid @ModelAttribute SpeechToTextRequestDTO request) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Result.badRequest("请选择要转写的音频文件"));
        }

        log.info("[语音转文字] 收到转写请求: fileName={}, size={}, language={}",
                file.getOriginalFilename(), file.getSize(), request.getLanguage());

        SpeechToTextResultDTO result = speechToTextService.transcribe(file, request);
        return ResponseEntity.ok(Result.success("语音转文字完成", result));
    }

    @GetMapping("/formats")
    public ResponseEntity<Result<List<AudioFormatInfoDTO>>> getSupportedFormats() {
        List<AudioFormatInfoDTO> formats = speechToTextService.getSupportedFormats();
        return ResponseEntity.ok(Result.success(formats));
    }
}
