package com.example.agentplatform.tools.service;

import com.example.agentplatform.tools.dto.FileConvertRequestDTO;
import com.example.agentplatform.tools.dto.FileConvertResultDTO;
import com.example.agentplatform.tools.dto.FileFormatInfoDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 文件格式转换服务接口
 * 定义文件格式转换的核心业务方法
 */
public interface FileConvertService {

    /**
     * 转换文件格式
     *
     * @param file    上传的文件
     * @param request 转换请求参数
     * @return 转换结果DTO
     */
    FileConvertResultDTO convert(MultipartFile file, FileConvertRequestDTO request);

    /**
     * 获取支持的文件格式列表
     *
     * @return 支持的格式信息列表
     */
    List<FileFormatInfoDTO> getSupportedFormats();
}
