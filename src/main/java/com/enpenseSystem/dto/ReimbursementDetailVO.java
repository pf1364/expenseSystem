package com.enpenseSystem.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 报销单详情返回对象。
 *
 * <p>该对象以报销单主表字段为根节点，嵌套行程、每日补助和费用分摊，
 * 用于详情展示、草稿编辑回显以及提交已有草稿时还原完整业务数据。</p>
 */
@Data
public class ReimbursementDetailVO {

    private Long id; // 报销单主表 ID
    private String reimNo; // 唯一报销单号
    private String billStatus; // 单据状态编码
    private String billStatusName; // 单据状态中文名
    private String billType; // 单据类型编码
    private String billTypeName; // 单据类型名称
    private String reimburserId; // 报销人 ID
    private String reimburserNo; // 报销人工号
    private String reimburserName; // 报销人姓名
    private String reimDepartmentId; // 报销部门 ID
    private String reimDepartmentNo; // 报销部门编号
    private String reimDepartmentName; // 报销部门名称
    private String reimCompanyNames; // 从分摊表汇总的费用归属公司名称
    private String businessTypeId; // 业务类型 ID
    private String businessTypeNo; // 业务类型编号
    private String businessTypeName; // 业务类型名称
    private String title; // 报销标题
    private String reason; // 出差事由
    private BigDecimal allowanceAmount; // 三类补助总金额
    private BigDecimal mealAmount; // 餐费补助合计
    private BigDecimal trafficAmount; // 交通补助合计
    private BigDecimal communicationAmount; // 通讯补助合计
    private String remark; // 备注
    private LocalDateTime submittedAt; // 提交时间
    private LocalDateTime createdAt; // 创建时间
    private LocalDateTime updatedAt; // 最后更新时间
    private List<ItineraryVO> itineraries = new ArrayList<>(); // 行程及其每日补助
    private List<AllocationVO> allocations = new ArrayList<>(); // 费用归属及分摊

    /** 详情中的单条行程，内部继续嵌套该行程的每日补助。 */
    @Data
    public static class ItineraryVO {
        private Long id;
        private String travelerId;
        private String travelerNo;
        private String travelerName;
        private String startCityCode;
        private String startCityName;
        private String endCityCode;
        private String endCityName;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer days;
        private String routeText;
        private String description;
        private List<AllowanceDayVO> allowanceDays = new ArrayList<>();
    }

    /** 详情中的单日补助标准、勾选状态和实报金额。 */
    @Data
    public static class AllowanceDayVO {
        private Long id;
        private LocalDate allowanceDate;
        private String weekName;
        private String cityCode;
        private String cityName;
        private Integer cityLevel;
        private BigDecimal mealStandard;
        private Integer mealSelected;
        private BigDecimal mealAmount;
        private BigDecimal trafficStandard;
        private Integer trafficSelected;
        private BigDecimal trafficAmount;
        private BigDecimal communicationStandard;
        private Integer communicationSelected;
        private BigDecimal communicationAmount;
        private BigDecimal dayAmount;
    }

    /** 详情中的单条费用归属及分摊信息。 */
    @Data
    public static class AllocationVO {
        private Long id;
        private String allocationOwnerType;
        private String allocationOwnerId;
        private String allocationOwnerNo;
        private String allocationOwnerName;
        private String businessId;
        private String businessName;
        private BigDecimal allocationRatio;
        private BigDecimal allocationAmount;
        private Integer sortNo;
    }
}
