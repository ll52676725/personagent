package com.example.agentplatform.tools.service;

import com.example.agentplatform.tools.dto.AudioFormatInfoDTO;
import com.example.agentplatform.tools.dto.SpeechToTextRequestDTO;
import com.example.agentplatform.tools.dto.SpeechToTextResultDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SpeechToTextService {

    SpeechToTextResultDTO transcribe(MultipartFile file, SpeechToTextRequestDTO request);

    List<AudioFormatInfoDTO> getSupportedFormats();
}
