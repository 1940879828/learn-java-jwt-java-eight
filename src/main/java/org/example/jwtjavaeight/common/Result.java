package org.example.jwtjavaeight.common;

import org.example.jwtjavaeight.enums.ErrorCode;
import org.slf4j.MDC;

import java.io.Serializable;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Result<T> implements Serializable {

  private static final long serialVersionUID = 1L;

  private Integer code;
  private String message;
  private T data;
  private String traceId;
  private OffsetDateTime timestamp;

  public static <T> Result<T> success(T data) {
    Result<T> result = new Result<>();
    result.setCode(ErrorCode.SUCCESS.getCode());
    result.setMessage(ErrorCode.SUCCESS.getMessage());
    result.setData(data);
    result.setTraceId(MDC.get("traceId"));
    result.setTimestamp(OffsetDateTime.now());
    return result;
  }

  public static <T> Result<T> success() {
    Result<T> result = new Result<>();
    result.setCode(ErrorCode.SUCCESS.getCode());
    result.setMessage(ErrorCode.SUCCESS.getMessage());
    result.setTraceId(MDC.get("traceId"));
    result.setTimestamp(OffsetDateTime.now());
    return result;
  }

  public static <T> Result<T> failure(Integer code, String message) {
    Result<T> result = new Result<>();
    result.setCode(code);
    result.setMessage(message);
    result.setTraceId(MDC.get("traceId"));
    result.setTimestamp(OffsetDateTime.now());
    return result;
  }

  public static <T> Result<T> failure(Integer code, String message, T data) {
    Result<T> result = new Result<>();
    result.setCode(code);
    result.setMessage(message);
    result.setData(data);
    result.setTraceId(MDC.get("traceId"));
    result.setTimestamp(OffsetDateTime.now());
    return result;
  }

    public static <T> Result<T> error(ErrorCode errorCode, String message) {
    Result<T> result = new Result<>();
    result.setCode(errorCode.getCode());
    result.setMessage(message);
    result.setTraceId(MDC.get("traceId"));
    result.setTimestamp(OffsetDateTime.now());
    return result;
  }
}