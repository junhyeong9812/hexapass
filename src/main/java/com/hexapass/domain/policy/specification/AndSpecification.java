package com.hexapass.domain.policy.specification;

import com.hexapass.domain.policy.ReservationContext;
import com.hexapass.domain.policy.ReservationSpecification;

/**
 * AND 조건 사양
 * 두 사양이 모두 만족되어야 하는 조건을 표현
 */
public class AndSpecification implements ReservationSpecification {

    private final ReservationSpecification left;
    private final ReservationSpecification right;

    public AndSpecification(ReservationSpecification left, ReservationSpecification right) {
        this.left = validateNotNull(left, "좌측 사양");
        this.right = validateNotNull(right, "우측 사양");
    }

    @Override
    public boolean isSatisfiedBy(ReservationContext context) {
        return left.isSatisfiedBy(context) && right.isSatisfiedBy(context);
    }

    @Override
    public String getDescription() {
        return String.format("(%s) AND (%s)", left.getDescription(), right.getDescription());
    }

    // =========================
    // Getter 메서드들
    // =========================

    public ReservationSpecification getLeft() {
        return left;
    }

    public ReservationSpecification getRight() {
        return right;
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