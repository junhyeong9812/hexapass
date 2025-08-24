package com.hexapass.domain.policy.reservation;

import com.hexapass.domain.policy.ReservationContext;
import com.hexapass.domain.policy.ReservationPolicy;
import com.hexapass.domain.policy.ReservationSpecification;
import com.hexapass.domain.policy.specification.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 표준 예약 정책 (모든 기본 조건들을 포함)
 */
public class StandardReservationPolicy implements ReservationPolicy {

    private final ReservationSpecification specification;

    public StandardReservationPolicy() {
        this.specification = new ActiveMemberSpecification()
                .and(new MembershipPrivilegeSpecification())
                .and(new ResourceCapacitySpecification())
                .and(new ValidReservationTimeSpecification())
                .and(new SimultaneousReservationLimitSpecification())
                .and(new AdvanceReservationLimitSpecification());
    }

    @Override
    public boolean canReserve(ReservationContext context) {
        return specification.isSatisfiedBy(context);
    }

    @Override
    public String getViolationReason(ReservationContext context) {
        List<String> violations = new ArrayList<>();

        // 각 조건을 개별적으로 체크하여 위반 사유 수집
        if (!new ActiveMemberSpecification().isSatisfiedBy(context)) {
            violations.add("비활성 회원이거나 멤버십이 만료됨");
        }
        if (!new MembershipPrivilegeSpecification().isSatisfiedBy(context)) {
            violations.add("멤버십에서 해당 리소스 이용 권한 없음");
        }
        if (!new ResourceCapacitySpecification().isSatisfiedBy(context)) {
            violations.add("리소스 수용 인원 초과");
        }
        if (!new ValidReservationTimeSpecification().isSatisfiedBy(context)) {
            violations.add("유효하지 않은 예약 시간");
        }
        if (!new SimultaneousReservationLimitSpecification().isSatisfiedBy(context)) {
            violations.add("동시 예약 수 한도 초과");
        }
        if (!new AdvanceReservationLimitSpecification().isSatisfiedBy(context)) {
            violations.add("선예약 기간 한도 초과");
        }

        return violations.isEmpty() ? "예약 가능" : String.join(", ", violations);
    }

    @Override
    public String getDescription() {
        return "표준 예약 정책 - 기본 예약 조건들";
    }
}