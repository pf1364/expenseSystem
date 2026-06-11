package com.enpenseSystem.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 报销单创建和更新的完整请求对象。
 *
 * <p>包含主表基础信息、行程、每日补助和费用分摊。客户端传入的补助标准、
 * 当日合计、主表总额等金额结果不可信，Service 会查询数据库标准并重新计算。</p>
 */
@Data
public class ReimbursementSaveRequest {

    private String reimNo; // 报销单号，创建时可为空，更新时必传或从路径获取
    private String title; // 报销标题
    private String reimburserId; // 报销人ID，当前可为空
    private String reimburserNo; // 报销人工号
    private String reimburserName; // 报销人姓名
    private String reimDepartmentId; // 报销部门ID，当前可为空
    private String reimDepartmentNo; // 报销部门编号
    private String reimDepartmentName; // 报销部门名称
    private String reimCompanyNames; // 费用归属公司名称汇总
    private String businessTypeId; // 业务类型ID，当前可为空
    private String businessTypeNo; // 业务类型编号
    private String businessTypeName; // 业务类型名称
    private String reason; // 出差事由
    private String remark; // 备注信息
    private List<ItineraryRequest> itineraries; // 行程信息
    private List<AllocationRequest> allocations; // 费用归属及分摊信息

    /** 单条行程请求，包含该行程对应的每日补助明细。 */
    @Data
    public static class ItineraryRequest {

        private Long id; // 行程ID，创建时为空，更新时可传
        private String travelerId; // 出行人ID，当前可为空
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
        private List<AllowanceDayRequest> allowanceDays; // 当前行程对应的每日补助明细
    }

    /** 单日补助请求，记录三类补助的勾选状态和实报金额。 */
    @Data
    public static class AllowanceDayRequest {

        private Long id; // 每日补助明细ID，创建时为空，更新时可传
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
    }

    /** 单条费用归属及分摊请求。 */
    @Data
    public static class AllocationRequest {

        private Long id; // 分摊ID，创建时为空，更新时可传
        private String allocationOwnerType; // 分摊归属方类型：COMPANY公司，DEPARTMENT部门
        private String allocationOwnerId; // 分摊归属方ID，当前可为空
        private String allocationOwnerNo; // 分摊归属方编号
        private String allocationOwnerName; // 分摊归属方名称
        private String businessId; // 分摊业务ID，当前可为空
        private String businessName; // 分摊业务名称
        private BigDecimal allocationRatio; // 分摊比例，数据库存0-1
        private BigDecimal allocationAmount; // 分摊金额
        private Integer sortNo; // 排序号
    }
}
