package com.enpenseSystem.service.support;

import com.enpenseSystem.dto.AllowanceGenerateRequest;
import com.enpenseSystem.dto.ReimbursementSaveRequest;
import com.enpenseSystem.entity.FkCityAllowance;
import com.enpenseSystem.entity.FkReimAllowanceDay;
import com.enpenseSystem.service.FkCityAllowanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AllowanceCalculatorTests {

    private AllowanceCalculator calculator;

    @BeforeEach
    void setUp() {
        FkCityAllowanceService cityAllowanceService = mock(FkCityAllowanceService.class);
        when(cityAllowanceService.getOne(any(), eq(false))).thenReturn(beijing());
        calculator = new AllowanceCalculator(cityAllowanceService);
    }

    @Test
    void generatesOneAllowanceDayForEachTravelDate() {
        AllowanceGenerateRequest request = new AllowanceGenerateRequest();
        request.setEndCityCode("10119");
        request.setEndCityName("北京");
        request.setStartDate(LocalDate.parse("2026-04-13"));
        request.setEndDate(LocalDate.parse("2026-04-17"));

        List<ReimbursementSaveRequest.AllowanceDayRequest> days = calculator.generateAllowanceDays(request);

        assertThat(days).hasSize(5);
        assertThat(days.get(0).getCityName()).isEqualTo("北京");
        assertThat(days.get(0).getDayAmount()).isEqualByComparingTo("180.00");
    }

    @Test
    void rejectsAllowanceAmountAboveDatabaseStandard() {
        ReimbursementSaveRequest.AllowanceDayRequest day = day();
        day.setMealAmount(new BigDecimal("100000.00"));

        assertThatThrownBy(() -> calculator.fillAllowanceDay(new FkReimAllowanceDay(), 1L, 2L, day))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("餐补不能超过标准金额100.00");
    }

    @Test
    void unselectedAllowanceItemIsForcedToZero() {
        ReimbursementSaveRequest.AllowanceDayRequest day = day();
        day.setTrafficSelected(0);
        day.setTrafficAmount(new BigDecimal("40.00"));

        FkReimAllowanceDay entity = new FkReimAllowanceDay();
        calculator.fillAllowanceDay(entity, 1L, 2L, day);

        assertThat(entity.getTrafficAmount()).isEqualByComparingTo("0.00");
        assertThat(entity.getDayAmount()).isEqualByComparingTo("120.00");
        assertThat(day.getDayAmount()).isEqualByComparingTo("120.00");
    }

    private ReimbursementSaveRequest.AllowanceDayRequest day() {
        ReimbursementSaveRequest.AllowanceDayRequest day = new ReimbursementSaveRequest.AllowanceDayRequest();
        day.setAllowanceDate(LocalDate.parse("2026-04-13"));
        day.setCityCode("10119");
        day.setCityName("北京");
        day.setMealSelected(1);
        day.setMealAmount(new BigDecimal("80.00"));
        day.setTrafficSelected(1);
        day.setTrafficAmount(new BigDecimal("40.00"));
        day.setCommunicationSelected(1);
        day.setCommunicationAmount(new BigDecimal("40.00"));
        return day;
    }

    private FkCityAllowance beijing() {
        FkCityAllowance standard = new FkCityAllowance();
        standard.setCityCode("10119");
        standard.setCityName("北京");
        standard.setCityLevel(1);
        standard.setMealStandard(new BigDecimal("100.00"));
        standard.setTrafficStandard(new BigDecimal("40.00"));
        standard.setCommunicationStandard(new BigDecimal("40.00"));
        return standard;
    }
}
