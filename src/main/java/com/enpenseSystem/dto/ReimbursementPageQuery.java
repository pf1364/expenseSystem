package com.enpenseSystem.dto;

import lombok.Data;

/**
 * 报销单分页查询参数。
 *
 * <p>Spring 会把 GET Query 参数绑定到该对象。字符串条件为空时不参与 SQL 拼接。</p>
 */
@Data
public class ReimbursementPageQuery {

    private Integer pageNum = 1; // 当前页，默认 1
    private Integer pageNo; // 当前页兼容字段，有值时优先于 pageNum
    private Integer pageSize = 10; // 每页条数，默认 10
    private String reimNo; // 报销单号，模糊查询
    private String title; // 报销标题，模糊查询
    private String reason; // 出差事由，模糊查询
    private String reimCompanyName; // 费用归属公司，模糊查询主表汇总字段
    private String reimDepartmentName; // 报销部门名称，精确查询
    private String reimburserKeyword; // 报销人姓名或工号的统一精确查询值
    private String reimburserName; // 报销人姓名，精确查询
    private String reimburserNo; // 报销人工号，精确查询
    private String businessTypeName; // 业务类型名称，精确查询
    private String billStatus; // 单据状态编码，精确查询

    /** 兼容 pageNum/pageNo，并保证页码最小为 1。 */
    public Integer getPageNum() {
        Integer current = pageNo == null ? pageNum : pageNo;
        return current == null || current < 1 ? 1 : current;
    }

    /** pageSize 为空或小于 1 时使用默认值 10。 */
    public Integer getPageSize() {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }
}
