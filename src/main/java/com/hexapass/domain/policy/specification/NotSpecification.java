package com.hexapass.domain.policy.specification;

import com.hexapass.domain.policy.ReservationContext;
import com.hexapass.domain.policy.ReservationSpecification;

/**
 * NOT 조건 사양
 * 기존 사양과 반대 조건을 표현
 */
public class NotSpecification implements ReservationSpecification {

    private final ReservationSpecification specification;

    public NotSpecification(ReservationSpecification specification) {
        this.specification = validateNotNull(specification, "사양");
    }

    @Override
    public boolean isSatisfiedBy(ReservationContext context) {
        return !specification.isSatisfiedBy(context);
    }

    @Override
    public String getDescription() {
        return String.format("NOT (%s)", specification.getDescription());
    }

    // =========================
    // Getter 메서드들
    // =========================

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