package com.enpenseSystem.service.support;

import com.enpenseSystem.dto.ReimbursementDetailVO;
import com.enpenseSystem.dto.ReimbursementSaveRequest;
import com.enpenseSystem.entity.FkReimAllocation;
import com.enpenseSystem.entity.FkReimAllowanceDay;
import com.enpenseSystem.entity.FkReimItinerary;
import com.enpenseSystem.entity.FkReimMain;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReimbursementDetailAssemblerTests {

    private final ReimbursementDetailAssembler assembler = new ReimbursementDetailAssembler();

    @Test
    void assemblesAllowanceDaysUnderTheirItinerary() {
        FkReimMain main = main();
        FkReimItinerary first = itinerary(10L, "武汉-北京");
        FkReimItinerary second = itinerary(11L, "北京-上海");
        FkReimAllowanceDay firstDay = allowanceDay(100L, 10L, "2026-04-13", "北京");
        FkReimAllowanceDay secondDay = allowanceDay(101L, 11L, "2026-04-14", "上海");
        FkReimAllocation allocation = allocation();

        ReimbursementDetailVO detail = assembler.assemble(
                main,
                List.of(first, second),
                List.of(firstDay, secondDay),
                List.of(allocation)
        );

        assertThat(detail.getReimNo()).isEqualTo("CLBX202606160001");
        assertThat(detail.getItineraries()).hasSize(2);
        assertThat(detail.getItineraries().get(0).getRouteText()).isEqualTo("武汉-北京");
        assertThat(detail.getItineraries().get(0).getAllowanceDays())
                .extracting(ReimbursementDetailVO.AllowanceDayVO::getId)
                .containsExactly(100L);
        assertThat(detail.getItineraries().get(1).getAllowanceDays())
                .extracting(ReimbursementDetailVO.AllowanceDayVO::getCityName)
                .containsExactly("上海");
        assertThat(detail.getAllocations())
                .extracting(ReimbursementDetailVO.AllocationVO::getAllocationOwnerName)
                .containsExactly("胜意科技北京分公司");
    }

    @Test
    void convertsDetailBackToSaveRequestForCopyAndSubmitValidation() {
        ReimbursementDetailVO detail = assembler.assemble(
                main(),
                List.of(itinerary(10L, "武汉-北京")),
                List.of(allowanceDay(100L, 10L, "2026-04-13", "北京")),
                List.of(allocation())
        );

        ReimbursementSaveRequest request = assembler.toSaveRequest(detail);

        assertThat(request.getReimNo()).isEqualTo("CLBX202606160001");
        assertThat(request.getItineraries()).hasSize(1);
        assertThat(request.getItineraries().get(0).getAllowanceDays()).hasSize(1);
        assertThat(request.getItineraries().get(0).getAllowanceDays().get(0).getMealAmount())
                .isEqualByComparingTo("80.00");
        assertThat(request.getAllocations()).hasSize(1);
        assertThat(request.getAllocations().get(0).getAllocationRatio())
                .isEqualByComparingTo("1.000000");
    }

    private FkReimMain main() {
        FkReimMain main = new FkReimMain();
        main.setId(1L);
        main.setReimNo("CLBX202606160001");
        main.setTitle("北京出差");
        main.setAllowanceAmount(new BigDecimal("160.00"));
        return main;
    }

    private FkReimItinerary itinerary(Long id, String routeText) {
        FkReimItinerary itinerary = new FkReimItinerary();
        itinerary.setId(id);
        itinerary.setMainId(1L);
        itinerary.setTravelerName("徐年年");
        itinerary.setStartCityName("武汉");
        itinerary.setEndCityName(routeText.endsWith("北京") ? "北京" : "上海");
        itinerary.setStartDate(LocalDate.parse("2026-04-13"));
        itinerary.setEndDate(LocalDate.parse("2026-04-13"));
        itinerary.setRouteText(routeText);
        return itinerary;
    }

    private FkReimAllowanceDay allowanceDay(Long id, Long itineraryId, String date, String cityName) {
        FkReimAllowanceDay day = new FkReimAllowanceDay();
        day.setId(id);
        day.setMainId(1L);
        day.setItineraryId(itineraryId);
        day.setAllowanceDate(LocalDate.parse(date));
        day.setCityName(cityName);
        day.setMealSelected(1);
        day.setMealAmount(new BigDecimal("80.00"));
        day.setTrafficSelected(1);
        day.setTrafficAmount(new BigDecimal("40.00"));
        day.setCommunicationSelected(1);
        day.setCommunicationAmount(new BigDecimal("40.00"));
        day.setDayAmount(new BigDecimal("160.00"));
        return day;
    }

    private FkReimAllocation allocation() {
        FkReimAllocation allocation = new FkReimAllocation();
        allocation.setId(20L);
        allocation.setMainId(1L);
        allocation.setAllocationOwnerType("COMPANY");
        allocation.setAllocationOwnerName("胜意科技北京分公司");
        allocation.setAllocationRatio(new BigDecimal("1.000000"));
        allocation.setAllocationAmount(new BigDecimal("160.00"));
        allocation.setSortNo(1);
        return allocation;
    }
}
