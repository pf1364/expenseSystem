package com.enpenseSystem.service.support;

import com.enpenseSystem.dto.ReimbursementSaveRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 费用归属及分摊计算器。
 *
 * <p>该组件只处理分摊比例和分摊金额，不保存数据库。提交时要求比例合计为 100%，
 * 金额合计等于后端计算出的报销总额，允许 0.01 元四舍五入误差。</p>
 */
@Component
public class AllocationCalculator {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    /**
     * 校验正式提交时的分摊信息，并在金额为空或为 0 时按比例补算金额。
     */
    public void validateForSubmit(List<ReimbursementSaveRequest.AllocationRequest> allocations, BigDecimal totalAmount) {
        if (allocations == null || allocations.isEmpty()) {
            throw new IllegalArgumentException("费用归属及分摊不能为空");
        }
        BigDecimal ratioTotal = BigDecimal.ZERO;
        BigDecimal amountTotal = ZERO;
        BigDecimal trustedTotalAmount = nvl(totalAmount);
        for (ReimbursementSaveRequest.AllocationRequest allocation : allocations) {
            if (!StringUtils.hasText(allocation.getAllocationOwnerName())) {
                throw new IllegalArgumentException("分摊归属名称不能为空");
            }
            BigDecimal ratio = normalizeRatio(allocation.getAllocationRatio());
            BigDecimal amount = nvl(allocation.getAllocationAmount());
            validateDraftAllocation(ratio, amount);
            if (amount.compareTo(ZERO) == 0 && trustedTotalAmount.compareTo(ZERO) > 0) {
                amount = trustedTotalAmount.multiply(ratio).setScale(2, RoundingMode.HALF_UP);
                allocation.setAllocationAmount(amount);
            }
            allocation.setAllocationRatio(ratio);
            ratioTotal = ratioTotal.add(ratio);
            amountTotal = amountTotal.add(amount);
        }
        if (ratioTotal.subtract(BigDecimal.ONE).abs().compareTo(new BigDecimal("0.000001")) > 0) {
            throw new IllegalArgumentException("分摊比例合计必须为100%");
        }
        if (amountTotal.subtract(trustedTotalAmount).abs().compareTo(new BigDecimal("0.01")) > 0) {
            throw new IllegalArgumentException("分摊金额合计必须等于报销总金额");
        }
    }

    /**
     * 草稿保存时的单行边界校验：比例必须在 0-1，金额不能为负数。
     */
    public void validateDraftAllocation(BigDecimal ratio, BigDecimal amount) {
        if (ratio.compareTo(BigDecimal.ZERO) < 0 || ratio.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("单条分摊比例必须在0到100%之间");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("分摊金额不能小于0");
        }
    }

    /**
     * 统一分摊比例格式，接口和数据库均使用 0-1 小数。
     */
    public BigDecimal normalizeRatio(BigDecimal ratio) {
        if (ratio == null) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }
        return ratio.setScale(6, RoundingMode.HALF_UP);
    }

    /**
     * 金额空值按 0 处理，并统一保留两位小数。
     */
    public BigDecimal nvl(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }
}
