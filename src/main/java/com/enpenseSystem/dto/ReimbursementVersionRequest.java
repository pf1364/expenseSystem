package com.enpenseSystem.dto;

import lombok.Data;

/**
 * 仅携带报销单当前版本号的请求体。
 *
 * <p>提交已有草稿时，前端需要把详情接口返回的 version 原样带回，
 * 后端据此判断用户编辑期间单据是否已经被其他人保存过。</p>
 */
@Data
public class ReimbursementVersionRequest {

    private Integer version; // 当前页面持有的乐观锁版本号
    private String lockToken; // 编辑锁令牌，用于后端校验当前用户是否持有编辑锁
}

