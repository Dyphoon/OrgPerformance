package com.cmbchina.orgperformance.controller;

import com.cmbchina.orgperformance.service.TemplateValidationException;
import com.cmbchina.orgperformance.vo.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理模板校验异常
     */
    @ExceptionHandler(TemplateValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> handleTemplateValidationException(TemplateValidationException ex) {
        return new ApiResponse<>(400, "Template validation failed", Map.of(
                "valid", false,
                "details", ex.getDetailedReport()
        ));
    }

    /**
     * 处理文件上传大小超限
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        return ApiResponse.error(400, "File size exceeds maximum limit");
    }

    /**
     * 处理通用运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleRuntimeException(RuntimeException ex) {
        String message = ex.getMessage();
        if (message == null || message.isEmpty()) {
            message = "An error occurred";
        }
        return ApiResponse.error(400, message);
    }
}
