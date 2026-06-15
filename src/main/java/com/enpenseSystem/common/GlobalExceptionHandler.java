package com.enpenseSystem.common;

import com.enpenseSystem.exception.BusinessException;
import com.enpenseSystem.utils.ResultCodeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * REST 接口统一异常处理器。
 *
 * <p>业务异常按子类返回对应的 HTTP 状态码（400/404/409），
 * Bean Validation 失败返回 400 + 字段级错误信息，
 * 未预期异常返回 500，记录完整堆栈但仅暴露通用信息给客户端。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result> handleBusinessException(BusinessException e) {
        return ResponseEntity.status(e.getHttpStatus())
                .body(Result.fail(e.getBizCode(), e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result> handleIllegalArgument(IllegalArgumentException e) {
        // 业务错误信息可以直接展示给用户，例如"餐补不能超过标准金额100.00"。
        return ResponseEntity.status(400)
                .body(Result.fail(ResultCodeConstants.PARAM_ERROR_CODE, e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(400)
                .body(Result.fail(ResultCodeConstants.PARAM_ERROR_CODE, msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result> handleException(Exception exception) {
        // 未预期异常必须记录堆栈，响应只暴露通用信息。
        log.error("Unhandled server exception", exception);
        return ResponseEntity.status(500)
                .body(Result.fail(ResultCodeConstants.ERROR_CODE, ResultCodeConstants.ERROR_MESSAGE));
    }
}
