package com.hexapass.domain.policy.cancellation;

import com.hexapass.domain.common.Money;
import com.hexapass.domain.policy.CancellationContext;
import com.hexapass.domain.policy.CancellationPolicy;

import java.math.BigDecimal;

/**
 * 표준 취소 정책
 * 시간대별 차등 취소 수수료 적용
 * - 24시간 전: 무료
 * - 6-24시간 전: 20%
 * - 2-6시간 전: 50%
 * - 2시간 미만: 80%
 * - 예약 시간 이후: 100%
 */
public class StandardCancellationPolicy implements CancellationPolicy {

    private static final BigDecimal FREE_RATE = BigDecimal.ZERO;
    private static final BigDecimal LOW_FEE_RATE = new BigDecimal("0.20");
    private static final BigDecimal MEDIUM_FEE_RATE = new BigDecimal("0.50");
    private static final BigDecimal HIGH_FEE_RATE = new BigDecimal("0.80");
    private static final BigDecimal FULL_FEE_RATE = BigDecimal.ONE;

    @Override
    public Money calculateCancellationFee(Money originalPrice, CancellationContext context) {
        if (!isCancellationAllowed(context)) {
            return originalPrice; // 취소 불가능한 경우 전액
        }

        BigDecimal feeRate = determineFeeRate(context);
        return originalPrice.multiply(feeRate);
    }

    @Override
    public boolean isCancellationAllowed(CancellationContext context) {
        // 기본적으로 모든 취소를 허용 (단, 수수료는 차등 적용)
        return true;
    }

    @Override
    public String getCancellationDenialReason(CancellationContext context) {
        return null; // 표준 정책에서는 모든 취소 허용
    }

    @Override
    public String getDescription() {
        return "표준 취소 정책 - 시간대별 차등 수수료 (24시간 전 무료, 이후 점진적 증가)";
    }

    /**
     * 취소 시점에 따른 수수료율 결정
     */
    private BigDecimal determineFeeRate(CancellationContext context) {
        long hoursUntilReservation = context.getHoursUntilReservation();

        if (context.isAfterReservationTime()) {
            return FULL_FEE_RATE; // 100% - 예약 시간 이후
        } else if (hoursUntilReservation < 2) {
            return HIGH_FEE_RATE; // 80% - 2시간 미만
        } else if (hoursUntilReservation < 6) {
            return MEDIUM_FEE_RATE; // 50% - 2-6시간 전
        } else if (hoursUntilReservation < 24) {
            return LOW_FEE_RATE; // 20% - 6-24시간 전
        } else {
            return FREE_RATE; // 0% - 24시간 이전
        }
    }

    /**
     * 취소 수수료율 정보 제공
     */
    public String getFeeRateInfo(CancellationContext context) {
        BigDecimal feeRate = determineFeeRate(context);
        int percentage = feeRate.multiply(BigDecimal.valueOf(100)).intValue();

        return String.format("취소 수수료: %d%% (%s까지 %d시간 남음)",
                percentage,
                context.getReservationTime().toString(),
                context.getHoursUntilReservation());
    }
}