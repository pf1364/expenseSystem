package com.enpenseSystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.enpenseSystem.dto.ReimbursementEditLockVO;
import com.enpenseSystem.entity.FkReimMain;
import com.enpenseSystem.entity.SysUser;
import com.enpenseSystem.exception.ResourceNotFoundException;
import com.enpenseSystem.exception.StatusConflictException;
import com.enpenseSystem.service.FkReimMainService;
import com.enpenseSystem.service.ReimbursementEditLockService;
import com.enpenseSystem.service.support.RedisLockClient;
import com.enpenseSystem.utils.ReimbursementConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Optional;

/**
 * 报销单编辑锁服务实现。
 *
 * <p>锁的粒度是报销单号：reim:lock:{reimNo}。只有草稿允许加编辑锁，
 * 已提交和已作废单据只能查看，不能进入可编辑状态。</p>
 */
@Service
public class ReimbursementEditLockServiceImpl implements ReimbursementEditLockService {

    private static final String KEY_PREFIX = "reim:lock:";

    private final FkReimMainService mainService;
    private final RedisLockClient redisLockClient;
    private final Duration ttl;

    public ReimbursementEditLockServiceImpl(FkReimMainService mainService,
                                            RedisLockClient redisLockClient,
                                            @Value("${app.reimbursement.edit-lock.ttl:10m}") Duration ttl) {
        this.mainService = mainService;
        this.redisLockClient = redisLockClient;
        this.ttl = ttl;
    }

    @Override
    public ReimbursementEditLockVO tryLock(String reimNo, SysUser user) {
        FkReimMain main = getMain(reimNo);
        if (!ReimbursementConstants.STATUS_DRAFT.equals(main.getBillStatus())) {
            throw new StatusConflictException("只有草稿状态可以编辑");
        }
        RedisLockClient.LockResult result = redisLockClient.tryLock(key(reimNo), ownerId(user), ownerName(user), ttl);
        return toVO(result.acquired(), result.lockInfo(), result.message());
    }

    @Override
    public ReimbursementEditLockVO renew(String reimNo, String lockToken, SysUser user) {
        getMain(reimNo);
        boolean renewed = redisLockClient.renew(key(reimNo), lockToken, ttl);
        RedisLockClient.LockInfo info = redisLockClient.readLockInfo(key(reimNo)).orElse(null);
        return toVO(renewed, info, renewed ? "编辑锁已续期" : "编辑锁续期失败，请刷新页面后重试");
    }

    @Override
    public ReimbursementEditLockVO unlock(String reimNo, String lockToken, SysUser user) {
        getMain(reimNo);
        boolean unlocked = redisLockClient.unlock(key(reimNo), lockToken);
        RedisLockClient.LockInfo info = redisLockClient.readLockInfo(key(reimNo)).orElse(null);
        return toVO(unlocked, info, unlocked ? "编辑锁已释放" : "编辑锁释放失败或已过期");
    }

    @Override
    public ReimbursementEditLockVO getLockInfo(String reimNo) {
        getMain(reimNo);
        Optional<RedisLockClient.LockInfo> info = redisLockClient.readLockInfo(key(reimNo));
        return toVO(false, info.orElse(null), info.isPresent() ? "该报销单正在被编辑" : "该报销单暂无编辑锁");
    }

    private FkReimMain getMain(String reimNo) {
        if (!StringUtils.hasText(reimNo)) {
            throw new IllegalArgumentException("报销单号不能为空");
        }
        FkReimMain main = mainService.getOne(new LambdaQueryWrapper<FkReimMain>().eq(FkReimMain::getReimNo, reimNo), false);
        if (main == null) {
            throw new ResourceNotFoundException("报销单不存在：" + reimNo);
        }
        return main;
    }

    private ReimbursementEditLockVO toVO(boolean acquired, RedisLockClient.LockInfo info, String message) {
        return new ReimbursementEditLockVO(
                acquired,
                info != null,
                info == null ? null : info.getOwnerId(),
                info == null ? null : info.getOwnerName(),
                acquired && info != null ? info.getLockToken() : null,
                info == null ? null : info.getExpireAt(),
                message);
    }

    private String key(String reimNo) {
        return KEY_PREFIX + reimNo;
    }

    private String ownerId(SysUser user) {
        return user == null || user.getId() == null ? "" : String.valueOf(user.getId());
    }

    private String ownerName(SysUser user) {
        if (user == null) {
            return "未知用户";
        }
        return StringUtils.hasText(user.getDisplayName()) ? user.getDisplayName() : user.getUsername();
    }
}

