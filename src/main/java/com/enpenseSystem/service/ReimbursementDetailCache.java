package com.enpenseSystem.service;

import com.enpenseSystem.dto.ReimbursementDetailVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Optional;

/**
 * 报销单详情专用 Redis 缓存。
 *
 * <p>一张报销单的主表、行程、每日补助和费用分摊已经被组装为
 * {@link ReimbursementDetailVO}，因此缓存时将整个对象序列化为一份 JSON。
 * Redis 异常只会让本次请求回退数据库，不会影响详情接口的基本可用性。</p>
 */
@Component
public class ReimbursementDetailCache {

    private static final Logger log = LoggerFactory.getLogger(ReimbursementDetailCache.class);
    private static final String KEY_PREFIX = "reim:detail:";
    /** 不存在报销单的缓存标记，区别于 Redis 中完全没有该 Key。 */
    private static final String NULL_MARKER = "__NULL__";

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final Duration ttl;
    private final Duration nullTtl;

    public ReimbursementDetailCache(
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            ObjectMapper objectMapper,
            @Value("${app.reimbursement.detail-cache.enabled:true}") boolean enabled,
            @Value("${app.reimbursement.detail-cache.ttl:10m}") Duration ttl,
            @Value("${app.reimbursement.detail-cache.null-ttl:2m}") Duration nullTtl) {
        this.redisTemplateProvider = redisTemplateProvider;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.ttl = ttl;
        this.nullTtl = nullTtl;
    }

    /**
     * 按报销单号读取缓存，并区分未命中、正常详情命中和空值命中。
     *
     * <p>只有 MISS 才允许继续查询数据库；NULL_HIT 表示该单号在短时间内已确认不存在，
     * 调用方应直接返回 404，从而阻止相同非法单号持续穿透到数据库。</p>
     */
    public LookupResult lookup(String reimNo) {
        if (!enabled || !StringUtils.hasText(reimNo)) {
            return LookupResult.miss();
        }
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return LookupResult.miss();
        }

        String key = key(reimNo);
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(json)) {
                return LookupResult.miss();
            }
            if (NULL_MARKER.equals(json)) {
                return LookupResult.nullHit();
            }
            return LookupResult.hit(objectMapper.readValue(json, ReimbursementDetailVO.class));
        } catch (JsonProcessingException exception) {
            // 无法解析的缓存不能继续使用，删除后让调用方重新从数据库构建。
            log.warn("报销单详情缓存 JSON 损坏，删除后回退数据库，reimNo={}", reimNo);
            deleteQuietly(redisTemplate, key);
            return LookupResult.miss();
        } catch (Exception exception) {
            log.warn("读取报销单详情缓存失败，回退数据库，reimNo={}, reason={}",
                    reimNo, exception.getMessage());
            return LookupResult.miss();
        }
    }

    /**
     * 兼容只关心正常详情的调用方；空值命中和普通未命中均返回空。
     */
    public Optional<ReimbursementDetailVO> get(String reimNo) {
        return Optional.ofNullable(lookup(reimNo).detail());
    }

    /**
     * 将数据库组装出的完整详情写入 Redis，并设置物理 TTL。
     */
    public void put(String reimNo, ReimbursementDetailVO detail) {
        if (!enabled || !StringUtils.hasText(reimNo) || detail == null) {
            return;
        }
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(detail);
            redisTemplate.opsForValue().set(key(reimNo), json, ttl);
        } catch (Exception exception) {
            // 缓存写入失败不改变本次数据库查询已经得到的正确结果。
            log.warn("写入报销单详情缓存失败，reimNo={}, reason={}",
                    reimNo, exception.getMessage());
        }
    }

    /**
     * 缓存“不存在”结果，使用比正常详情更短的 TTL，防止非法单号反复查询数据库。
     */
    public void putNull(String reimNo) {
        if (!enabled || !StringUtils.hasText(reimNo)) {
            return;
        }
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return;
        }

        try {
            redisTemplate.opsForValue().set(key(reimNo), NULL_MARKER, nullTtl);
        } catch (Exception exception) {
            log.warn("写入报销单空值缓存失败，reimNo={}, reason={}",
                    reimNo, exception.getMessage());
        }
    }

    /**
     * 在当前数据库事务提交成功后删除缓存。
     *
     * <p>如果调用方不在事务中则立即删除。事务回滚时不会执行 afterCommit，
     * 从而避免数据库没有修改成功却提前删除缓存。</p>
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
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate != null) {
            deleteQuietly(redisTemplate, key(reimNo));
        }
    }

    private void deleteQuietly(StringRedisTemplate redisTemplate, String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception exception) {
            log.warn("删除报销单详情缓存失败，key={}, reason={}", key, exception.getMessage());
        }
    }

    private String key(String reimNo) {
        return KEY_PREFIX + reimNo;
    }

    /** Redis 详情查询的三种状态。 */
    public enum LookupStatus {
        MISS,
        HIT,
        NULL_HIT
    }

    /** 查询状态及正常命中时携带的详情数据。 */
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
