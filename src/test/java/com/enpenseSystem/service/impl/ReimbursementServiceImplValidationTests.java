package com.enpenseSystem.service.impl;

import com.enpenseSystem.dto.ReimbursementSaveRequest;
import com.enpenseSystem.entity.FkCityAllowance;
import com.enpenseSystem.service.FkCityAllowanceService;
import com.enpenseSystem.service.FkReimAllocationService;
import com.enpenseSystem.service.FkReimAllowanceDayService;
import com.enpenseSystem.service.FkReimItineraryService;
import com.enpenseSystem.service.FkReimMainService;
import com.enpenseSystem.service.ReimbursementDetailCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReimbursementServiceImplValidationTests {

    private ReimbursementServiceImpl service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        FkCityAllowanceService cityAllowanceService = mock(FkCityAllowanceService.class);
        FkCityAllowance beijing = new FkCityAllowance();
        beijing.setCityCode("10119");
        beijing.setCityName("北京");
        beijing.setCityLevel(1);
        beijing.setMealStandard(new BigDecimal("100.00"));
        beijing.setTrafficStandard(new BigDecimal("40.00"));
        beijing.setCommunicationStandard(new BigDecimal("40.00"));
        when(cityAllowanceService.getOne(any(), eq(false))).thenReturn(beijing);

        service = new ReimbursementServiceImpl(
                mock(FkReimMainService.class),
                mock(FkReimItineraryService.class),
                mock(FkReimAllowanceDayService.class),
                mock(FkReimAllocationService.class),
                cityAllowanceService,
                mock(ObjectProvider.class),
                mock(ObjectProvider.class),
                mock(ReimbursementDetailCache.class)
        );
    }

    @Test
    void rejectsAllowanceDateOutsideItinerary() {
        ReimbursementSaveRequest.ItineraryRequest itinerary = itinerary();
        ReimbursementSaveRequest.AllowanceDayRequest day = allowanceDay("2026-04-18", "10119", "北京");

        assertThatThrownBy(() -> validateAllowanceDays(itinerary, List.of(day)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("每日补助日期必须在行程日期范围内");
    }

    @Test
    void rejectsDuplicateAllowanceDateWithinItinerary() {
        ReimbursementSaveRequest.ItineraryRequest itinerary = itinerary();
        ReimbursementSaveRequest.AllowanceDayRequest first = allowanceDay("2026-04-13", "10119", "北京");
        ReimbursementSaveRequest.AllowanceDayRequest duplicate = allowanceDay("2026-04-13", "10119", "北京");

        assertThatThrownBy(() -> validateAllowanceDays(itinerary, List.of(first, duplicate)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("同一行程的每日补助日期不能重复");
    }

    @Test
    void rejectsAllowanceCityDifferentFromDestination() {
        ReimbursementSaveRequest.ItineraryRequest itinerary = itinerary();
        ReimbursementSaveRequest.AllowanceDayRequest day = allowanceDay("2026-04-13", "10458", "武汉");

        assertThatThrownBy(() -> validateAllowanceDays(itinerary, List.of(day)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("每日补助城市必须与行程目的地一致");
    }

    @Test
    void rejectsMealAmountAboveDatabaseStandard() {
        ReimbursementSaveRequest.AllowanceDayRequest day = allowanceDay("2026-04-13", "10119", "北京");
        day.setMealSelected(1);
        day.setMealAmount(new BigDecimal("100000.00"));
        day.setTrafficSelected(0);
        day.setCommunicationSelected(0);

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                service,
                "fillAllowanceDay",
                new com.enpenseSystem.entity.FkReimAllowanceDay(),
                0L,
                0L,
                day
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("餐补不能超过标准金额100.00");
    }

    private void validateAllowanceDays(ReimbursementSaveRequest.ItineraryRequest itinerary,
                                       List<ReimbursementSaveRequest.AllowanceDayRequest> days) {
        ReflectionTestUtils.invokeMethod(service, "validateAllowanceDaysForItinerary", itinerary, days);
    }

    private ReimbursementSaveRequest.ItineraryRequest itinerary() {
        ReimbursementSaveRequest.ItineraryRequest itinerary = new ReimbursementSaveRequest.ItineraryRequest();
        itinerary.setStartCityCode("10458");
        itinerary.setStartCityName("武汉");
        itinerary.setEndCityCode("10119");
        itinerary.setEndCityName("北京");
        itinerary.setStartDate(LocalDate.parse("2026-04-13"));
        itinerary.setEndDate(LocalDate.parse("2026-04-17"));
        return itinerary;
    }

    private ReimbursementSaveRequest.AllowanceDayRequest allowanceDay(String date, String cityCode, String cityName) {
        ReimbursementSaveRequest.AllowanceDayRequest day = new ReimbursementSaveRequest.AllowanceDayRequest();
        day.setAllowanceDate(LocalDate.parse(date));
        day.setCityCode(cityCode);
        day.setCityName(cityName);
        day.setMealSelected(1);
        day.setMealAmount(new BigDecimal("80.00"));
        day.setTrafficSelected(1);
        day.setTrafficAmount(new BigDecimal("40.00"));
        day.setCommunicationSelected(1);
        day.setCommunicationAmount(new BigDecimal("40.00"));
        return day;
    }
}
