package com.workstation.common.result;

/**
 * 统一响应体。成功恒为 code = 0，失败为对应业务错误码。
 */
public record ApiResponse<T>(int code, String message, T data, long timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(ErrorCode.OK.getCode(), ErrorCode.OK.getMessage(), data, System.currentTimeMillis());
    }

    public static <T> ApiResponse<T> ok() {
        return ok(null);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return fail(errorCode.getCode(), errorCode.getMessage());
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message) {
        return fail(errorCode.getCode(), message);
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null, System.currentTimeMillis());
    }
}
