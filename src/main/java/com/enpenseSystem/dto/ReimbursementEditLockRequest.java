package com.enpenseSystem.dto;

import lombok.Data;

/**
 * 编辑锁续期和释放请求。
 *
 * <p>lockToken 是加锁成功后后端返回的随机令牌，释放和续期时必须带回。
 * 后端会先比对 Redis 中的令牌，避免当前用户误删或续期别人的锁。</p>
 */
@Data
public class ReimbursementEditLockRequest {

    private String lockToken; // 加锁成功后得到的锁令牌
}

