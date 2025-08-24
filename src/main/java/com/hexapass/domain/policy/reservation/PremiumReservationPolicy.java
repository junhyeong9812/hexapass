package com.hexapass.domain.policy.reservation;

import com.hexapass.domain.policy.ReservationContext;
import com.hexapass.domain.policy.ReservationPolicy;
import com.hexapass.domain.policy.ReservationSpecification;
import com.hexapass.domain.policy.specification.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 프리미엄 예약 정책 (더 관대한 조건들)
 * 동시 예약 수 제한과 선예약 기간 제한을 완화
 */
public class PremiumReservationPolicy implements ReservationPolicy {

    private final ReservationSpecification specification;

    public PremiumReservationPolicy() {
        this.specification = new ActiveMemberSpecification()
                .and(new MembershipPrivilegeSpecification())
                .and(new ResourceCapacitySpecification())
                .and(new ValidReservationTimeSpecification(730)); // 2년까지 선예약 허용
        // 동시 예약 수 제한과 선예약 기간 제한을 완화
    }

    @Override
    public boolean canReserve(ReservationContext context) {
        return specification.isSatisfiedBy(context);
    }

    @Override
    public String getViolationReason(ReservationContext context) {
        List<String> violations = new ArrayList<>();

        if (!new ActiveMemberSpecification().isSatisfiedBy(context)) {
            violations.add("비활성 회원이거나 멤버십이 만료됨");
        }
        if (!new MembershipPrivilegeSpecification().isSatisfiedBy(context)) {
            violations.add("멤버십에서 해당 리소스 이용 권한 없음");
        }
        if (!new ResourceCapacitySpecification().isSatisfiedBy(context)) {
            violations.add("리소스 수용 인원 초과");
        }
        if (!new ValidReservationTimeSpecification(730).isSatisfiedBy(context)) {
            violations.add("유효하지 않은 예약 시간 (2년 이내)");
        }

        return violations.isEmpty() ? "예약 가능" : String.join(", ", violations);
    }

    @Override
    public String getDescription() {
        return "프리미엄 예약 정책 - 관대한 예약 조건들";
    }
}