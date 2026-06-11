package com.enpenseSystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 报销单行程实体，对应数据库表 fk_reim_itinerary。
 *
 * <p>一张报销单可以包含多条行程；一条行程又可以包含多条每日补助。</p>
 */
@Data
@TableName("fk_reim_itinerary")
public class FkReimItinerary {

    @TableId(type = IdType.AUTO)
    private Long id; // 主键ID
    private Long mainId; // 报销单主表ID
    private String travelerId; // 出行人ID
    private String travelerNo; // 出行人工号
    private String travelerName; // 出行人姓名
    private String startCityCode; // 出发城市编码
    private String startCityName; // 出发城市名称
    private String endCityCode; // 到达城市编码
    private String endCityName; // 到达城市名称
    private LocalDate startDate; // 出发日期
    private LocalDate endDate; // 到达日期
    private Integer days; // 行程天数
    private String routeText; // 行程展示文本
    private String description; // 行程说明
    private LocalDateTime createdAt; // 创建时间
    private LocalDateTime updatedAt; // 更新时间
}
