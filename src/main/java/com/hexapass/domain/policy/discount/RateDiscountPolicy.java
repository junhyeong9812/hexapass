package com.hexapass.domain.policy.discount;

import com.hexapass.domain.common.Money;
import com.hexapass.domain.policy.DiscountContext;
import com.hexapass.domain.policy.DiscountPolicy;

import java.math.BigDecimal;

/**
 * 정률 할인 정책 (예: 10%, 20% 할인)
 */
public class RateDiscountPolicy implements DiscountPolicy {

    private final BigDecimal discountRate; // 0.0 ~ 1.0
    private final String description;
    private final Money minimumAmount; // 최소 할인 적용 금액
    private final Money maximumDiscount; // 최대 할인 금액
    private final int priority;

    private RateDiscountPolicy(BigDecimal discountRate, String description,
                               Money minimumAmount, Money maximumDiscount, int priority) {
        this.discountRate = validateDiscountRate(discountRate);
        this.description = validateNotBlank(description, "설명");
        this.minimumAmount = minimumAmount;
        this.maximumDiscount = maximumDiscount;
        this.priority = priority;
    }

    /**
     * 기본 정률 할인 정책 생성
     */
    public static RateDiscountPolicy create(BigDecimal discountRate, String description) {
        return new RateDiscountPolicy(discountRate, description, null, null, 100);
    }

    /**
     * 최소 금액 제한이 있는 정률 할인 정책
     */
    public static RateDiscountPolicy withMinimum(BigDecimal discountRate, String description, Money minimumAmount) {
        return new RateDiscountPolicy(discountRate, description, minimumAmount, null, 100);
    }

    /**
     * 최대 할인 한도가 있는 정률 할인 정책
     */
    public static RateDiscountPolicy withCap(BigDecimal discountRate, String description, Money maximumDiscount) {
        return new RateDiscountPolicy(discountRate, description, null, maximumDiscount, 100);
    }

    /**
     * 모든 제한이 있는 정률 할인 정책
     */
    public static RateDiscountPolicy withLimits(BigDecimal discountRate, String description,
                                                Money minimumAmount, Money maximumDiscount, int priority) {
        return new RateDiscountPolicy(discountRate, description, minimumAmount, maximumDiscount, priority);
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

        // 할인 금액 계산
        Money discountAmount = originalPrice.multiply(discountRate);

        // 최대 할인 한도 적용
        if (maximumDiscount != null && discountAmount.isGreaterThan(maximumDiscount)) {
            discountAmount = maximumDiscount;
        }

        return originalPrice.subtract(discountAmount);
    }

    @Override
    public boolean isApplicable(DiscountContext context) {
        // 기본적으로 모든 상황에 적용 가능 (서브클래스에서 오버라이드)
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

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    public Money getMinimumAmount() {
        return minimumAmount;
    }

    public Money getMaximumDiscount() {
        return maximumDiscount;
    }

    // =========================
    // 검증 메서드들 (private)
    // =========================

    private BigDecimal validateDiscountRate(BigDecimal rate) {
        if (rate == null || rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("할인율은 0.0 이상 1.0 이하여야 합니다: " + rate);
        }
        return rate;
    }

    private String validateNotBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "은 null이거나 빈 값일 수 없습니다");
        }
        return value.trim();
    }
}