package com.example.NFC_system.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 确保上传等 API 异常返回 JSON 格式的错误信息，避免返回 HTML 导致前端 Network Error
 */
@RestControllerAdvice(basePackages = "com.example.NFC_system.controller")
public class GlobalExceptionHandler {

    /**
     * 处理文件上传大小超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", "File size exceeds limit (max 100MB), please compress and retry");
        result.put("error", "MaxUploadSizeExceeded");
        System.err.println("⚠️ Upload size exceeded: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(result);
    }

    /**
     * IO exception (file save failure etc.)
     */
    @ExceptionHandler(java.io.IOException.class)
    public ResponseEntity<Map<String, Object>> handleIOException(java.io.IOException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", "File operation failed: " + e.getMessage());
        result.put("error", "IOException");
        System.err.println("⚠️ IO exception: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

    /**
     * Illegal argument exception
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", "Invalid parameter: " + e.getMessage());
        result.put("error", "IllegalArgument");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * Runtime exception
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("message", "Internal server error: " + e.getMessage());
        result.put("error", e.getClass().getSimpleName());
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }
}