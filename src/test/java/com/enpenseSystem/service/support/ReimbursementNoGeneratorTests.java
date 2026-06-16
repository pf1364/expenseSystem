package com.enpenseSystem.service.support;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReimbursementNoGeneratorTests {

    @Test
    @SuppressWarnings("unchecked")
    void generatesIncrementalNumberWithRedisSequence() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(startsWith("reim:no:"))).thenReturn(7L);

        String reimNo = new ReimbursementNoGenerator(provider).nextReimNo();

        assertThat(reimNo).startsWith("CLBX");
        assertThat(reimNo).endsWith("0007");
    }

    @Test
    @SuppressWarnings("unchecked")
    void fallsBackToTimestampWhenRedisIsUnavailable() {
        ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenThrow(new IllegalStateException("redis down"));

        String reimNo = new ReimbursementNoGenerator(provider).nextReimNo();

        assertThat(reimNo).startsWith("CLBX");
        assertThat(reimNo).hasSize(21);
    }
}
