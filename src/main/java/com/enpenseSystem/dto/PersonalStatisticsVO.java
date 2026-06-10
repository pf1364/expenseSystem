package com.enpenseSystem.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class PersonalStatisticsVO {

    private Long totalCount = 0L;
    private Long draftCount = 0L;
    private Long submittedCount = 0L;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private List<MonthlyAmount> monthlyAmounts = new ArrayList<>();
    private List<CompanyShare> companyShares = new ArrayList<>();
    private List<ReimbursementPageVO> recentBills = new ArrayList<>();

    @Data
    public static class MonthlyAmount {
        private String month;
        private BigDecimal amount;

        public MonthlyAmount(String month, BigDecimal amount) {
            this.month = month;
            this.amount = amount;
        }
    }

    @Data
    public static class CompanyShare {
        private String companyName;
        private BigDecimal amount;

        public CompanyShare(String companyName, BigDecimal amount) {
            this.companyName = companyName;
            this.amount = amount;
        }
    }
}
