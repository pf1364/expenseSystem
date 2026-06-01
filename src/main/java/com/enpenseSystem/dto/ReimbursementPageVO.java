package com.enpenseSystem.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ReimbursementPageVO {

    private Long id; // 报销单主表ID
    private String reimNo; // 报销单号
    private String billStatus; // 单据状态编码
    private String billStatusName; // 单据状态名称
    private String billTypeName; // 单据类型名称
    private String reimburserName; // 报销人姓名
    private String reimburserNo; // 报销人工号
    private String reimDepartmentName; // 报销部门名称
    private String reimCompanyNames; // 费用归属公司名称，多个用逗号拼接
    private String businessTypeName; // 业务类型名称
    private String title; // 报销标题
    private String reason; // 报销事由
    private BigDecimal allowanceAmount; // 补助金额
    private LocalDateTime createdAt; // 创建时间
}
