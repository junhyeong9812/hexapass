package com.hexapass.domain.policy;

import com.hexapass.domain.common.Money;

/**
 * 할인 정책을 정의하는 전략 인터페이스
 * 전략 패턴(Strategy Pattern)을 적용하여 다양한 할인 방식을 캡슐화
 */
public interface DiscountPolicy {

    /**
     * 할인을 적용하여 최종 금액을 반환
     *
     * @param originalPrice 원래 가격
     * @param context 할인 적용에 필요한 컨텍스트 정보
     * @return 할인 적용 후 최종 금액
     */
    Money applyDiscount(Money originalPrice, DiscountContext context);

    /**
     * 현재 컨텍스트에서 이 할인이 적용 가능한지 확인
     *
     * @param context 할인 적용 컨텍스트
     * @return 적용 가능하면 true, 불가능하면 false
     */
    boolean isApplicable(DiscountContext context);

    /**
     * 할인 정책 설명 (로깅, 영수증 출력용)
     *
     * @return 할인 정책에 대한 설명
     */
    String getDescription();

    /**
     * 할인 우선순위 (낮은 숫자일수록 높은 우선순위)
     *
     * @return 우선순위 (기본값: 100)
     */
    default int getPriority() {
        return 100;
    }
}