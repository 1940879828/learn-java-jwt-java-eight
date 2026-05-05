package org.example.jwtjavaeight.exception;

import org.example.jwtjavaeight.common.Result;
import org.example.jwtjavaeight.common.ResultCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 全局异常处理器 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /** 业务异常 */
  @ExceptionHandler(BusinessException.class)
  public Result<Void> handleBusinessException(BusinessException e) {
    log.warn("[业务异常] {}", e.getMessage());
    return Result.failure(e.getCode(), e.getMessage());
  }

  /** 用户名或密码错误 */
  @ExceptionHandler(BadCredentialsException.class)
  public Result<Void> handleBadCredentialsException(BadCredentialsException e) {
    log.warn("[认证失败] 用户名或密码错误");
    return Result.failure(
        ResultCode.BAD_CREDENTIALS.getCode(), ResultCode.BAD_CREDENTIALS.getMessage());
  }

  /** 参数校验异常 (RequestBody) */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
    FieldError fieldError = e.getBindingResult().getFieldError();
    String message = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
    log.warn("[参数校验失败] {}", message);
    return Result.failure(400, message);
  }

  /** 参数绑定异常 (Form) */
  @ExceptionHandler(BindException.class)
  public Result<Void> handleBindException(BindException e) {
    FieldError fieldError = e.getBindingResult().getFieldError();
    String message = fieldError != null ? fieldError.getDefaultMessage() : "参数绑定失败";
    log.warn("[参数绑定失败] {}", message);
    return Result.failure(400, message);
  }

  /** 系统异常 */
  @ExceptionHandler(Exception.class)
  public Result<Void> handleException(Exception e) {
    log.error("[系统异常] ", e);
    return Result.failure(ResultCode.ERROR.getCode(), "系统异常，请联系管理员");
  }
}
