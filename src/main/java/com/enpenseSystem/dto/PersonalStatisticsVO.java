package com.enpenseSystem.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 个人报销统计返回对象。
 *
 * <p>同时承载页面汇总卡片、月度柱状图、公司占比饼图和最近报销单列表的数据。</p>
 */
@Data
public class PersonalStatisticsVO {

    private Long totalCount = 0L; // 非作废报销单总数
    private Long draftCount = 0L; // 草稿数量
    private Long submittedCount = 0L; // 已提交数量
    private BigDecimal totalAmount = BigDecimal.ZERO; // 非作废单据补助金额合计
    private List<MonthlyAmount> monthlyAmounts = new ArrayList<>(); // 按创建月份汇总的金额
    private List<CompanyShare> companyShares = new ArrayList<>(); // 按费用归属公司汇总的金额
    private List<ReimbursementPageVO> recentBills = new ArrayList<>(); // 最近十张报销单

    /** 月度柱状图的单个数据点。 */
    @Data
    public static class MonthlyAmount {
        private String month; // 月份，格式 yyyy-MM
        private BigDecimal amount; // 该月补助金额

        public MonthlyAmount(String month, BigDecimal amount) {
            this.month = month;
            this.amount = amount;
        }
    }

    /** 公司金额占比饼图的单个数据点。 */
    @Data
    public static class CompanyShare {
        private String companyName; // 费用归属公司名称
        private BigDecimal amount; // 该公司累计分摊金额

        public CompanyShare(String companyName, BigDecimal amount) {
            this.companyName = companyName;
            this.amount = amount;
        }
    }
}
