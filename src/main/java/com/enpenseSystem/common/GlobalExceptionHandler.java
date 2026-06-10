package com.enpenseSystem.common;

import com.enpenseSystem.utils.ResultCodeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(IllegalArgumentException.class)
    public Result handleIllegalArgumentException(IllegalArgumentException exception) {
        return Result.fail(ResultCodeConstants.PARAM_ERROR_CODE, exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result handleException(Exception exception) {
        log.error("Unhandled server exception", exception);
        return Result.fail(ResultCodeConstants.ERROR_CODE, ResultCodeConstants.ERROR_MESSAGE);
    }
}
