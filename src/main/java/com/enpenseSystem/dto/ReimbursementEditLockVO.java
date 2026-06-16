package com.enpenseSystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 报销单编辑锁状态。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReimbursementEditLockVO {

    private Boolean acquired; // 本次是否成功获得锁
    private Boolean locked; // 当前报销单是否存在编辑锁
    private String ownerId; // 持锁用户ID
    private String ownerName; // 持锁用户显示名称
    private String lockToken; // 当前用户获得锁时返回的令牌
    private LocalDateTime expireAt; // 锁预计过期时间
    private String message; // 给前端展示的提示信息
}

