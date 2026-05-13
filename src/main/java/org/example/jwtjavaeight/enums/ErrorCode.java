package org.example.jwtjavaeight.enums;

public enum ErrorCode {
    SUCCESS(200, "操作成功"),

    BAD_REQUEST(40000, "请求参数错误"),
    VALIDATION_FAILED(40001, "参数校验失败"),

    UNAUTHORIZED(40100, "未登录或Token无效"),
    REFRESH_TOKEN_INVALID(40101, "Refresh Token无效"),

    FORBIDDEN(40300, "权限不足"),

    NOT_FOUND(40400, "资源不存在"),
    USER_NOT_FOUND(40401, "用户不存在"),
    ROLE_NOT_FOUND(40402, "角色不存在"),
    MENU_NOT_FOUND(40403, "菜单不存在"),

    CONFLICT(40900, "资源冲突"),
    DUPLICATE_RESOURCE(40901, "资源已存在"),
    RESOURCE_IN_USE(40902, "资源被引用，无法删除"),

    LOCKED(42300, "账户已被锁定"),

    TOO_MANY_REQUESTS(42900, "请求过于频繁"),

    INTERNAL_ERROR(50000, "服务器内部错误");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
