package com.hexapass.domain.policy.specification;

import com.hexapass.domain.policy.ReservationContext;
import com.hexapass.domain.policy.ReservationSpecification;

/**
 * 멤버십 권한 확인 사양
 * 회원의 멤버십에서 해당 리소스 타입에 대한 이용 권한이 있는지 확인
 */
public class MembershipPrivilegeSpecification implements ReservationSpecification {

    @Override
    public boolean isSatisfiedBy(ReservationContext context) {
        return context.getMember().canReserve(
                context.getResourceType(),
                context.getReservationDate()
        );
    }

    @Override
    public String getDescription() {
        return "멤버십 리소스 이용 권한";
    }
}