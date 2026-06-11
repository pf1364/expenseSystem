package com.enpenseSystem.common;

import com.enpenseSystem.utils.ResultCodeConstants;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 所有 HTTP 接口统一使用的响应结构。
 *
 * <p>code 表示业务结果，message 表示提示信息，data 承载实际数据。
 * Controller 不直接返回 Entity，通常先转换为 DTO/VO 再放入 data。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result {

    private Integer code; // 业务状态码：200 成功、400 参数错误、500 系统错误
    private String message; // 成功或失败提示
    private Object data; // 接口实际返回数据，无数据时为 null

    /** 创建不携带业务数据的成功响应。 */
    public static Result ok() {
        return new Result(ResultCodeConstants.SUCCESS_CODE, ResultCodeConstants.SUCCESS_MESSAGE, null);
    }

    /** 创建携带业务数据的成功响应。 */
    public static Result ok(Object data) {
        return new Result(ResultCodeConstants.SUCCESS_CODE, ResultCodeConstants.SUCCESS_MESSAGE, data);
    }

    /** 使用默认系统错误码创建失败响应。 */
    public static Result fail(String message) {
        return new Result(ResultCodeConstants.ERROR_CODE, message, null);
    }

    /** 使用指定业务错误码创建失败响应。 */
    public static Result fail(Integer code, String message) {
        return new Result(code, message, null);
    }
}

