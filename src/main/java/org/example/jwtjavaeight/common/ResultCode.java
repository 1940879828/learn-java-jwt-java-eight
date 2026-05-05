package org.example.jwtjavaeight.common;

public enum ResultCode {
  SUCCESS(200, "操作成功"),
  BAD_CREDENTIALS(400, "用户名或密码错误"),
  USERNAME_EXISTS(400, "用户名已存在"),
  UNAUTHORIZED(401, "未登录或Token已过期"),
  FORBIDDEN(403, "没有权限"),
  ERROR(500, "系统异常");

  private final Integer code;
  private final String message;

  ResultCode(Integer code, String message) {
    this.code = code;
    this.message = message;
  }

  public Integer getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }
}
