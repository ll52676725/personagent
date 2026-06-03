package com.example.agentplatform.tools.service.impl;

import com.example.agentplatform.tools.dto.AudioFormatInfoDTO;
import com.example.agentplatform.tools.dto.SpeechToTextRequestDTO;
import com.example.agentplatform.tools.dto.SpeechToTextResultDTO;
import com.example.agentplatform.tools.service.SpeechToTextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

@Slf4j
@Service
public class SpeechToTextServiceImpl implements SpeechToTextService {

    private static final long MAX_FILE_SIZE = 25 * 1024 * 1024;

    private static final List<AudioFormatInfoDTO> SUPPORTED_FORMATS = List.of(
            AudioFormatInfoDTO.builder()
                    .formatName("MP3").extension("mp3").mimeType("audio/mpeg")
                    .description("MP3 音频，最常见的压缩格式，手机录音常用")
                    .maxFileSize(MAX_FILE_SIZE).build(),
            AudioFormatInfoDTO.builder()
                    .formatName("WAV").extension("wav").mimeType("audio/wav")
                    .description("WAV 无损音频，文件较大但质量最高")
                    .maxFileSize(MAX_FILE_SIZE).build(),
            AudioFormatInfoDTO.builder()
                    .formatName("M4A").extension("m4a").mimeType("audio/mp4")
                    .description("M4A 音频，iPhone 录音默认格式")
                    .maxFileSize(MAX_FILE_SIZE).build(),
            AudioFormatInfoDTO.builder()
                    .formatName("AAC").extension("aac").mimeType("audio/aac")
                    .description("AAC 音频，Android 录音常用格式")
                    .maxFileSize(MAX_FILE_SIZE).build(),
            AudioFormatInfoDTO.builder()
                    .formatName("OGG").extension("ogg").mimeType("audio/ogg")
                    .description("OGG 音频，开源格式，微信录音常用")
                    .maxFileSize(MAX_FILE_SIZE).build(),
            AudioFormatInfoDTO.builder()
                    .formatName("FLAC").extension("flac").mimeType("audio/flac")
                    .description("FLAC 无损音频，音质最佳但文件最大")
                    .maxFileSize(MAX_FILE_SIZE).build(),
            AudioFormatInfoDTO.builder()
                    .formatName("AMR").extension("amr").mimeType("audio/amr")
                    .description("AMR 音频，手机通话录音常用格式")
                    .maxFileSize(MAX_FILE_SIZE).build(),
            AudioFormatInfoDTO.builder()
                    .formatName("WMA").extension("wma").mimeType("audio/x-ms-wma")
                    .description("WMA 音频，Windows Media 格式")
                    .maxFileSize(MAX_FILE_SIZE).build(),
            AudioFormatInfoDTO.builder()
                    .formatName("WebM").extension("webm").mimeType("audio/webm")
                    .description("WebM 音频，网页录音常用格式")
                    .maxFileSize(MAX_FILE_SIZE).build()
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "mp3", "wav", "m4a", "aac", "ogg", "flac", "amr", "wma", "webm"
    );

    private final RestTemplate whisperRestTemplate;

    @Value("${agent.platform.speech.whisper.base-url:https://api.openai.com/v1}")
    private String whisperBaseUrl;

    @Value("${agent.platform.speech.whisper.api-key:}")
    private String whisperApiKey;

    @Value("${agent.platform.speech.whisper.model:whisper-1}")
    private String whisperModel;

    public SpeechToTextServiceImpl(RestTemplateBuilder restTemplateBuilder) {
        this.whisperRestTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(30))
                .setReadTimeout(Duration.ofSeconds(120))
                .build();
    }

    @Override
    public SpeechToTextResultDTO transcribe(MultipartFile file, SpeechToTextRequestDTO request) {
        String originalFileName = file.getOriginalFilename();
        long originalSize = file.getSize();
        String audioFormat = extractExtension(originalFileName);

        log.info("[语音转文字] 开始处理音频文件: fileName={}, size={}, format={}",
                originalFileName, originalSize, audioFormat);

        validateFile(file);

        String model = StringUtils.hasText(request.getModel()) ? request.getModel() : whisperModel;
        String language = request.getLanguage();
        Double temperature = request.getTemperature() != null ? request.getTemperature() : 0.0;

        log.debug("[语音转文字] 转写参数: model={}, language={}, temperature={}",
                model, language, temperature);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            if (StringUtils.hasText(whisperApiKey)) {
                headers.setBearerAuth(whisperApiKey);
            }

            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return originalFileName;
                }
            };

            MultiValueMap<String, Object> formData = new LinkedMultiValueMap<>();
            formData.add("file", fileResource);
            formData.add("model", model);

            if (StringUtils.hasText(language)) {
                formData.add("language", language);
            }
            if (StringUtils.hasText(request.getPrompt())) {
                formData.add("prompt", request.getPrompt());
            }
            formData.add("temperature", temperature.toString());

            if (Boolean.TRUE.equals(request.getVerboseJson())) {
                formData.add("response_format", "verbose_json");
            } else {
                formData.add("response_format", "json");
            }

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(formData, headers);

            String endpoint = whisperBaseUrl + "/audio/transcriptions";
            log.debug("[语音转文字] 请求 Whisper API: endpoint={}", endpoint);

            ResponseEntity<Map> response = whisperRestTemplate.exchange(
                    endpoint,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null) {
                log.error("[语音转文字] Whisper API 返回空响应");
                throw new RuntimeException("语音转文字服务返回空响应");
            }

            String transcribedText = (String) responseBody.get("text");
            String detectedLanguage = (String) responseBody.get("language");
            Double duration = responseBody.get("duration") != null
                    ? ((Number) responseBody.get("duration")).doubleValue()
                    : null;

            log.info("[语音转文字] 转写完成: textLength={}, detectedLanguage={}, duration={}s",
                    transcribedText != null ? transcribedText.length() : 0,
                    detectedLanguage,
                    duration);

            return SpeechToTextResultDTO.builder()
                    .text(transcribedText)
                    .language(language)
                    .detectedLanguage(detectedLanguage)
                    .duration(duration)
                    .model(model)
                    .originalFileName(originalFileName)
                    .originalSize(originalSize)
                    .audioFormat(audioFormat)
                    .durationMs(duration != null ? (long) (duration * 1000) : null)
                    .build();

        } catch (IOException e) {
            log.error("[语音转文字] 读取音频文件失败: {}", e.getMessage(), e);
            throw new RuntimeException("读取音频文件失败: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("[语音转文字] 调用 Whisper API 失败: {}", e.getMessage(), e);

            if (e.getMessage() != null && e.getMessage().contains("401")) {
                throw new RuntimeException("Whisper API 认证失败，请检查 API Key 配置", e);
            }
            if (e.getMessage() != null && e.getMessage().contains("413")) {
                throw new RuntimeException("音频文件超过 Whisper API 大小限制（25MB）", e);
            }
            throw new RuntimeException("语音转文字失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<AudioFormatInfoDTO> getSupportedFormats() {
        log.debug("[语音转文字] 获取支持的音频格式列表");
        return SUPPORTED_FORMATS;
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("音频文件不能为空");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    String.format("音频文件大小 %d bytes 超过最大限制 %d bytes（25MB）",
                            file.getSize(), MAX_FILE_SIZE));
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException(
                    String.format("不支持的音频格式: .%s，支持格式: %s",
                            extension, String.join(", ", ALLOWED_EXTENSIONS)));
        }
    }

    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return null;
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}
