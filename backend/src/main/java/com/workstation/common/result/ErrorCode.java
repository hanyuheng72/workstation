package com.workstation.common.result;

public enum ErrorCode {

    OK(0, "ok"),

    BAD_REQUEST(400, "请求参数有误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有权限"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "数据冲突"),
    INTERNAL(500, "服务器内部错误"),

    VALIDATION_FAILED(4000, "参数校验未通过"),

    AI_NOT_CONFIGURED(1001, "尚未配置 DeepSeek API Key"),
    AI_CALL_FAILED(1002, "AI 服务调用失败"),
    AI_PARSE_FAILED(1003, "没能理解这句话，请换个说法");

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
