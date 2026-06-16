package com.enpenseSystem.service.support;

import com.enpenseSystem.dto.ReimbursementDetailVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Optional;

/**
 * 报销单详情专用缓存。
 *
 * <p>业务层只关心报销单详情的 Key、TTL 和事务提交后的失效时机。
 * JSON 序列化、空值标记、Redis 异常回退等通用细节交给 {@link CacheClient}。</p>
 */
@Component
public class ReimbursementDetailCache {

    private static final String KEY_PREFIX = "reim:detail:";

    private final CacheClient cacheClient;
    private final boolean enabled;
    private final Duration ttl;
    private final Duration nullTtl;

    public ReimbursementDetailCache(
            CacheClient cacheClient,
            @Value("${app.reimbursement.detail-cache.enabled:true}") boolean enabled,
            @Value("${app.reimbursement.detail-cache.ttl:10m}") Duration ttl,
            @Value("${app.reimbursement.detail-cache.null-ttl:2m}") Duration nullTtl) {
        this.cacheClient = cacheClient;
        this.enabled = enabled;
        this.ttl = ttl;
        this.nullTtl = nullTtl;
    }

    /**
     * 按报销单号读取缓存，并区分未命中、详情命中和空值命中。
     */
    public LookupResult lookup(String reimNo) {
        if (!enabled || !StringUtils.hasText(reimNo)) {
            return LookupResult.miss();
        }
        CacheClient.LookupResult<ReimbursementDetailVO> result =
                cacheClient.lookup(key(reimNo), ReimbursementDetailVO.class);
        if (result.status() == CacheClient.LookupStatus.HIT) {
            return LookupResult.hit(result.value());
        }
        if (result.status() == CacheClient.LookupStatus.NULL_HIT) {
            return LookupResult.nullHit();
        }
        return LookupResult.miss();
    }

    /**
     * 兼容只关心正常详情命中的调用方。
     */
    public Optional<ReimbursementDetailVO> get(String reimNo) {
        return Optional.ofNullable(lookup(reimNo).detail());
    }

    /**
     * 写入完整详情缓存。
     */
    public void put(String reimNo, ReimbursementDetailVO detail) {
        if (!enabled || !StringUtils.hasText(reimNo)) {
            return;
        }
        cacheClient.put(key(reimNo), detail, ttl);
    }

    /**
     * 写入“不存在”的短 TTL 空值缓存。
     */
    public void putNull(String reimNo) {
        if (!enabled || !StringUtils.hasText(reimNo)) {
            return;
        }
        cacheClient.putNull(key(reimNo), nullTtl);
    }

    /**
     * 在当前数据库事务提交成功后删除详情缓存。
     */
    public void evictAfterCommit(String reimNo) {
        if (!enabled || !StringUtils.hasText(reimNo)) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evictNow(reimNo);
                }
            });
            return;
        }
        evictNow(reimNo);
    }

    private void evictNow(String reimNo) {
        cacheClient.deleteQuietly(key(reimNo));
    }

    private String key(String reimNo) {
        return KEY_PREFIX + reimNo;
    }

    public enum LookupStatus {
        MISS,
        HIT,
        NULL_HIT
    }

    public record LookupResult(LookupStatus status, ReimbursementDetailVO detail) {

        public static LookupResult miss() {
            return new LookupResult(LookupStatus.MISS, null);
        }

        public static LookupResult hit(ReimbursementDetailVO detail) {
            return new LookupResult(LookupStatus.HIT, detail);
        }

        public static LookupResult nullHit() {
            return new LookupResult(LookupStatus.NULL_HIT, null);
        }
    }
}
