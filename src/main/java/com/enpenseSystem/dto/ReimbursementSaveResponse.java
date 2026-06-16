package com.enpenseSystem.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建、更新、提交和复制接口的轻量响应。
 *
 * <p>前端拿到单号和状态后，可以跳转详情页或刷新列表，不需要接口再次返回完整详情。</p>
 */
@Data
@NoArgsConstructor
public class ReimbursementSaveResponse {

    private String reimNo; // 报销单号
    private String billStatus; // 单据状态编码
    private String billStatusName; // 单据状态名称
    private Integer version; // 保存/提交后的最新乐观锁版本号，前端继续编辑时应使用该值

    public ReimbursementSaveResponse(String reimNo, String billStatus, String billStatusName) {
        this(reimNo, billStatus, billStatusName, null);
    }

    public ReimbursementSaveResponse(String reimNo, String billStatus, String billStatusName, Integer version) {
        this.reimNo = reimNo;
        this.billStatus = billStatus;
        this.billStatusName = billStatusName;
        this.version = version;
    }
}
