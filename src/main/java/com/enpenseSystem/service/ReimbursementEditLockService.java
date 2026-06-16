package com.enpenseSystem.service;

import com.enpenseSystem.dto.ReimbursementEditLockVO;
import com.enpenseSystem.entity.SysUser;

/**
 * 报销单编辑锁业务服务。
 *
 * <p>编辑锁只用于改善多人编辑体验，真正防止覆盖的兜底仍然是数据库 version 乐观锁。</p>
 */
public interface ReimbursementEditLockService {

    ReimbursementEditLockVO tryLock(String reimNo, SysUser user);

    ReimbursementEditLockVO renew(String reimNo, String lockToken, SysUser user);

    ReimbursementEditLockVO unlock(String reimNo, String lockToken, SysUser user);

    ReimbursementEditLockVO getLockInfo(String reimNo);
}

