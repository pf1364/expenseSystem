package com.enpenseSystem.dto;

import lombok.Data;

@Data
public class ReimbursementPageQuery {

    private Integer pageNum = 1; // 当前页
    private Integer pageSize = 10; // 每页条数
    private String reimNo; // 报销单号，模糊查询
    private String title; // 报销标题，模糊查询
    private String reason; // 报销事由，模糊查询
    private String reimCompanyName; // 费用归属公司名称，查询主表缓存字段
    private String reimDepartmentName; // 报销部门名称，精确查询
    private String reimburserKeyword; // 报销人姓名或工号，精确查询
    private String businessTypeName; // 业务类型名称，精确查询

    public Integer getPageNum() {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    public Integer getPageSize() {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }
}
