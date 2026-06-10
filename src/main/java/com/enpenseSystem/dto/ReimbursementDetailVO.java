package com.enpenseSystem.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ReimbursementDetailVO {

    private Long id;
    private String reimNo;
    private String billStatus;
    private String billStatusName;
    private String billType;
    private String billTypeName;
    private String reimburserId;
    private String reimburserNo;
    private String reimburserName;
    private String reimDepartmentId;
    private String reimDepartmentNo;
    private String reimDepartmentName;
    private String reimCompanyNames;
    private String businessTypeId;
    private String businessTypeNo;
    private String businessTypeName;
    private String title;
    private String reason;
    private BigDecimal allowanceAmount;
    private BigDecimal mealAmount;
    private BigDecimal trafficAmount;
    private BigDecimal communicationAmount;
    private String remark;
    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ItineraryVO> itineraries = new ArrayList<>();
    private List<AllocationVO> allocations = new ArrayList<>();

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
