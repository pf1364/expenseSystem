package com.enpenseSystem.common;

import com.enpenseSystem.utils.ResultCodeConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result {

    private Integer code;
    private String message;
    private Object data;

    public static Result ok() {
        return new Result(ResultCodeConstants.SUCCESS_CODE, ResultCodeConstants.SUCCESS_MESSAGE, null);
    }

    public static Result ok(Object data) {
        return new Result(ResultCodeConstants.SUCCESS_CODE, ResultCodeConstants.SUCCESS_MESSAGE, data);
    }

    public static Result fail(String message) {
        return new Result(ResultCodeConstants.ERROR_CODE, message, null);
    }

    public static Result fail(Integer code, String message) {
        return new Result(code, message, null);
    }
}

