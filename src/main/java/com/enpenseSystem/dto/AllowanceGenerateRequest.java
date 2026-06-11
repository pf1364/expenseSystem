package com.enpenseSystem.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 生成每日补助的请求参数。
 *
 * <p>当前补助标准取到达城市，出发城市主要用于保留完整行程语义。
 * 日期范围包含开始日期和结束日期。</p>
 */
@Data
public class AllowanceGenerateRequest {

    private String startCityCode; // 出发城市编码
    private String startCityName; // 出发城市名称
    private String endCityCode; // 到达城市编码，优先用于查询补助标准
    private String endCityName; // 到达城市名称，编码为空时作为查询条件
    private LocalDate startDate; // 行程开始日期
    private LocalDate endDate; // 行程结束日期
}
