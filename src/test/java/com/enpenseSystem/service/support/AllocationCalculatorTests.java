package com.enpenseSystem.service.support;

import com.enpenseSystem.dto.ReimbursementSaveRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AllocationCalculatorTests {

    private final AllocationCalculator calculator = new AllocationCalculator();

    @Test
    void rejectsSubmitWhenRatioTotalIsNotOne() {
        ReimbursementSaveRequest.AllocationRequest allocation = allocation("0.900000", "90.00");

        assertThatThrownBy(() -> calculator.validateForSubmit(List.of(allocation), new BigDecimal("100.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("分摊比例合计必须为100%");
    }

    @Test
    void fillsAmountFromRatioWhenSubmitAmountIsZero() {
        ReimbursementSaveRequest.AllocationRequest allocation = allocation("1.000000", "0.00");

        calculator.validateForSubmit(List.of(allocation), new BigDecimal("160.00"));

        assertThat(allocation.getAllocationAmount()).isEqualByComparingTo("160.00");
        assertThat(allocation.getAllocationRatio()).isEqualByComparingTo("1.000000");
    }

    @Test
    void rejectsNegativeDraftAllocationAmount() {
        assertThatThrownBy(() -> calculator.validateDraftAllocation(new BigDecimal("0.5"), new BigDecimal("-1.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("分摊金额不能小于0");
    }

    private ReimbursementSaveRequest.AllocationRequest allocation(String ratio, String amount) {
        ReimbursementSaveRequest.AllocationRequest allocation = new ReimbursementSaveRequest.AllocationRequest();
        allocation.setAllocationOwnerName("胜意科技北京分公司");
        allocation.setAllocationRatio(new BigDecimal(ratio));
        allocation.setAllocationAmount(new BigDecimal(amount));
        return allocation;
    }
}
