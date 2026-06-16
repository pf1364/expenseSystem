package com.enpenseSystem.service.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * 通用 Redis JSON 缓存客户端。
 *
 * <p>该组件只封装缓存读写细节：JSON 序列化、空值标记、坏缓存删除和 Redis 异常回退。
 * 具体业务 Key、TTL 和缓存是否启用仍由上层业务缓存组件决定。</p>
 */
@Component
public class CacheClient {

    public static final String NULL_MARKER = "__NULL__";

    private static final Logger log = LoggerFactory.getLogger(CacheClient.class);

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    public CacheClient(ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                       ObjectMapper objectMapper,
                       @Value("${app.cache.enabled:true}") boolean enabled) {
        this.redisTemplateProvider = redisTemplateProvider;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
    }

    /**
     * 读取指定 Key，并区分未命中、正常命中和空值命中。
     */
    public <T> LookupResult<T> lookup(String key, Class<T> valueType) {
        if (!enabled || !StringUtils.hasText(key)) {
            return LookupResult.miss();
        }
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return LookupResult.miss();
        }

        try {
            String json = redisTemplate.opsForValue().get(key);
            if (!StringUtils.hasText(json)) {
                return LookupResult.miss();
            }
            if (NULL_MARKER.equals(json)) {
                return LookupResult.nullHit();
            }
            return LookupResult.hit(objectMapper.readValue(json, valueType));
        } catch (JsonProcessingException exception) {
            log.warn("缓存 JSON 损坏，删除后回退数据库，key={}", key);
            deleteQuietly(key);
            return LookupResult.miss();
        } catch (Exception exception) {
            log.warn("读取缓存失败，回退数据库，key={}, reason={}", key, exception.getMessage());
            return LookupResult.miss();
        }
    }

    /**
     * 将对象序列化为 JSON 并写入 Redis。
     */
    public void put(String key, Object value, Duration ttl) {
        if (!enabled || !StringUtils.hasText(key) || value == null) {
            return;
        }
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (Exception exception) {
            log.warn("写入缓存失败，key={}, reason={}", key, exception.getMessage());
        }
    }

    /**
     * 写入空值标记，通常用于短时间防缓存穿透。
     */
    public void putNull(String key, Duration ttl) {
        if (!enabled || !StringUtils.hasText(key)) {
            return;
        }
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, NULL_MARKER, ttl);
        } catch (Exception exception) {
            log.warn("写入空值缓存失败，key={}, reason={}", key, exception.getMessage());
        }
    }

    /**
     * 尝试删除缓存，失败只记录日志，不影响主业务。
     */
    public void deleteQuietly(String key) {
        if (!enabled || !StringUtils.hasText(key)) {
            return;
        }
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.delete(key);
        } catch (Exception exception) {
            log.warn("删除缓存失败，key={}, reason={}", key, exception.getMessage());
        }
    }

    public enum LookupStatus {
        MISS,
        HIT,
        NULL_HIT
    }

    public record LookupResult<T>(LookupStatus status, T value) {

        public static <T> LookupResult<T> miss() {
            return new LookupResult<>(LookupStatus.MISS, null);
        }

        public static <T> LookupResult<T> hit(T value) {
            return new LookupResult<>(LookupStatus.HIT, value);
        }

        public static <T> LookupResult<T> nullHit() {
            return new LookupResult<>(LookupStatus.NULL_HIT, null);
        }
    }
}
