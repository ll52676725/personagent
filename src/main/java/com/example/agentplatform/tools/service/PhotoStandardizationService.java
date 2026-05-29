package com.example.agentplatform.tools.service;

import com.example.agentplatform.tools.dto.PhotoSizeDTO;
import com.example.agentplatform.tools.dto.PhotoStandardizationRequestDTO;
import com.example.agentplatform.tools.dto.PhotoStandardizationResultDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PhotoStandardizationService {
    
    PhotoStandardizationResultDTO standardize(MultipartFile file, PhotoStandardizationRequestDTO request);
    
    List<PhotoSizeDTO> getSupportedPhotoSizes();
    
    List<String> getSupportedBackgroundColors();
}
