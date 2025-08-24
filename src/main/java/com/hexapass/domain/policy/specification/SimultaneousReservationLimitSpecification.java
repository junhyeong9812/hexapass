package com.hexapass.domain.policy.specification;

import com.hexapass.domain.policy.ReservationContext;
import com.hexapass.domain.policy.ReservationSpecification;

/**
 * 동시 예약 수 제한 확인 사양
 * 회원의 현재 활성 예약 수가 멤버십 허용 한도 내에 있는지 확인
 */
public class SimultaneousReservationLimitSpecification implements ReservationSpecification {

    @Override
    public boolean isSatisfiedBy(ReservationContext context) {
        return context.getMember().canReserveSimultaneously(
                context.getCurrentActiveReservations()
        );
    }

    @Override
    public String getDescription() {
        return "동시 예약 수 제한";
    }
}