package com.hexapass.domain.policy.cancellation;

import com.hexapass.domain.common.Money;
import com.hexapass.domain.policy.CancellationContext;
import com.hexapass.domain.policy.CancellationPolicy;

import java.math.BigDecimal;

/**
 * 유연한 취소 정책
 * 첫 회 취소 시 더 관대한 조건 적용
 * - 첫 회 취소: 2시간 전까지 무료
 * - 일반 취소: 표준 정책과 동일
 */
public class FlexibleCancellationPolicy implements CancellationPolicy {

    private static final BigDecimal FREE_RATE = BigDecimal.ZERO;
    private static final BigDecimal LOW_FEE_RATE = new BigDecimal("0.10"); // 표준보다 낮은 10%
    private static final BigDecimal MEDIUM_FEE_RATE = new BigDecimal("0.30"); // 표준보다 낮은 30%
    private static final BigDecimal HIGH_FEE_RATE = new BigDecimal("0.60"); // 표준보다 낮은 60%
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
        return true; // 모든 취소 허용
    }

    @Override
    public String getCancellationDenialReason(CancellationContext context) {
        return null;
    }

    @Override
    public String getDescription() {
        return "유연한 취소 정책 - 첫 회 취소자에게 더 관대한 조건 적용";
    }

    /**
     * 취소 시점과 첫 회 취소 여부에 따른 수수료율 결정
     */
    private BigDecimal determineFeeRate(CancellationContext context) {
        long hoursUntilReservation = context.getHoursUntilReservation();
        boolean isFirstTime = context.isFirstTimeCancellation();

        if (context.isAfterReservationTime()) {
            return FULL_FEE_RATE; // 100% - 예약 시간 이후는 동일
        }

        if (isFirstTime) {
            // 첫 회 취소자에게 더 관대한 조건
            if (hoursUntilReservation >= 2) {
                return FREE_RATE; // 0% - 2시간 전까지 무료
            } else {
                return LOW_FEE_RATE; // 10% - 2시간 미만도 낮은 수수료
            }
        } else {
            // 일반 취소자 (표준 정책보다 조금 관대)
            if (hoursUntilReservation < 2) {
                return HIGH_FEE_RATE; // 60%
            } else if (hoursUntilReservation < 6) {
                return MEDIUM_FEE_RATE; // 30%
            } else if (hoursUntilReservation < 24) {
                return LOW_FEE_RATE; // 10%
            } else {
                return FREE_RATE; // 0%
            }
        }
    }

    /**
     * 취소 수수료율 정보 제공
     */
    public String getFeeRateInfo(CancellationContext context) {
        BigDecimal feeRate = determineFeeRate(context);
        int percentage = feeRate.multiply(BigDecimal.valueOf(100)).intValue();
        String customerType = context.isFirstTimeCancellation() ? "첫 회 취소" : "일반 취소";

        return String.format("취소 수수료: %d%% (%s, %d시간 전)",
                percentage, customerType, context.getHoursUntilReservation());
    }
}