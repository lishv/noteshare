package com.note.exception;

import com.note.enums.ErrorCode;
import com.note.vo.Result;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return Result.error(ErrorCode.BAD_REQUEST.getCode(), message);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<?> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        log.warn("上传文件大小超限: {}", e.getMessage());
        return Result.error(ErrorCode.BAD_REQUEST.getCode(), "上传文件过大，单张图片请控制在 10MB 以内");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<?> handleTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        log.warn("参数类型转换失败, 请求路径: {}, 参数名: {}, 值: {}", request.getRequestURI(), e.getName(), e.getValue());
        return Result.error(ErrorCode.BAD_REQUEST.getCode(), "参数格式错误");
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常, 请求路径: {}", request.getRequestURI(), e);
        return Result.error(ErrorCode.INTERNAL_ERROR.getCode(), "服务器内部错误");
    }
}
