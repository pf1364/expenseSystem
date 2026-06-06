package com.enpenseSystem.dto;

import lombok.Data;

@Data
public class ReimbursementPageQuery {

    private Integer pageNum = 1;
    private Integer pageNo;
    private Integer pageSize = 10;
    private String reimNo;
    private String title;
    private String reason;
    private String reimCompanyName;
    private String reimDepartmentName;
    private String reimburserKeyword;
    private String reimburserName;
    private String reimburserNo;
    private String businessTypeName;
    private String billStatus;

    public Integer getPageNum() {
        Integer current = pageNo == null ? pageNum : pageNo;
        return current == null || current < 1 ? 1 : current;
    }

    public Integer getPageSize() {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }
}
