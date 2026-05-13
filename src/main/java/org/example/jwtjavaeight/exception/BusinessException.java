package org.example.jwtjavaeight.exception;

import org.example.jwtjavaeight.enums.ErrorCode;

public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    private final int httpStatus;
    private String customMessage;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.httpStatus = errorCode.getCode() / 100;
    }

    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
        this.httpStatus = errorCode.getCode() / 100;
        this.customMessage = customMessage;
    }

    public static BusinessException of(ErrorCode errorCode, String customMessage) {
        return new BusinessException(errorCode, customMessage);
    }

    @Override
    public String getMessage() {
        return customMessage != null ? customMessage : super.getMessage();
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
