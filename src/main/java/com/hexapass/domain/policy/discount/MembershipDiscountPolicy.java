package com.hexapass.domain.policy.discount;

import com.hexapass.domain.common.Money;
import com.hexapass.domain.policy.DiscountContext;
import com.hexapass.domain.policy.DiscountPolicy;
import com.hexapass.domain.type.MemberStatus;

import java.math.BigDecimal;

/**
 * 멤버십 기본 할인 정책 (멤버십 플랜의 할인율 적용)
 */
public class MembershipDiscountPolicy implements DiscountPolicy {

    @Override
    public Money applyDiscount(Money originalPrice, DiscountContext context) {
        if (!isApplicable(context)) {
            return originalPrice;
        }

        BigDecimal membershipDiscountRate = context.getMembershipPlan().getDiscountRate();
        if (membershipDiscountRate.equals(BigDecimal.ZERO)) {
            return originalPrice;
        }

        Money discountAmount = originalPrice.multiply(membershipDiscountRate);
        return originalPrice.subtract(discountAmount);
    }

    @Override
    public boolean isApplicable(DiscountContext context) {
        return context.getMember() != null &&
                context.getMember().getStatus() == MemberStatus.ACTIVE &&
                context.getMembershipPlan() != null &&
                context.getMembershipPlan().isActive();
    }

    @Override
    public String getDescription() {
        return "멤버십 할인";
    }

    @Override
    public int getPriority() {
        return 50; // 중간 우선순위
    }
}