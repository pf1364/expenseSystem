package com.enpenseSystem.service;

import com.enpenseSystem.dto.ReimbursementDetailVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReimbursementDetailCacheTests {

    private static final String REIM_NO = "PERF20260615000050";
    private static final String CACHE_KEY = "reim:detail:" + REIM_NO;

    private ObjectProvider<StringRedisTemplate> redisProvider;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private ObjectMapper objectMapper;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisProvider = mock(ObjectProvider.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();

        when(redisProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void readsNestedDetailWithoutLosingDatesOrMoneyPrecision() throws Exception {
        ReimbursementDetailVO expected = detail();
        when(valueOperations.get(CACHE_KEY)).thenReturn(objectMapper.writeValueAsString(expected));

        Optional<ReimbursementDetailVO> result = enabledCache().get(REIM_NO);

        assertThat(result).isPresent();
        assertThat(result.get().getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 6, 15, 12, 30));
        assertThat(result.get().getAllowanceAmount()).isEqualByComparingTo("500.00");
        assertThat(result.get().getItineraries()).hasSize(1);
        assertThat(result.get().getItineraries().get(0).getAllowanceDays().get(0).getAllowanceDate())
                .isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(result.get().getItineraries().get(0).getAllowanceDays().get(0).getMealAmount())
                .isEqualByComparingTo("100.00");
    }

    @Test
    void writesCompleteDetailWithConfiguredRedisTtl() {
        ReimbursementDetailCache cache = enabledCache();

        cache.put(REIM_NO, detail());

        verify(valueOperations).set(eq(CACHE_KEY), any(String.class), eq(Duration.ofMinutes(10)));
    }

    @Test
    void readsNullMarkerAsKnownMissingBill() {
        when(valueOperations.get(CACHE_KEY)).thenReturn("__NULL__");

        ReimbursementDetailCache.LookupResult result = enabledCache().lookup(REIM_NO);

        assertThat(result.status()).isEqualTo(ReimbursementDetailCache.LookupStatus.NULL_HIT);
        assertThat(result.detail()).isNull();
    }

    @Test
    void writesNullMarkerWithShortTtl() {
        ReimbursementDetailCache cache = enabledCache();

        cache.putNull(REIM_NO);

        verify(valueOperations).set(CACHE_KEY, "__NULL__", Duration.ofMinutes(2));
    }

    @Test
    void malformedJsonIsDeletedAndTreatedAsCacheMiss() {
        when(valueOperations.get(CACHE_KEY)).thenReturn("{not-json");

        Optional<ReimbursementDetailVO> result = enabledCache().get(REIM_NO);

        assertThat(result).isEmpty();
        verify(redisTemplate).delete(CACHE_KEY);
    }

    @Test
    void redisFailureIsTreatedAsCacheMiss() {
        when(valueOperations.get(CACHE_KEY)).thenThrow(new IllegalStateException("redis unavailable"));

        assertThat(enabledCache().get(REIM_NO)).isEmpty();
    }

    @Test
    void disabledCacheDoesNotAccessRedis() {
        ReimbursementDetailCache cache = new ReimbursementDetailCache(
                redisProvider, objectMapper, false, Duration.ofMinutes(10), Duration.ofMinutes(2));

        assertThat(cache.get(REIM_NO)).isEmpty();
        cache.put(REIM_NO, detail());
        cache.putNull(REIM_NO);
        cache.evictAfterCommit(REIM_NO);

        verify(redisProvider, never()).getIfAvailable();
    }

    @Test
    void cacheIsDeletedOnlyAfterTransactionCommit() {
        ReimbursementDetailCache cache = enabledCache();
        TransactionSynchronizationManager.initSynchronization();

        cache.evictAfterCommit(REIM_NO);

        verify(redisTemplate, never()).delete(CACHE_KEY);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
        verify(redisTemplate).delete(CACHE_KEY);
    }

    @Test
    void cacheIsNotDeletedWhenTransactionRollsBack() {
        ReimbursementDetailCache cache = enabledCache();
        TransactionSynchronizationManager.initSynchronization();

        cache.evictAfterCommit(REIM_NO);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(item -> item.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(redisTemplate, never()).delete(CACHE_KEY);
    }

    private ReimbursementDetailCache enabledCache() {
        return new ReimbursementDetailCache(
                redisProvider, objectMapper, true, Duration.ofMinutes(10), Duration.ofMinutes(2));
    }

    private ReimbursementDetailVO detail() {
        ReimbursementDetailVO detail = new ReimbursementDetailVO();
        detail.setReimNo(REIM_NO);
        detail.setAllowanceAmount(new BigDecimal("500.00"));
        detail.setCreatedAt(LocalDateTime.of(2026, 6, 15, 12, 30));

        ReimbursementDetailVO.AllowanceDayVO day = new ReimbursementDetailVO.AllowanceDayVO();
        day.setAllowanceDate(LocalDate.of(2026, 5, 1));
        day.setMealAmount(new BigDecimal("100.00"));

        ReimbursementDetailVO.ItineraryVO itinerary = new ReimbursementDetailVO.ItineraryVO();
        itinerary.setStartDate(LocalDate.of(2026, 5, 1));
        itinerary.setAllowanceDays(List.of(day));
        detail.setItineraries(List.of(itinerary));
        return detail;
    }
}
