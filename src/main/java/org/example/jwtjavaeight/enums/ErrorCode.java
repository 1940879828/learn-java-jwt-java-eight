package org.example.jwtjavaeight.enums;

import lombok.Getter;

@Getter
public enum ErrorCode {
    SUCCESS(200, "操作成功"),

    VALIDATION_FAILED(40001, "参数校验失败"),

    UNAUTHORIZED(40100, "未登录或Token无效"),

    FORBIDDEN(40300, "权限不足"),

    NOT_FOUND(40400, "资源不存在"),

    DUPLICATE_RESOURCE(40901, "资源已存在"),
    RESOURCE_IN_USE(40902, "资源被引用，无法删除"),

    INTERNAL_ERROR(50000, "服务器内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
