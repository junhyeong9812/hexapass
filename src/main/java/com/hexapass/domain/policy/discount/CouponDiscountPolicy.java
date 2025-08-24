package com.hexapass.domain.policy.discount;

import com.hexapass.domain.common.Money;
import com.hexapass.domain.policy.DiscountContext;
import com.hexapass.domain.policy.DiscountPolicy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

/**
 * 쿠폰 할인 정책
 */
public class CouponDiscountPolicy implements DiscountPolicy {

    private final String couponCode;
    private final Money discountAmount;
    private final BigDecimal discountRate;
    private final LocalDate validFrom;
    private final LocalDate validUntil;
    private final Money minimumAmount;
    private final Set<String> targetMemberIds; // 특정 회원 대상
    private final boolean isRateDiscount;

    private CouponDiscountPolicy(String couponCode, Money discountAmount, BigDecimal discountRate,
                                 LocalDate validFrom, LocalDate validUntil, Money minimumAmount,
                                 Set<String> targetMemberIds, boolean isRateDiscount) {
        this.couponCode = validateNotBlank(couponCode, "쿠폰 코드");
        this.discountAmount = discountAmount;
        this.discountRate = discountRate;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
        this.minimumAmount = minimumAmount;
        this.targetMemberIds = targetMemberIds != null ? Set.copyOf(targetMemberIds) : Set.of();
        this.isRateDiscount = isRateDiscount;
    }

    /**
     * 정액 할인 쿠폰 생성
     */
    public static CouponDiscountPolicy amountCoupon(String couponCode, Money discountAmount,
                                                    LocalDate validFrom, LocalDate validUntil) {
        return new CouponDiscountPolicy(couponCode, discountAmount, null,
                validFrom, validUntil, null, null, false);
    }

    /**
     * 정률 할인 쿠폰 생성
     */
    public static CouponDiscountPolicy rateCoupon(String couponCode, BigDecimal discountRate,
                                                  LocalDate validFrom, LocalDate validUntil) {
        return new CouponDiscountPolicy(couponCode, null, discountRate,
                validFrom, validUntil, null, null, true);
    }

    /**
     * 최소 금액 제한이 있는 정액 할인 쿠폰
     */
    public static CouponDiscountPolicy amountCouponWithMinimum(String couponCode, Money discountAmount,
                                                               LocalDate validFrom, LocalDate validUntil,
                                                               Money minimumAmount) {
        return new CouponDiscountPolicy(couponCode, discountAmount, null,
                validFrom, validUntil, minimumAmount, null, false);
    }

    /**
     * 특정 회원 대상 쿠폰
     */
    public static CouponDiscountPolicy targetedCoupon(String couponCode, Money discountAmount,
                                                      LocalDate validFrom, LocalDate validUntil,
                                                      Set<String> targetMemberIds) {
        return new CouponDiscountPolicy(couponCode, discountAmount, null,
                validFrom, validUntil, null, targetMemberIds, false);
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

        if (isRateDiscount) {
            Money discountAmountCalculated = originalPrice.multiply(discountRate);
            return originalPrice.subtract(discountAmountCalculated);
        } else {
            if (discountAmount.isGreaterThan(originalPrice)) {
                return Money.zero(originalPrice.getCurrency());
            }
            return originalPrice.subtract(discountAmount);
        }
    }

    @Override
    public boolean isApplicable(DiscountContext context) {
        // 쿠폰 코드 일치 확인
        if (!couponCode.equals(context.getCouponCode())) {
            return false;
        }

        // 유효 기간 확인
        LocalDate today = context.getPurchaseDate();
        if (validFrom != null && today.isBefore(validFrom)) {
            return false;
        }
        if (validUntil != null && today.isAfter(validUntil)) {
            return false;
        }

        // 특정 회원 대상 확인
        if (!targetMemberIds.isEmpty() && !targetMemberIds.contains(context.getMember().getMemberId())) {
            return false;
        }

        return true;
    }

    @Override
    public String getDescription() {
        String type = isRateDiscount ?
                (discountRate.multiply(BigDecimal.valueOf(100)).intValue() + "% 할인") :
                (discountAmount + " 할인");
        return String.format("쿠폰 할인 [%s] - %s", couponCode, type);
    }

    @Override
    public int getPriority() {
        return 10; // 높은 우선순위
    }

    // =========================
    // Getter 메서드들
    // =========================

    public String getCouponCode() {
        return couponCode;
    }

    public Money getDiscountAmount() {
        return discountAmount;
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public Money getMinimumAmount() {
        return minimumAmount;
    }

    public boolean isRateDiscount() {
        return isRateDiscount;
    }

    // =========================
    // 검증 메서드들 (private)
    // =========================

    private String validateNotBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "은 null이거나 빈 값일 수 없습니다");
        }
        return value.trim();
    }
}