package com.enpenseSystem.exception;

/**
 * 业务异常基类，携带 HTTP 状态码和业务错误码。
 *
 * <p>子类按场景区分：ResourceNotFoundException（404）、
 * StatusConflictException（409）等。GlobalExceptionHandler
 * 根据 httpStatus 返回对应的 HTTP 响应状态。</p>
 */
public class BusinessException extends RuntimeException {

    private final int httpStatus;
    private final int bizCode;

    public BusinessException(int httpStatus, int bizCode, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.bizCode = bizCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public int getBizCode() {
        return bizCode;
    }
}
