package com.hexapass.domain.policy.reservation;

import com.hexapass.domain.policy.ReservationContext;
import com.hexapass.domain.policy.ReservationPolicy;
import com.hexapass.domain.policy.ReservationSpecification;
import com.hexapass.domain.policy.specification.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 제한적 예약 정책 (더 엄격한 조건들)
 * 주말 예약 제한, 시간대 제한 등 추가 조건 적용
 */
public class RestrictiveReservationPolicy implements ReservationPolicy {

    private final ReservationSpecification specification;

    public RestrictiveReservationPolicy() {
        this.specification = new ActiveMemberSpecification()
                .and(new MembershipPrivilegeSpecification())
                .and(new ResourceCapacitySpecification())
                .and(new ValidReservationTimeSpecification(30)) // 30일까지만 선예약 허용
                .and(new SimultaneousReservationLimitSpecification())
                .and(new AdvanceReservationLimitSpecification())
                .and(WeekendReservationSpecification.restrictWeekend()) // 주말 예약 불가
                .and(TimeSlotRestrictionSpecification.businessHours()); // 09:00-18:00만 가능
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
        if (!new ValidReservationTimeSpecification(30).isSatisfiedBy(context)) {
            violations.add("유효하지 않은 예약 시간 (30일 이내만 가능)");
        }
        if (!new SimultaneousReservationLimitSpecification().isSatisfiedBy(context)) {
            violations.add("동시 예약 수 한도 초과");
        }
        if (!new AdvanceReservationLimitSpecification().isSatisfiedBy(context)) {
            violations.add("선예약 기간 한도 초과");
        }
        if (!WeekendReservationSpecification.restrictWeekend().isSatisfiedBy(context)) {
            violations.add("주말 예약 제한");
        }
        if (!TimeSlotRestrictionSpecification.businessHours().isSatisfiedBy(context)) {
            violations.add("비즈니스 시간 외 예약 제한 (09:00-18:00만 가능)");
        }

        return violations.isEmpty() ? "예약 가능" : String.join(", ", violations);
    }

    @Override
    public String getDescription() {
        return "제한적 예약 정책 - 엄격한 예약 조건들";
    }
}