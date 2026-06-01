package com.enpenseSystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReimbursementSaveResponse {

    private String reimNo; // 报销单号
    private String billStatus; // 单据状态编码
    private String billStatusName; // 单据状态名称
}
