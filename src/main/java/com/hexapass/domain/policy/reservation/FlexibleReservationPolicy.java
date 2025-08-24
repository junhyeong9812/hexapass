package com.hexapass.domain.policy.reservation;

import com.hexapass.domain.policy.ReservationContext;
import com.hexapass.domain.policy.ReservationPolicy;
import com.hexapass.domain.policy.ReservationSpecification;

import java.util.List;

/**
 * 유연한 예약 정책
 * 사양들을 동적으로 조합할 수 있는 정책
 */
public class FlexibleReservationPolicy implements ReservationPolicy {

    private final ReservationSpecification specification;
    private final String description;

    private FlexibleReservationPolicy(ReservationSpecification specification, String description) {
        this.specification = validateNotNull(specification, "예약 사양");
        this.description = description != null ? description : "유연한 예약 정책";
    }

    /**
     * 사양을 조합한 유연한 정책 생성
     */
    public static FlexibleReservationPolicy create(ReservationSpecification specification, String description) {
        return new FlexibleReservationPolicy(specification, description);
    }

    /**
     * 여러 사양을 AND 조건으로 조합한 정책 생성
     */
    public static FlexibleReservationPolicy createWithAnd(List<ReservationSpecification> specifications, String description) {
        if (specifications == null || specifications.isEmpty()) {
            throw new IllegalArgumentException("사양 목록은 null이거나 빈 목록일 수 없습니다");
        }

        ReservationSpecification combinedSpec = specifications.get(0);
        for (int i = 1; i < specifications.size(); i++) {
            combinedSpec = combinedSpec.and(specifications.get(i));
        }

        return new FlexibleReservationPolicy(combinedSpec, description);
    }

    /**
     * 여러 사양을 OR 조건으로 조합한 정책 생성
     */
    public static FlexibleReservationPolicy createWithOr(List<ReservationSpecification> specifications, String description) {
        if (specifications == null || specifications.isEmpty()) {
            throw new IllegalArgumentException("사양 목록은 null이거나 빈 목록일 수 없습니다");
        }

        ReservationSpecification combinedSpec = specifications.get(0);
        for (int i = 1; i < specifications.size(); i++) {
            combinedSpec = combinedSpec.or(specifications.get(i));
        }

        return new FlexibleReservationPolicy(combinedSpec, description);
    }

    @Override
    public boolean canReserve(ReservationContext context) {
        return specification.isSatisfiedBy(context);
    }

    @Override
    public String getViolationReason(ReservationContext context) {
        if (canReserve(context)) {
            return "예약 가능";
        }

        return String.format("조건 미충족: %s", specification.getDescription());
    }

    @Override
    public String getDescription() {
        return description;
    }

    /**
     * 현재 사양 정보 반환
     */
    public ReservationSpecification getSpecification() {
        return specification;
    }

    // =========================
    // 검증 메서드들 (private)
    // =========================

    private <T> T validateNotNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + "은 null일 수 없습니다");
        }
        return value;
    }
}