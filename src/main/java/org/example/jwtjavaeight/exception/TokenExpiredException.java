package org.example.jwtjavaeight.exception;

import org.example.jwtjavaeight.enums.ErrorCode;

/** Token过期异常 */
public class TokenExpiredException extends BusinessException {

  public TokenExpiredException() {
    super(ErrorCode.UNAUTHORIZED, "Token已过期");
  }

  public TokenExpiredException(String message) {
    super(ErrorCode.UNAUTHORIZED, message);
  }
}
