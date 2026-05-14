package org.example.jwtjavaeight.exception;

import lombok.Getter;
import org.example.jwtjavaeight.enums.ErrorCode;

public class BusinessException extends RuntimeException {
    @Getter
    private final ErrorCode errorCode;
    @Getter
    private final int httpStatus;
    private final String customMessage;

    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
        this.httpStatus = errorCode.getCode() / 100;
        this.customMessage = customMessage;
    }

    @Override
    public String getMessage() {
        return customMessage != null ? customMessage : super.getMessage();
    }

}
