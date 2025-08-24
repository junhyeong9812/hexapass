package com.hexapass.domain.policy.specification;

import com.hexapass.domain.policy.ReservationContext;
import com.hexapass.domain.policy.ReservationSpecification;
import com.hexapass.domain.type.MemberStatus;

/**
 * 회원 상태 확인 사양
 * 회원이 활성 상태이고 멤버십이 유효한지 확인
 */
public class ActiveMemberSpecification implements ReservationSpecification {

    @Override
    public boolean isSatisfiedBy(ReservationContext context) {
        return context.getMember() != null &&
                context.getMember().getStatus() == MemberStatus.ACTIVE &&
                context.getMember().hasMembershipActive();
    }

    @Override
    public String getDescription() {
        return "활성 회원 여부";
    }
}