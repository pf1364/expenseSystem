package com.enpenseSystem.service.support;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 报销单号生成器。
 *
 * <p>优先使用 Redis INCR 生成当天递增序列；Redis 未配置或不可用时降级为毫秒时间戳。
 * 数据库中的 reim_no 唯一索引仍然是最终防重保障。</p>
 */
@Component
public class ReimbursementNoGenerator {

    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    public ReimbursementNoGenerator(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.redisTemplateProvider = redisTemplateProvider;
    }

    /**
     * 生成格式为 CLBXyyyyMMdd0001 的报销单号。
     */
    public String nextReimNo() {
        String day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String key = "reim:no:" + day;
        try {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate != null) {
                Long seq = redisTemplate.opsForValue().increment(key);
                if (seq != null) {
                    return "CLBX" + day + String.format("%04d", seq);
                }
            }
        } catch (Exception ignored) {
            // 本地训练环境可能没有 Redis，异常时允许业务继续创建单据。
        }
        return "CLBX" + day + DateTimeFormatter.ofPattern("HHmmssSSS").format(LocalDateTime.now());
    }
}
