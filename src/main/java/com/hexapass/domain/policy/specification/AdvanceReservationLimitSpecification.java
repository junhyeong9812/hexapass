package com.hexapass.domain.policy.specification;

import com.hexapass.domain.policy.ReservationContext;
import com.hexapass.domain.policy.ReservationSpecification;

/**
 * 선예약 기간 제한 확인 사양
 * 회원의 멤버십에서 허용하는 선예약 기간 내에 예약하는지 확인
 */
public class AdvanceReservationLimitSpecification implements ReservationSpecification {

    @Override
    public boolean isSatisfiedBy(ReservationContext context) {
        return context.getMember().canReserveInAdvance(
                context.getReservationDate()
        );
    }

    @Override
    public String getDescription() {
        return "선예약 기간 제한";
    }
}