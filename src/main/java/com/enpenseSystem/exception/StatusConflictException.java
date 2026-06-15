package com.enpenseSystem.exception;

/**
 * 状态冲突异常，对应 HTTP 409。
 * 例如：草稿已提交不能重复提交、已作废单据不能修改。
 */
public class StatusConflictException extends BusinessException {

    public StatusConflictException(String message) {
        super(409, 409, message);
    }
}
