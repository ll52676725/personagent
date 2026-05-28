package com.example.agentplatform.tools.service;

import com.example.agentplatform.tools.dto.ImageConvertRequestDTO;
import com.example.agentplatform.tools.dto.ImageConvertResultDTO;
import com.example.agentplatform.tools.dto.ImageFormatInfoDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ImageConvertService {

    ImageConvertResultDTO convert(MultipartFile file, ImageConvertRequestDTO request);

    List<ImageFormatInfoDTO> getSupportedFormats();
}
