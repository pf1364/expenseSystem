package com.enpenseSystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日补助明细实体，对应数据库表 fk_reim_allowance_day。
 *
 * <p>每条记录表示某段行程中的一天，同时保存城市标准快照、
 * 用户勾选状态、实报金额和后端计算的当日合计。</p>
 */
@Data
@TableName("fk_reim_allowance_day")
public class FkReimAllowanceDay {

    @TableId(type = IdType.AUTO)
    private Long id; // 主键ID
    private Long mainId; // 报销单主表ID
    private Long itineraryId; // 行程表ID
    private LocalDate allowanceDate; // 补助日期
    private String weekName; // 星期名称
    private String cityCode; // 补助城市编码
    private String cityName; // 补助城市名称
    private Integer cityLevel; // 城市等级：1一线，2二线，3三线
    private BigDecimal mealStandard; // 餐费补助标准金额
    private Integer mealSelected; // 是否选择餐费补助：1是，0否
    private BigDecimal mealAmount; // 实际餐费补助金额
    private BigDecimal trafficStandard; // 交通补助标准金额
    private Integer trafficSelected; // 是否选择交通补助：1是，0否
    private BigDecimal trafficAmount; // 实际交通补助金额
    private BigDecimal communicationStandard; // 通讯补助标准金额
    private Integer communicationSelected; // 是否选择通讯补助：1是，0否
    private BigDecimal communicationAmount; // 实际通讯补助金额
    private BigDecimal dayAmount; // 当日补助合计
    private LocalDateTime createdAt; // 创建时间
    private LocalDateTime updatedAt; // 更新时间
}
