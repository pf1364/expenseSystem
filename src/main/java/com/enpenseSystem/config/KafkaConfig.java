package com.enpenseSystem.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Kafka 条件配置。
 *
 * <p>只有配置 app.kafka.enabled=true 时才启用 Kafka，
 * 使没有 Kafka 环境的本地开发也能正常启动项目。</p>
 */
@EnableKafka
@Configuration
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true")
public class KafkaConfig {
}
