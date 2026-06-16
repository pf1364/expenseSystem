package com.enpenseSystem.service.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 自研 Redis 分布式锁客户端。
 *
 * <p>加锁使用 SET key value NX EX ttl；续期和释放使用 Lua 脚本先校验 lockToken，
 * 再执行 expire/delete，避免 A 用户误删 B 用户后续获得的锁。</p>
 */
@Component
public class RedisLockClient {

    private static final Logger log = LoggerFactory.getLogger(RedisLockClient.class);

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private static final DefaultRedisScript<Long> RENEW_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "redis.call('set', KEYS[1], ARGV[2], 'EX', ARGV[3]); return 1 else return 0 end",
            Long.class);

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectMapper objectMapper;

    public RedisLockClient(ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                           ObjectMapper objectMapper) {
        this.redisTemplateProvider = redisTemplateProvider;
        this.objectMapper = objectMapper;
    }

    /**
     * 尝试获得锁。成功时返回随机 lockToken，后续续期和释放都必须携带该 token。
     */
    public LockResult tryLock(String key, String ownerId, String ownerName, Duration ttl) {
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            return LockResult.failed(null, "Redis 不可用，暂时无法获得编辑锁");
        }
        try {
            String lockToken = UUID.randomUUID().toString().replace("-", "");
            LockInfo info = new LockInfo(ownerId, ownerName, lockToken, LocalDateTime.now().plus(ttl));
            String value = objectMapper.writeValueAsString(info);
            Boolean success = redis.opsForValue().setIfAbsent(key, value, ttl);
            if (Boolean.TRUE.equals(success)) {
                return LockResult.acquired(info);
            }
            return LockResult.failed(readLockInfo(key).orElse(null), "该报销单正在被其他人编辑");
        } catch (Exception exception) {
            log.warn("获取 Redis 编辑锁失败，key={}, reason={}", key, exception.getMessage());
            return LockResult.failed(null, "获取编辑锁失败，请稍后重试");
        }
    }

    /**
     * 续期当前用户持有的锁。只有 Redis 中的 lockToken 与请求一致才会续期。
     */
    public boolean renew(String key, String lockToken, Duration ttl) {
        if (!StringUtils.hasText(lockToken)) {
            return false;
        }
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            return false;
        }
        try {
            LockInfo oldInfo = readLockInfo(key).orElse(null);
            if (oldInfo == null || !lockToken.equals(oldInfo.getLockToken())) {
                return false;
            }
            String oldValue = objectMapper.writeValueAsString(oldInfo);
            LockInfo newInfo = new LockInfo(oldInfo.getOwnerId(), oldInfo.getOwnerName(), oldInfo.getLockToken(), LocalDateTime.now().plus(ttl));
            String newValue = objectMapper.writeValueAsString(newInfo);
            Long result = redis.execute(RENEW_SCRIPT, List.of(key), oldValue, newValue, String.valueOf(ttl.toSeconds()));
            return Long.valueOf(1L).equals(result);
        } catch (Exception exception) {
            log.warn("续期 Redis 编辑锁失败，key={}, reason={}", key, exception.getMessage());
            return false;
        }
    }

    /**
     * 释放当前用户持有的锁。只有 Redis 中的 lockToken 与请求一致才会删除。
     */
    public boolean unlock(String key, String lockToken) {
        if (!StringUtils.hasText(lockToken)) {
            return false;
        }
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            return false;
        }
        try {
            Long result = redis.execute(UNLOCK_SCRIPT, List.of(key), redisTokenValue(key, lockToken));
            return Long.valueOf(1L).equals(result);
        } catch (Exception exception) {
            log.warn("释放 Redis 编辑锁失败，key={}, reason={}", key, exception.getMessage());
            return false;
        }
    }

    /**
     * 读取锁信息，供前端展示当前持锁人。
     */
    public Optional<LockInfo> readLockInfo(String key) {
        StringRedisTemplate redis = redisTemplateProvider.getIfAvailable();
        if (redis == null) {
            return Optional.empty();
        }
        try {
            String value = redis.opsForValue().get(key);
            if (!StringUtils.hasText(value)) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(value, LockInfo.class));
        } catch (JsonProcessingException exception) {
            log.warn("Redis 编辑锁内容损坏，删除坏锁，key={}", key);
            redis.delete(key);
            return Optional.empty();
        } catch (Exception exception) {
            log.warn("读取 Redis 编辑锁失败，key={}, reason={}", key, exception.getMessage());
            return Optional.empty();
        }
    }

    private String redisTokenValue(String key, String lockToken) {
        LockInfo info = readLockInfo(key).orElse(null);
        if (info == null || !lockToken.equals(info.getLockToken())) {
            return "";
        }
        try {
            return objectMapper.writeValueAsString(info);
        } catch (JsonProcessingException exception) {
            return "";
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LockInfo {
        private String ownerId; // 持锁用户ID
        private String ownerName; // 持锁用户名称
        private String lockToken; // 随机锁令牌
        private LocalDateTime expireAt; // 前端展示用的预计过期时间
    }

    public record LockResult(boolean acquired, LockInfo lockInfo, String message) {

        public static LockResult acquired(LockInfo lockInfo) {
            return new LockResult(true, lockInfo, "已获得编辑锁");
        }

        public static LockResult failed(LockInfo lockInfo, String message) {
            return new LockResult(false, lockInfo, message);
        }
    }
}
