package com.hexapass.domain.policy.cancellation;

import com.hexapass.domain.common.Money;
import com.hexapass.domain.policy.CancellationContext;
import com.hexapass.domain.policy.CancellationPolicy;

import java.math.BigDecimal;

/**
 * 엄격한 취소 정책
 * - 48시간 전: 무료
 * - 24-48시간 전: 30%
 * - 6-24시간 전: 60%
 * - 6시간 미만: 90%
 * - 예약 시간 이후: 100%
 * - 예약 당일 취소: 추가 제한
 */
public class StrictCancellationPolicy implements CancellationPolicy {

    private static final BigDecimal FREE_RATE = BigDecimal.ZERO;
    private static final BigDecimal LOW_FEE_RATE = new BigDecimal("0.30");
    private static final BigDecimal MEDIUM_FEE_RATE = new BigDecimal("0.60");
    private static final BigDecimal HIGH_FEE_RATE = new BigDecimal("0.90");
    private static final BigDecimal FULL_FEE_RATE = BigDecimal.ONE;

    @Override
    public Money calculateCancellationFee(Money originalPrice, CancellationContext context) {
        if (!isCancellationAllowed(context)) {
            return originalPrice;
        }

        BigDecimal feeRate = determineFeeRate(context);
        return originalPrice.multiply(feeRate);
    }

    @Override
    public boolean isCancellationAllowed(CancellationContext context) {
        // 예약 시간 이후에는 취소 불가
        if (context.isAfterReservationTime()) {
            return false;
        }

        // 예약 당일 취소 시 특정 조건에서만 허용
        if (context.isSameDayAsCancellation()) {
            return context.getHoursUntilReservation() >= 6; // 6시간 전까지만 허용
        }

        return true;
    }

    @Override
    public String getCancellationDenialReason(CancellationContext context) {
        if (context.isAfterReservationTime()) {
            return "예약 시간이 이미 지났으므로 취소할 수 없습니다";
        }

        if (context.isSameDayAsCancellation() && context.getHoursUntilReservation() < 6) {
            return "예약 당일에는 6시간 전까지만 취소 가능합니다";
        }

        return null;
    }

    @Override
    public String getDescription() {
        return "엄격한 취소 정책 - 높은 수수료 및 당일 취소 제한";
    }

    /**
     * 취소 시점에 따른 수수료율 결정
     */
    private BigDecimal determineFeeRate(CancellationContext context) {
        long hoursUntilReservation = context.getHoursUntilReservation();

        if (context.isAfterReservationTime()) {
            return FULL_FEE_RATE; // 100%
        } else if (hoursUntilReservation < 6) {
            return HIGH_FEE_RATE; // 90%
        } else if (hoursUntilReservation < 24) {
            return MEDIUM_FEE_RATE; // 60%
        } else if (hoursUntilReservation < 48) {
            return LOW_FEE_RATE; // 30%
        } else {
            return FREE_RATE; // 0%
        }
    }

    /**
     * 취소 수수료율 정보 제공
     */
    public String getFeeRateInfo(CancellationContext context) {
        if (!isCancellationAllowed(context)) {
            return "취소 불가: " + getCancellationDenialReason(context);
        }

        BigDecimal feeRate = determineFeeRate(context);
        int percentage = feeRate.multiply(BigDecimal.valueOf(100)).intValue();

        return String.format("취소 수수료: %d%% (엄격한 정책, %d시간 전)",
                percentage, context.getHoursUntilReservation());
    }
}