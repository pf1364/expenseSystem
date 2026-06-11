package com.enpenseSystem.utils;

/**
 * 统一响应业务码及默认提示。
 */
public final class ResultCodeConstants {

    /** 工具常量类不允许实例化。 */
    private ResultCodeConstants() {
    }

    public static final Integer SUCCESS_CODE = 200;
    public static final String SUCCESS_MESSAGE = "success";

    public static final Integer ERROR_CODE = 500;
    public static final String ERROR_MESSAGE = "error";

    public static final Integer PARAM_ERROR_CODE = 400;
    public static final String PARAM_ERROR_MESSAGE = "参数错误";

    public static final Integer NOT_FOUND_CODE = 404;
    public static final String NOT_FOUND_MESSAGE = "资源不存在";
}
