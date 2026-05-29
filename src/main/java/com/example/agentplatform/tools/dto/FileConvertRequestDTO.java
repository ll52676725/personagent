package com.example.agentplatform.tools.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 文件格式转换请求DTO
 * 用于接收前端传递的转换请求参数
 */
@Data
public class FileConvertRequestDTO {

    /**
     * 目标格式，不能为空
     * 支持的格式：pdf, docx, doc, txt, html, xlsx, xls, csv
     */
    @NotBlank(message = "目标格式不能为空")
    private String targetFormat;
}
