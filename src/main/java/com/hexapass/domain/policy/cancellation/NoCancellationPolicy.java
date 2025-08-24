package com.hexapass.domain.policy.cancellation;

import com.hexapass.domain.common.Money;
import com.hexapass.domain.policy.CancellationContext;
import com.hexapass.domain.policy.CancellationPolicy;

/**
 * 취소 불가 정책
 * 특별 이벤트나 할인 예약에 적용되는 취소 불가 정책
 * 예외 상황에서만 취소 허용 (시스템 오류, 불가항력 등)
 */
public class NoCancellationPolicy implements CancellationPolicy {

    private final boolean allowEmergencyCancel; // 응급 상황 취소 허용 여부

    public NoCancellationPolicy() {
        this(false);
    }

    public NoCancellationPolicy(boolean allowEmergencyCancel) {
        this.allowEmergencyCancel = allowEmergencyCancel;
    }

    /**
     * 응급 상황 취소를 허용하는 정책
     */
    public static NoCancellationPolicy withEmergencyAllowance() {
        return new NoCancellationPolicy(true);
    }

    /**
     * 완전히 취소를 금지하는 정책
     */
    public static NoCancellationPolicy strict() {
        return new NoCancellationPolicy(false);
    }

    @Override
    public Money calculateCancellationFee(Money originalPrice, CancellationContext context) {
        // 취소가 허용되지 않으므로 항상 전액 수수료
        return originalPrice;
    }

    @Override
    public boolean isCancellationAllowed(CancellationContext context) {
        if (allowEmergencyCancel) {
            // 응급 상황: 예약 시간 24시간 이전에만 취소 가능
            return context.getHoursUntilReservation() >= 24;
        }

        // 완전 취소 불가
        return false;
    }

    @Override
    public String getCancellationDenialReason(CancellationContext context) {
        if (allowEmergencyCancel) {
            if (context.getHoursUntilReservation() < 24) {
                return "응급 상황을 제외하고는 예약 24시간 전까지만 취소 가능합니다";
            }
            return null; // 취소 가능
        }

        return "이 예약은 취소할 수 없습니다 (취소 불가 정책 적용)";
    }

    @Override
    public String getDescription() {
        if (allowEmergencyCancel) {
            return "취소 불가 정책 - 응급 상황 시 24시간 전까지만 취소 허용";
        }

        return "취소 불가 정책 - 모든 취소 금지";
    }

    /**
     * 응급 상황 취소 허용 여부 반환
     */
    public boolean isEmergencyCancelAllowed() {
        return allowEmergencyCancel;
    }

    /**
     * 정책 정보 제공
     */
    public String getPolicyInfo(CancellationContext context) {
        if (!isCancellationAllowed(context)) {
            return String.format("취소 불가: %s", getCancellationDenialReason(context));
        }

        if (allowEmergencyCancel) {
            return String.format("응급 취소 가능 (%d시간 전)", context.getHoursUntilReservation());
        }

        return "취소 불가 정책 적용";
    }
}