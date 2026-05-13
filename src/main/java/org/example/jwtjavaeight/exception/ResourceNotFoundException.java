package org.example.jwtjavaeight.exception;

import org.example.jwtjavaeight.enums.ErrorCode;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String resourceType, Object id) {
        super(ErrorCode.NOT_FOUND, String.format("%s[id=%s]不存在", resourceType, id));
    }

    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
