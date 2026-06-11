package com.enpenseSystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建、更新、提交和复制接口的轻量响应。
 *
 * <p>前端拿到单号和状态后，可以跳转详情页或刷新列表，不需要接口再次返回完整详情。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReimbursementSaveResponse {

    private String reimNo; // 报销单号
    private String billStatus; // 单据状态编码
    private String billStatusName; // 单据状态名称
}
