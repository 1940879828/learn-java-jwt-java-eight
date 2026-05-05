package org.example.jwtjavaeight.common;

import java.io.Serializable;
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

  public static <T> Result<T> success(T data) {
    return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
  }

  public static <T> Result<T> success() {
    return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), null);
  }

  public static <T> Result<T> failure(Integer code, String message) {
    return new Result<>(code, message, null);
  }

  public static <T> Result<T> failure(Integer code, String message, T data) {
    return new Result<>(code, message, data);
  }
}