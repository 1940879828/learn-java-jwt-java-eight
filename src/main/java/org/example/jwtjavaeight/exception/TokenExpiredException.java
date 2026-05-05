package org.example.jwtjavaeight.exception;

import org.example.jwtjavaeight.common.ResultCode;

/** Token过期异常 */
public class TokenExpiredException extends BusinessException {

  public TokenExpiredException() {
    super(ResultCode.UNAUTHORIZED.getCode(), "Token已过期");
  }

  public TokenExpiredException(String message) {
    super(ResultCode.UNAUTHORIZED.getCode(), message);
  }
}
