package com.hexapass.domain.policy.discount;

import com.hexapass.domain.common.Money;
import com.hexapass.domain.policy.DiscountContext;
import com.hexapass.domain.policy.DiscountPolicy;

/**
 * 정액 할인 정책 (예: 1,000원, 5,000원 할인)
 */
public class AmountDiscountPolicy implements DiscountPolicy {

    private final Money discountAmount;
    private final String description;
    private final Money minimumAmount; // 최소 할인 적용 금액
    private final int priority;

    private AmountDiscountPolicy(Money discountAmount, String description, Money minimumAmount, int priority) {
        this.discountAmount = validateNotNull(discountAmount, "할인 금액");
        this.description = validateNotBlank(description, "설명");
        this.minimumAmount = minimumAmount;
        this.priority = priority;

        if (!discountAmount.isPositive()) {
            throw new IllegalArgumentException("할인 금액은 0보다 커야 합니다: " + discountAmount);
        }
    }

    /**
     * 기본 정액 할인 정책 생성
     */
    public static AmountDiscountPolicy create(Money discountAmount, String description) {
        return new AmountDiscountPolicy(discountAmount, description, null, 100);
    }

    /**
     * 최소 금액 제한이 있는 정액 할인 정책
     */
    public static AmountDiscountPolicy withMinimum(Money discountAmount, String description, Money minimumAmount) {
        return new AmountDiscountPolicy(discountAmount, description, minimumAmount, 100);
    }

    /**
     * 우선순위가 지정된 정액 할인 정책
     */
    public static AmountDiscountPolicy withPriority(Money discountAmount, String description, int priority) {
        return new AmountDiscountPolicy(discountAmount, description, null, priority);
    }

    /**
     * 모든 옵션이 지정된 정액 할인 정책
     */
    public static AmountDiscountPolicy withOptions(Money discountAmount, String description,
                                                   Money minimumAmount, int priority) {
        return new AmountDiscountPolicy(discountAmount, description, minimumAmount, priority);
    }

    @Override
    public Money applyDiscount(Money originalPrice, DiscountContext context) {
        if (!isApplicable(context)) {
            return originalPrice;
        }

        // 최소 금액 체크
        if (minimumAmount != null && originalPrice.isLessThan(minimumAmount)) {
            return originalPrice;
        }

        // 할인 금액이 원래 가격보다 크면 0원까지만
        if (discountAmount.isGreaterThan(originalPrice)) {
            return Money.zero(originalPrice.getCurrency());
        }

        return originalPrice.subtract(discountAmount);
    }

    @Override
    public boolean isApplicable(DiscountContext context) {
        return true;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    // =========================
    // Getter 메서드들
    // =========================

    public Money getDiscountAmount() {
        return discountAmount;
    }

    public Money getMinimumAmount() {
        return minimumAmount;
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

    private String validateNotBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "은 null이거나 빈 값일 수 없습니다");
        }
        return value.trim();
    }
}