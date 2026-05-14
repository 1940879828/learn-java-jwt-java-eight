package org.example.jwtjavaeight.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import org.example.jwtjavaeight.common.Result;
import org.example.jwtjavaeight.enums.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理 @Valid 校验失败异常（JSON请求体）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> {
                String field = error.getField();
                String msg = error.getDefaultMessage();
                Object rejectedValue = error.getRejectedValue();
                if (rejectedValue != null && !rejectedValue.toString().isEmpty()) {
                    return String.format("%s: %s (当前值: %s)", field, msg, rejectedValue);
                }
                return String.format("%s: %s", field, msg);
            })
            .collect(Collectors.joining("; "));
        log.warn("[ValidationError] 字段校验失败: {}", message);
        return ResponseEntity.status(400)
            .body(Result.error(ErrorCode.VALIDATION_FAILED, message));
    }

    /**
     * 处理表单绑定错误
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> String.format("%s: %s", error.getField(), error.getDefaultMessage()))
            .collect(Collectors.joining("; "));
        log.warn("[BindError] 表单绑定失败: {}", message);
        return ResponseEntity.status(400)
            .body(Result.error(ErrorCode.VALIDATION_FAILED, message));
    }

    /**
     * 处理Bean Validation约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
            .map(violation -> {
                String propertyPath = violation.getPropertyPath().toString();
                String msg = violation.getMessage();
                return String.format("%s: %s", propertyPath, msg);
            })
            .collect(Collectors.joining("; "));
        log.warn("[ConstraintViolation] 约束校验失败: {}", message);
        return ResponseEntity.status(400)
            .body(Result.error(ErrorCode.VALIDATION_FAILED, message));
    }

    /**
     * 处理JSON请求体解析错误
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();
        String message = "请求数据格式错误";

        if (cause instanceof UnrecognizedPropertyException) {
            UnrecognizedPropertyException upe = (UnrecognizedPropertyException) cause;
            String fieldName = upe.getPropertyName();
            message = String.format("未知字段: '%s'", fieldName);
            log.warn("[JsonParseError] 请求包含未知字段: {}", fieldName);

        } else if (cause instanceof InvalidFormatException) {
            InvalidFormatException ife = (InvalidFormatException) cause;
            String fieldName = ife.getPath().isEmpty() ? "unknown" : ife.getPath().get(0).getFieldName();
            String targetType = ife.getTargetType().getSimpleName();
            message = String.format("字段 '%s' 格式错误，期望类型: %s", fieldName, targetType);
            log.warn("[JsonParseError] 字段格式错误: {} -> {}", fieldName, targetType);

        } else if (cause instanceof MismatchedInputException) {
            MismatchedInputException mie = (MismatchedInputException) cause;
            if (!mie.getPath().isEmpty()) {
                String fieldName = mie.getPath().get(0).getFieldName();
                message = String.format("字段 '%s' 值不匹配或缺失", fieldName);
                log.warn("[JsonParseError] 字段值不匹配: {}", fieldName);
            } else {
                message = "请求体格式错误";
                log.warn("[JsonParseError] 请求体格式错误");
            }
        } else {
            log.warn("[JsonParseError] JSON解析失败: {}", ex.getMessage());
        }

        return ResponseEntity.status(400)
            .body(Result.error(ErrorCode.VALIDATION_FAILED, message));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException ex) {
        log.warn("Business exception: {}", ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus())
            .body(Result.error(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Result<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(404)
            .body(Result.error(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<Void>> handleAuth(AuthenticationException ex) {
        return ResponseEntity.status(401)
            .body(Result.error(ErrorCode.UNAUTHORIZED, "认证失败"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccess(AccessDeniedException ex) {
        return ResponseEntity.status(403)
            .body(Result.error(ErrorCode.FORBIDDEN, "权限不足"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleGeneric(Exception ex, HttpServletRequest request) {
        String traceId = MDC.get("traceId");
        log.error("Unexpected error [traceId={}]: ", traceId, ex);
        return ResponseEntity.status(500)
            .body(Result.error(ErrorCode.INTERNAL_ERROR, "系统异常，请联系管理员"));
    }
}
