package com.hexapass.domain.policy.discount;

import com.hexapass.domain.common.Money;
import com.hexapass.domain.policy.DiscountContext;
import com.hexapass.domain.policy.DiscountPolicy;

/**
 * 할인을 적용하지 않는 정책 (Null Object Pattern)
 * 할인 정책이 없는 경우 null 대신 사용하여 NPE를 방지
 */
public class NoDiscountPolicy implements DiscountPolicy {

    @Override
    public Money applyDiscount(Money originalPrice, DiscountContext context) {
        return originalPrice;
    }

    @Override
    public boolean isApplicable(DiscountContext context) {
        return true; // 항상 적용 가능 (할인 안함)
    }

    @Override
    public String getDescription() {
        return "할인 없음";
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE; // 가장 낮은 우선순위
    }
}