package com.workstation.common.exception;

import com.workstation.common.result.ApiResponse;
import com.workstation.common.result.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        ErrorCode code = ex.getErrorCode();
        // 业务异常是预期内的，用 warn 且不打堆栈
        log.warn("业务异常 code={} message={}", code.getCode(), ex.getMessage());
        return ResponseEntity.status(httpStatusOf(code))
                .body(ApiResponse.fail(code, ex.getMessage()));
    }

    /** @Valid 校验失败：把字段错误拼成一句可读的中文提示。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(this::describeFieldError)
                .collect(Collectors.joining("；"));
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ErrorCode.VALIDATION_FAILED, detail));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraint(ConstraintViolationException ex) {
        String detail = ex.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .collect(Collectors.joining("；"));
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ErrorCode.VALIDATION_FAILED, detail));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleMalformedRequest(Exception ex) {
        log.warn("请求格式有误: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ErrorCode.BAD_REQUEST, "请求格式有误，请检查参数"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail(ErrorCode.FORBIDDEN));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.fail(ErrorCode.BAD_REQUEST, "该地址不支持此请求方式"));
    }

    /**
     * 静态资源缺失。SPA 的前端路由由 WebConfig 的 fallback 兜底，
     * 走到这里说明确实找不到，返回 404 而非 500。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(ErrorCode.NOT_FOUND));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        // 非预期异常要留完整堆栈，但不回显给前端
        log.error("未预期的异常", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ErrorCode.INTERNAL));
    }

    private String describeFieldError(FieldError error) {
        String message = error.getDefaultMessage();
        return message == null ? error.getField() + " 不合法" : message;
    }

    private HttpStatus httpStatusOf(ErrorCode code) {
        return switch (code) {
            case BAD_REQUEST, VALIDATION_FAILED -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            default -> HttpStatus.OK;
        };
    }
}
