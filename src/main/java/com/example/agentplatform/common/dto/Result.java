package com.example.agentplatform.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> {
    private Integer code;
    private String message;
    private T data;
    private Long timestamp;
    
    public static <T> Result<T> success(T data) {
        return Result.<T>builder()
                .code(200)
                .message("success")
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    public static <T> Result<T> success(String message, T data) {
        return Result.<T>builder()
                .code(200)
                .message(message)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    public static <T> Result<T> error(Integer code, String message) {
        return Result.<T>builder()
                .code(code)
                .message(message)
                .timestamp(System.currentTimeMillis())
                .build();
    }
    
    public static <T> Result<T> badRequest(String message) {
        return error(400, message);
    }
    
    public static <T> Result<T> unauthorized(String message) {
        return error(401, message);
    }
    
    public static <T> Result<T> forbidden(String message) {
        return error(403, message);
    }
    
    public static <T> Result<T> notFound(String message) {
        return error(404, message);
    }
    
    public static <T> Result<T> serverError(String message) {
        return error(500, message);
    }
}