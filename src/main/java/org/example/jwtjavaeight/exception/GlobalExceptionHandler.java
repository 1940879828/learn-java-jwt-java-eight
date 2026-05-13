package org.example.jwtjavaeight.exception;

import org.example.jwtjavaeight.common.Result;
import org.example.jwtjavaeight.enums.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
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
