package com.enpenseSystem.service.support;

import com.enpenseSystem.dto.ReimbursementDetailVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CacheClientTests {

    private static final String KEY = "reim:detail:CLBX202606160001";

    private ObjectProvider<StringRedisTemplate> redisProvider;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private ObjectMapper objectMapper;
    private CacheClient cacheClient;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisProvider = mock(ObjectProvider.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();

        when(redisProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cacheClient = new CacheClient(redisProvider, objectMapper, true);
    }

    @Test
    void readsJsonValueAsRequestedType() throws Exception {
        ReimbursementDetailVO detail = new ReimbursementDetailVO();
        detail.setReimNo("CLBX202606160001");
        detail.setAllowanceAmount(new BigDecimal("500.00"));
        when(valueOperations.get(KEY)).thenReturn(objectMapper.writeValueAsString(detail));

        CacheClient.LookupResult<ReimbursementDetailVO> result = cacheClient.lookup(KEY, ReimbursementDetailVO.class);

        assertThat(result.status()).isEqualTo(CacheClient.LookupStatus.HIT);
        assertThat(result.value().getReimNo()).isEqualTo("CLBX202606160001");
        assertThat(result.value().getAllowanceAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    void readsNullMarkerAsKnownMissingValue() {
        when(valueOperations.get(KEY)).thenReturn(CacheClient.NULL_MARKER);

        CacheClient.LookupResult<ReimbursementDetailVO> result = cacheClient.lookup(KEY, ReimbursementDetailVO.class);

        assertThat(result.status()).isEqualTo(CacheClient.LookupStatus.NULL_HIT);
        assertThat(result.value()).isNull();
    }

    @Test
    void writesJsonAndNullMarkerWithConfiguredTtl() {
        ReimbursementDetailVO detail = new ReimbursementDetailVO();
        detail.setReimNo("CLBX202606160001");

        cacheClient.put(KEY, detail, Duration.ofMinutes(10));
        cacheClient.putNull(KEY, Duration.ofMinutes(2));

        verify(valueOperations).set(eq(KEY), any(String.class), eq(Duration.ofMinutes(10)));
        verify(valueOperations).set(KEY, CacheClient.NULL_MARKER, Duration.ofMinutes(2));
    }

    @Test
    void deletesMalformedJsonAndReturnsMiss() {
        when(valueOperations.get(KEY)).thenReturn("{not-json");

        CacheClient.LookupResult<ReimbursementDetailVO> result = cacheClient.lookup(KEY, ReimbursementDetailVO.class);

        assertThat(result.status()).isEqualTo(CacheClient.LookupStatus.MISS);
        verify(redisTemplate).delete(KEY);
    }

    @Test
    void redisFailureReturnsMiss() {
        when(valueOperations.get(KEY)).thenThrow(new IllegalStateException("redis unavailable"));

        CacheClient.LookupResult<ReimbursementDetailVO> result = cacheClient.lookup(KEY, ReimbursementDetailVO.class);

        assertThat(result.status()).isEqualTo(CacheClient.LookupStatus.MISS);
    }
}
