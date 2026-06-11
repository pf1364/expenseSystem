package com.enpenseSystem.common;

import com.enpenseSystem.utils.ResultCodeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * REST 接口统一异常处理器。
 *
 * <p>业务校验当前通过 IllegalArgumentException 表达，转换为 code=400；
 * 其他异常记录完整日志，对客户端仅返回通用错误，避免泄露内部堆栈。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public Result handleIllegalArgumentException(IllegalArgumentException exception) {
        // 业务错误信息可以直接展示给用户，例如“餐补不能超过标准金额100.00”。
        return Result.fail(ResultCodeConstants.PARAM_ERROR_CODE, exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result handleException(Exception exception) {
        // 未预期异常必须记录堆栈，响应只暴露通用信息。
        log.error("Unhandled server exception", exception);
        return Result.fail(ResultCodeConstants.ERROR_CODE, ResultCodeConstants.ERROR_MESSAGE);
    }
}
