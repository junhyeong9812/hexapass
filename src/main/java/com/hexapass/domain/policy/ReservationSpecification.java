package com.hexapass.domain.policy;

import com.hexapass.domain.policy.specification.AndSpecification;
import com.hexapass.domain.policy.specification.NotSpecification;
import com.hexapass.domain.policy.specification.OrSpecification;

/**
 * 예약 조건을 나타내는 사양 인터페이스 (Specification Pattern)
 * 복잡한 비즈니스 규칙을 조합 가능한 객체로 표현
 */
public interface ReservationSpecification {

    /**
     * 주어진 예약 컨텍스트가 이 사양을 만족하는지 확인
     *
     * @param context 예약 컨텍스트
     * @return 조건을 만족하면 true, 그렇지 않으면 false
     */
    boolean isSatisfiedBy(ReservationContext context);

    /**
     * AND 조건으로 다른 사양과 결합
     *
     * @param other 결합할 다른 사양
     * @return 두 사양이 모두 만족되어야 하는 새로운 사양
     */
    default ReservationSpecification and(ReservationSpecification other) {
        return new AndSpecification(this, other);
    }

    /**
     * OR 조건으로 다른 사양과 결합
     *
     * @param other 결합할 다른 사양
     * @return 두 사양 중 하나라도 만족되면 되는 새로운 사양
     */
    default ReservationSpecification or(ReservationSpecification other) {
        return new OrSpecification(this, other);
    }

    /**
     * NOT 조건 (부정)
     *
     * @return 이 사양과 반대 조건인 새로운 사양
     */
    default ReservationSpecification not() {
        return new NotSpecification(this);
    }

    /**
     * 사양 설명
     *
     * @return 이 사양이 검사하는 조건에 대한 설명
     */
    String getDescription();
}