package com.enpenseSystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("fk_city_allowance")
public class FkCityAllowance {

    @TableId(type = IdType.AUTO)
    private Long id; // 主键ID
    private String cityCode; // 城市编码
    private String cityName; // 城市名称
    private Integer cityLevel; // 城市等级：1一线，2二线，3三线
    private String cityLevelName; // 城市等级名称
    private BigDecimal mealStandard; // 餐费补助标准金额
    private BigDecimal trafficStandard; // 交通补助标准金额
    private BigDecimal communicationStandard; // 通讯补助标准金额
    private LocalDateTime createdAt; // 创建时间
    private LocalDateTime updatedAt; // 更新时间
}
