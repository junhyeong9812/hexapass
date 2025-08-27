package com.hexapass.domain.policy.specification;

import com.hexapass.domain.policy.ReservationContext;
import com.hexapass.domain.policy.ReservationSpecification;

/**
 * NOT 조건 사양 - 개선된 버전
 * 기존 사양과 반대 조건을 표현
 * 부정 조건의 의미를 더 명확하게 표현하고 중첩 부정을 최적화
 */
public class NotSpecification implements ReservationSpecification {

    private final ReservationSpecification specification;
    private final String customDescription; // 사용자 정의 설명 (선택적)

    public NotSpecification(ReservationSpecification specification) {
        this.specification = validateNotNull(specification, "사양");
        this.customDescription = null;
    }

    public NotSpecification(ReservationSpecification specification, String customDescription) {
        this.specification = validateNotNull(specification, "사양");
        this.customDescription = customDescription != null ? customDescription.trim() : null;
    }

    /**
     * 사용자 정의 설명과 함께 NOT 사양 생성
     */
    public static NotSpecification withDescription(ReservationSpecification specification, String description) {
        return new NotSpecification(specification, description);
    }

    /**
     * 이중 부정 최적화 - NOT(NOT(spec)) -> spec
     */
    public static ReservationSpecification optimized(ReservationSpecification specification) {
        if (specification instanceof NotSpecification) {
            // NOT(NOT(spec)) = spec
            return ((NotSpecification) specification).getSpecification();
        }
        return new NotSpecification(specification);
    }

    @Override
    public boolean isSatisfiedBy(ReservationContext context) {
        return !specification.isSatisfiedBy(context);
    }

    @Override
    public String getDescription() {
        if (customDescription != null && !customDescription.isEmpty()) {
            return customDescription;
        }

        // 이중 부정의 경우 좀 더 자연스럽게 표현
        if (specification instanceof NotSpecification) {
            return specification.getDescription().replace("NOT (", "").replaceFirst("\\)$", "");
        }

        return String.format("NOT (%s)", specification.getDescription());
    }

    /**
     * 원본 사양이 만족되었는지 확인
     */
    public boolean isOriginalSatisfied(ReservationContext context) {
        return specification.isSatisfiedBy(context);
    }

    /**
     * 부정 사양의 이유 설명
     */
    public String getNegationReason(ReservationContext context) {
        if (specification.isSatisfiedBy(context)) {
            return String.format("조건 '%s'이(가) 만족되어서 부정 조건이 불만족됨",
                    specification.getDescription());
        } else {
            return String.format("조건 '%s'이(가) 불만족되어서 부정 조건이 만족됨",
                    specification.getDescription());
        }
    }

    /**
     * 중첩된 NOT의 깊이 계산
     */
    public int getNegationDepth() {
        if (specification instanceof NotSpecification) {
            return 1 + ((NotSpecification) specification).getNegationDepth();
        }
        return 1;
    }

    /**
     * 가장 내부의 원본 사양 반환
     */
    public ReservationSpecification getInnerMostSpecification() {
        ReservationSpecification current = specification;
        while (current instanceof NotSpecification) {
            current = ((NotSpecification) current).getSpecification();
        }
        return current;
    }

    /**
     * 짝수 번 부정되었는지 확인 (짝수면 원본과 같은 의미)
     */
    public boolean isEvenNegation() {
        return getNegationDepth() % 2 == 0;
    }

    // =========================
    // Getter 메서드들
    // =========================

    public ReservationSpecification getSpecification() {
        return specification;
    }

    public String getCustomDescription() {
        return customDescription;
    }

    public boolean hasCustomDescription() {
        return customDescription != null && !customDescription.isEmpty();
    }

    // =========================
    // Object 메서드 오버라이드
    // =========================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        NotSpecification that = (NotSpecification) obj;
        return specification.equals(that.specification);
    }

    @Override
    public int hashCode() {
        return specification.hashCode() * 31; // NOT을 구분하기 위해 31 곱하기
    }

    @Override
    public String toString() {
        return "NotSpecification{" + getDescription() + "}";
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