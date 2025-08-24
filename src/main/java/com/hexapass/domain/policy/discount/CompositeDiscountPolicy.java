package com.hexapass.domain.policy.discount;

import com.hexapass.domain.common.Money;
import com.hexapass.domain.policy.DiscountContext;
import com.hexapass.domain.policy.DiscountPolicy;

import java.util.List;

/**
 * 여러 할인 정책을 조합하는 복합 정책
 * 데코레이터 패턴과 전략 패턴을 결합
 */
public class CompositeDiscountPolicy implements DiscountPolicy {

    private final List<DiscountPolicy> policies;
    private final String description;
    private final CombinationStrategy strategy;

    /**
     * 할인 조합 전략
     */
    public enum CombinationStrategy {
        SEQUENTIAL("순차 적용 - 모든 할인을 차례로 적용"),
        BEST_DISCOUNT("최고 할인 - 가장 큰 할인만 적용"),
        PRIORITY_FIRST("우선순위 - 가장 높은 우선순위 할인만 적용");

        private final String description;

        CombinationStrategy(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private CompositeDiscountPolicy(List<DiscountPolicy> policies, String description, CombinationStrategy strategy) {
        this.policies = List.copyOf(validateNotEmpty(policies, "할인 정책 목록"));
        this.description = description != null ? description : "복합 할인";
        this.strategy = strategy != null ? strategy : CombinationStrategy.SEQUENTIAL;
    }

    /**
     * 순차적으로 모든 할인을 적용하는 복합 정책
     */
    public static CompositeDiscountPolicy sequential(List<DiscountPolicy> policies, String description) {
        return new CompositeDiscountPolicy(policies, description, CombinationStrategy.SEQUENTIAL);
    }

    /**
     * 가장 큰 할인만 적용하는 복합 정책
     */
    public static CompositeDiscountPolicy bestDiscount(List<DiscountPolicy> policies, String description) {
        return new CompositeDiscountPolicy(policies, description, CombinationStrategy.BEST_DISCOUNT);
    }

    /**
     * 우선순위가 높은 할인만 적용하는 복합 정책
     */
    public static CompositeDiscountPolicy priorityFirst(List<DiscountPolicy> policies, String description) {
        return new CompositeDiscountPolicy(policies, description, CombinationStrategy.PRIORITY_FIRST);
    }

    @Override
    public Money applyDiscount(Money originalPrice, DiscountContext context) {
        if (!isApplicable(context)) {
            return originalPrice;
        }

        return switch (strategy) {
            case SEQUENTIAL -> applySequential(originalPrice, context);
            case BEST_DISCOUNT -> applyBestDiscount(originalPrice, context);
            case PRIORITY_FIRST -> applyPriorityFirst(originalPrice, context);
        };
    }

    /**
     * 순차적으로 모든 할인 적용
     */
    private Money applySequential(Money originalPrice, DiscountContext context) {
        Money currentPrice = originalPrice;
        for (DiscountPolicy policy : policies) {
            if (policy.isApplicable(context)) {
                currentPrice = policy.applyDiscount(currentPrice, context);
            }
        }
        return currentPrice;
    }

    /**
     * 가장 큰 할인만 적용
     */
    private Money applyBestDiscount(Money originalPrice, DiscountContext context) {
        Money bestPrice = originalPrice;
        Money bestDiscount = Money.zero(originalPrice.getCurrency());

        for (DiscountPolicy policy : policies) {
            if (policy.isApplicable(context)) {
                Money discountedPrice = policy.applyDiscount(originalPrice, context);
                Money discountAmount = originalPrice.subtract(discountedPrice);

                if (discountAmount.isGreaterThan(bestDiscount)) {
                    bestPrice = discountedPrice;
                    bestDiscount = discountAmount;
                }
            }
        }

        return bestPrice;
    }

    /**
     * 우선순위가 가장 높은 할인만 적용
     */
    private Money applyPriorityFirst(Money originalPrice, DiscountContext context) {
        return policies.stream()
                .filter(policy -> policy.isApplicable(context))
                .min((p1, p2) -> Integer.compare(p1.getPriority(), p2.getPriority()))
                .map(policy -> policy.applyDiscount(originalPrice, context))
                .orElse(originalPrice);
    }

    @Override
    public boolean isApplicable(DiscountContext context) {
        return policies.stream().anyMatch(policy -> policy.isApplicable(context));
    }

    @Override
    public String getDescription() {
        return description + " (" + strategy.getDescription() + ")";
    }

    @Override
    public int getPriority() {
        return policies.stream()
                .mapToInt(DiscountPolicy::getPriority)
                .min()
                .orElse(Integer.MAX_VALUE);
    }

    // =========================
    // Getter 메서드들
    // =========================

    public List<DiscountPolicy> getPolicies() {
        return List.copyOf(policies);
    }

    public CombinationStrategy getStrategy() {
        return strategy;
    }

    // =========================
    // 검증 메서드들 (private)
    // =========================

    private <T> List<T> validateNotEmpty(List<T> list, String fieldName) {
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException(fieldName + "은 null이거나 빈 목록일 수 없습니다");
        }
        return list;
    }
}