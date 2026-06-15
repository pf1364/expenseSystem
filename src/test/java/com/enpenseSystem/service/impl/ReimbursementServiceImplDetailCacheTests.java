package com.enpenseSystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.enpenseSystem.dto.ReimbursementDetailVO;
import com.enpenseSystem.dto.ReimbursementSaveRequest;
import com.enpenseSystem.entity.FkReimMain;
import com.enpenseSystem.exception.ResourceNotFoundException;
import com.enpenseSystem.service.FkCityAllowanceService;
import com.enpenseSystem.service.FkReimAllocationService;
import com.enpenseSystem.service.FkReimAllowanceDayService;
import com.enpenseSystem.service.FkReimItineraryService;
import com.enpenseSystem.service.FkReimMainService;
import com.enpenseSystem.service.ReimbursementDetailCache;
import com.enpenseSystem.utils.ReimbursementConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;

class ReimbursementServiceImplDetailCacheTests {

    private static final String REIM_NO = "PERF20260615000050";

    private FkReimMainService mainService;
    private FkReimItineraryService itineraryService;
    private FkReimAllowanceDayService allowanceDayService;
    private FkReimAllocationService allocationService;
    private ReimbursementDetailCache detailCache;
    private ReimbursementServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        mainService = mock(FkReimMainService.class);
        itineraryService = mock(FkReimItineraryService.class);
        allowanceDayService = mock(FkReimAllowanceDayService.class);
        allocationService = mock(FkReimAllocationService.class);
        detailCache = mock(ReimbursementDetailCache.class);

        service = new ReimbursementServiceImpl(
                mainService,
                itineraryService,
                allowanceDayService,
                allocationService,
                mock(FkCityAllowanceService.class),
                mock(ObjectProvider.class),
                mock(ObjectProvider.class),
                detailCache
        );
    }

    @Test
    void cacheHitReturnsDetailWithoutQueryingDatabase() {
        ReimbursementDetailVO cached = new ReimbursementDetailVO();
        cached.setReimNo(REIM_NO);
        when(detailCache.lookup(REIM_NO)).thenReturn(
                ReimbursementDetailCache.LookupResult.hit(cached));

        ReimbursementDetailVO result = service.detail(REIM_NO);

        assertThat(result).isSameAs(cached);
        verify(mainService, never()).getOne(any(), eq(false));
        verify(itineraryService, never()).list(any(Wrapper.class));
        verify(allowanceDayService, never()).list(any(Wrapper.class));
        verify(allocationService, never()).list(any(Wrapper.class));
    }

    @Test
    void cacheMissQueriesDatabaseAndStoresCompleteDetail() {
        when(detailCache.lookup(REIM_NO)).thenReturn(
                ReimbursementDetailCache.LookupResult.miss());
        when(mainService.getOne(any(), eq(false))).thenReturn(main());
        when(itineraryService.list(any(Wrapper.class))).thenReturn(List.of());
        when(allowanceDayService.list(any(Wrapper.class))).thenReturn(List.of());
        when(allocationService.list(any(Wrapper.class))).thenReturn(List.of());

        ReimbursementDetailVO result = service.detail(REIM_NO);

        assertThat(result.getReimNo()).isEqualTo(REIM_NO);
        verify(detailCache).put(REIM_NO, result);
    }

    @Test
    void nullCacheHitReturnsNotFoundWithoutQueryingDatabase() {
        when(detailCache.lookup(REIM_NO)).thenReturn(
                ReimbursementDetailCache.LookupResult.nullHit());

        assertThatThrownBy(() -> service.detail(REIM_NO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(REIM_NO);

        verify(mainService, never()).getOne(any(), eq(false));
    }

    @Test
    void databaseNotFoundWritesNullMarkerBeforeReturningNotFound() {
        when(detailCache.lookup(REIM_NO)).thenReturn(
                ReimbursementDetailCache.LookupResult.miss());
        when(mainService.getOne(any(), eq(false))).thenReturn(null);

        assertThatThrownBy(() -> service.detail(REIM_NO))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(detailCache).putNull(REIM_NO);
    }

    @Test
    void deletingDraftSchedulesCacheEviction() {
        when(mainService.getOne(any(), eq(false))).thenReturn(main());

        service.deleteDraft(REIM_NO);

        verify(detailCache).evictAfterCommit(REIM_NO);
    }

    @Test
    void updatingDraftSchedulesCacheEviction() {
        when(mainService.getOne(any(), eq(false))).thenReturn(main());
        when(itineraryService.list(any(Wrapper.class))).thenReturn(List.of());
        when(allowanceDayService.list(any(Wrapper.class))).thenReturn(List.of());
        when(allocationService.list(any(Wrapper.class))).thenReturn(List.of());

        service.update(REIM_NO, new ReimbursementSaveRequest());

        verify(detailCache).evictAfterCommit(REIM_NO);
    }

    @Test
    void voidingSubmittedBillSchedulesCacheEviction() {
        FkReimMain submitted = main();
        submitted.setBillStatus(ReimbursementConstants.STATUS_SUBMITTED);
        submitted.setBillStatusName(ReimbursementConstants.STATUS_SUBMITTED_NAME);
        when(mainService.getOne(any(), eq(false))).thenReturn(submitted);

        service.voidBill(REIM_NO);

        verify(detailCache).evictAfterCommit(REIM_NO);
    }

    @Test
    void submitValidationReadsDatabaseInsteadOfDetailCache() {
        when(mainService.getOne(any(), eq(false))).thenReturn(main());
        when(itineraryService.list(any(Wrapper.class))).thenReturn(List.of());
        when(allowanceDayService.list(any(Wrapper.class))).thenReturn(List.of());
        when(allocationService.list(any(Wrapper.class))).thenReturn(List.of());

        assertThatThrownBy(() -> service.submitDraft(REIM_NO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("费用归属及分摊不能为空");

        verify(detailCache, never()).get(REIM_NO);
    }

    @Test
    void creatingDraftEvictsPossibleNegativeCacheForGeneratedNumber() {
        doAnswer(invocation -> {
            FkReimMain saved = invocation.getArgument(0);
            saved.setId(99L);
            return true;
        }).when(mainService).save(any(FkReimMain.class));
        when(itineraryService.list(any(Wrapper.class))).thenReturn(List.of());
        when(allowanceDayService.list(any(Wrapper.class))).thenReturn(List.of());
        when(allocationService.list(any(Wrapper.class))).thenReturn(List.of());

        String generatedReimNo = service.createDraft(new ReimbursementSaveRequest()).getReimNo();

        verify(detailCache).evictAfterCommit(generatedReimNo);
    }

    private FkReimMain main() {
        FkReimMain main = new FkReimMain();
        main.setId(1L);
        main.setReimNo(REIM_NO);
        main.setBillStatus(ReimbursementConstants.STATUS_DRAFT);
        main.setBillStatusName(ReimbursementConstants.STATUS_DRAFT_NAME);
        return main;
    }
}
