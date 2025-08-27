package com.hexapass.domain.policy.discount;

import com.hexapass.domain.common.Money;
import com.hexapass.domain.policy.DiscountContext;
import com.hexapass.domain.policy.DiscountPolicy;

import java.util.*;

/**
 * 유연한 할인 정책 - 개선된 버전
 * 여러 할인 정책을 동적으로 조합할 수 있는 정책
 * 빌더 패턴과 체이닝을 지원하여 직관적인 정책 구성
 */
public class FlexibleDiscountPolicy implements DiscountPolicy {

    private final List<DiscountPolicy> policies;
    private final String name;
    private final String description;
    private final DiscountLevel level;
    private final CombinationStrategy strategy;
    private final Money maximumTotalDiscount;
    private final Money minimumFinalAmount;
    private final Map<String, Object> metadata;

    public enum DiscountLevel {
        MINIMAL("최소한의 할인만 적용"),
        BASIC("기본적인 할인들 적용"),
        STANDARD("표준적인 모든 할인 적용"),
        GENEROUS("관대한 할인 조합"),
        PREMIUM("프리미엄 회원 할인"),
        CUSTOM("사용자 정의 할인 조합");

        private final String description;
        DiscountLevel(String description) { this.description = description; }
        public String getDescription() { return description; }
    }

    public enum CombinationStrategy {
        SEQUENTIAL("순차적으로 모든 할인 적용"),
        BEST_SINGLE("가장 큰 단일 할인만 적용"),
        PRIORITY_BASED("우선순위 기반 적용"),
        SMART_COMBINATION("스마트 조합 - 최적의 할인 선택");

        private final String description;
        CombinationStrategy(String description) { this.description = description; }
        public String getDescription() { return description; }
    }

    private FlexibleDiscountPolicy(Builder builder) {
        this.policies = List.copyOf(builder.policies);
        this.name = builder.name != null ? builder.name : "유연한 할인 정책";
        this.description = builder.description != null ? builder.description : "동적으로 구성된 할인 정책";
        this.level = builder.level;
        this.strategy = builder.strategy;
        this.maximumTotalDiscount = builder.maximumTotalDiscount;
        this.minimumFinalAmount = builder.minimumFinalAmount;
        this.metadata = Map.copyOf(builder.metadata);
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static FlexibleDiscountPolicy ofLevel(DiscountLevel level, String name) {
        return new Builder(name).withLevel(level).build();
    }

    public static class Builder {
        private final String name;
        private String description;
        private DiscountLevel level = DiscountLevel.CUSTOM;
        private final List<DiscountPolicy> policies = new ArrayList<>();
        private CombinationStrategy strategy = CombinationStrategy.SEQUENTIAL;
        private Money maximumTotalDiscount;
        private Money minimumFinalAmount;
        private final Map<String, Object> metadata = new HashMap<>();

        public Builder(String name) {
            this.name = validateNotBlank(name, "정책명");
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withLevel(DiscountLevel level) {
            this.level = validateNotNull(level, "할인 레벨");
            addLevelBasedPolicies(level);
            return this;
        }

        public Builder withStrategy(CombinationStrategy strategy) {
            this.strategy = strategy;
            return this;
        }

        public Builder withMaximumDiscount(Money maximumDiscount) {
            this.maximumTotalDiscount = maximumDiscount;
            return this;
        }

        public Builder withMinimumFinalAmount(Money minimumAmount) {
            this.minimumFinalAmount = minimumAmount;
            return this;
        }

        public Builder withPolicy(DiscountPolicy policy) {
            if (policy != null) {
                this.policies.add(policy);
            }
            return this;
        }

        public Builder withMembershipDiscount() {
            return withPolicy(new MembershipDiscountPolicy());
        }

        public Builder withAmountDiscount(Money amount, String description) {
            return withPolicy(AmountDiscountPolicy.create(amount, description));
        }

        public Builder withRateDiscount(java.math.BigDecimal rate, String description) {
            return withPolicy(RateDiscountPolicy.create(rate, description));
        }

        public Builder withCouponSupport() {
            // 쿠폰 정책은 런타임에 추가됨 (컨텍스트 기반)
            this.metadata.put("supportsCoupons", true);
            return this;
        }

        public Builder withMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public FlexibleDiscountPolicy build() {
            if (policies.isEmpty() && level == DiscountLevel.CUSTOM) {
                throw new IllegalStateException("최소 하나의 할인 정책이 필요합니다");
            }
            return new FlexibleDiscountPolicy(this);
        }

        private void addLevelBasedPolicies(DiscountLevel level) {
            policies.clear();

            switch (level) {
                case MINIMAL:
                    withMembershipDiscount();
                    break;

                case BASIC:
                    withMembershipDiscount()
                            .withAmountDiscount(Money.won(1000), "기본 할인");
                    break;

                case STANDARD:
                    withMembershipDiscount()
                            .withAmountDiscount(Money.won(2000), "표준 할인")
                            .withCouponSupport();
                    this.strategy = CombinationStrategy.SMART_COMBINATION;
                    break;

                case GENEROUS:
                    withMembershipDiscount()
                            .withRateDiscount(new java.math.BigDecimal("0.15"), "관대한 정률 할인")
                            .withAmountDiscount(Money.won(5000), "관대한 정액 할인")
                            .withCouponSupport();
                    this.strategy = CombinationStrategy.SEQUENTIAL;
                    this.maximumTotalDiscount = Money.won(20000);
                    break;

                case PREMIUM:
                    withMembershipDiscount()
                            .withRateDiscount(new java.math.BigDecimal("0.20"), "프리미엄 할인")
                            .withAmountDiscount(Money.won(10000), "프리미엄 보너스")
                            .withCouponSupport();
                    this.strategy = CombinationStrategy.SEQUENTIAL;
                    this.maximumTotalDiscount = Money.won(50000);
                    this.minimumFinalAmount = Money.won(1000);
                    break;

                case CUSTOM:
                    // 사용자가 직접 정책들을 추가해야 함
                    break;
            }
        }

        private String validateNotBlank(String value, String fieldName) {
            if (value == null || value.trim().isEmpty()) {
                throw new IllegalArgumentException(fieldName + "은 null이거나 빈 값일 수 없습니다");
            }
            return value.trim();
        }

        private <T> T validateNotNull(T value, String fieldName) {
            if (value == null) {
                throw new IllegalArgumentException(fieldName + "은 null일 수 없습니다");
            }
            return value;
        }
    }

    @Override
    public Money applyDiscount(Money originalPrice, DiscountContext context) {
        if (!isApplicable(context)) {
            return originalPrice;
        }

        // 쿠폰 정책을 런타임에 추가
        List<DiscountPolicy> applicablePolicies = new ArrayList<>(policies);
        if (metadata.containsKey("supportsCoupons") && context.hasCoupon()) {
            addCouponPolicy(applicablePolicies, context);
        }

        Money finalPrice = applyStrategy(originalPrice, context, applicablePolicies);

        // 최종 제약 조건 적용
        finalPrice = applyFinalConstraints(originalPrice, finalPrice);

        return finalPrice;
    }

    private Money applyStrategy(Money originalPrice, DiscountContext context, List<DiscountPolicy> applicablePolicies) {
        return switch (strategy) {
            case SEQUENTIAL -> applySequential(originalPrice, context, applicablePolicies);
            case BEST_SINGLE -> applyBestSingle(originalPrice, context, applicablePolicies);
            case PRIORITY_BASED -> applyPriorityBased(originalPrice, context, applicablePolicies);
            case SMART_COMBINATION -> applySmartCombination(originalPrice, context, applicablePolicies);
        };
    }

    private Money applySequential(Money originalPrice, DiscountContext context, List<DiscountPolicy> applicablePolicies) {
        Money currentPrice = originalPrice;
        for (DiscountPolicy policy : applicablePolicies) {
            if (policy.isApplicable(context)) {
                currentPrice = policy.applyDiscount(currentPrice, context);
            }
        }
        return currentPrice;
    }

    private Money applyBestSingle(Money originalPrice, DiscountContext context, List<DiscountPolicy> applicablePolicies) {
        Money bestPrice = originalPrice;
        Money bestDiscount = Money.zero(originalPrice.getCurrency());

        for (DiscountPolicy policy : applicablePolicies) {
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

    private Money applyPriorityBased(Money originalPrice, DiscountContext context, List<DiscountPolicy> applicablePolicies) {
        return applicablePolicies.stream()
                .filter(policy -> policy.isApplicable(context))
                .min(Comparator.comparing(DiscountPolicy::getPriority))
                .map(policy -> policy.applyDiscount(originalPrice, context))
                .orElse(originalPrice);
    }

    private Money applySmartCombination(Money originalPrice, DiscountContext context, List<DiscountPolicy> applicablePolicies) {
        // 정률 할인과 정액 할인을 분리하여 최적 조합 찾기
        List<DiscountPolicy> ratePolicies = new ArrayList<>();
        List<DiscountPolicy> amountPolicies = new ArrayList<>();
        List<DiscountPolicy> otherPolicies = new ArrayList<>();

        for (DiscountPolicy policy : applicablePolicies) {
            if (policy instanceof RateDiscountPolicy) {
                ratePolicies.add(policy);
            } else if (policy instanceof AmountDiscountPolicy) {
                amountPolicies.add(policy);
            } else {
                otherPolicies.add(policy);
            }
        }

        // 1. 기타 정책 먼저 적용
        Money currentPrice = originalPrice;
        for (DiscountPolicy policy : otherPolicies) {
            if (policy.isApplicable(context)) {
                currentPrice = policy.applyDiscount(currentPrice, context);
            }
        }

        // 2. 정률 할인 중 최대값 적용
        Money bestRatePrice = currentPrice;
        if (!ratePolicies.isEmpty()) {
            bestRatePrice = applyBestSingle(currentPrice, context, ratePolicies);
        }

        // 3. 정액 할인 중 최대값 적용
        Money bestAmountPrice = currentPrice;
        if (!amountPolicies.isEmpty()) {
            bestAmountPrice = applyBestSingle(currentPrice, context, amountPolicies);
        }

        // 4. 더 큰 할인을 제공하는 방식 선택
        Money rateDiscount = currentPrice.subtract(bestRatePrice);
        Money amountDiscount = currentPrice.subtract(bestAmountPrice);

        return rateDiscount.isGreaterThan(amountDiscount) ? bestRatePrice : bestAmountPrice;
    }

    private Money applyFinalConstraints(Money originalPrice, Money finalPrice) {
        Money totalDiscount = originalPrice.subtract(finalPrice);

        // 최대 할인 한도 적용
        if (maximumTotalDiscount != null && totalDiscount.isGreaterThan(maximumTotalDiscount)) {
            finalPrice = originalPrice.subtract(maximumTotalDiscount);
        }

        // 최소 최종 금액 보장
        if (minimumFinalAmount != null && finalPrice.isLessThan(minimumFinalAmount)) {
            finalPrice = minimumFinalAmount;
        }

        return finalPrice;
    }

    private void addCouponPolicy(List<DiscountPolicy> policies, DiscountContext context) {
        // 실제 구현에서는 쿠폰 코드를 기반으로 적절한 쿠폰 정책을 조회해야 함
        // 여기서는 예시로 간단한 쿠폰 정책을 생성
        // CouponDiscountPolicy couponPolicy = CouponService.getCouponPolicy(context.getCouponCode());
        // if (couponPolicy != null) {
        //     policies.add(couponPolicy);
        // }
    }

    @Override
    public boolean isApplicable(DiscountContext context) {
        return policies.stream().anyMatch(policy -> policy.isApplicable(context));
    }

    @Override
    public String getDescription() {
        return String.format("%s (%s) - %s", name, level.getDescription(), description);
    }

    @Override
    public int getPriority() {
        return policies.stream()
                .mapToInt(DiscountPolicy::getPriority)
                .min()
                .orElse(Integer.MAX_VALUE);
    }

    /**
     * 할인 분석 결과
     */
    public DiscountAnalysis analyze(Money originalPrice, DiscountContext context) {
        return new DiscountAnalysis(originalPrice, context, this);
    }

    public static class DiscountAnalysis {
        private final Money originalPrice;
        private final Money finalPrice;
        private final Money totalDiscount;
        private final List<String> appliedPolicies;
        private final List<String> skippedPolicies;
        private final DiscountLevel level;
        private final CombinationStrategy strategy;

        private DiscountAnalysis(Money originalPrice, DiscountContext context, FlexibleDiscountPolicy policy) {
            this.originalPrice = originalPrice;
            this.finalPrice = policy.applyDiscount(originalPrice, context);
            this.totalDiscount = originalPrice.subtract(finalPrice);
            this.level = policy.level;
            this.strategy = policy.strategy;
            this.appliedPolicies = new ArrayList<>();
            this.skippedPolicies = new ArrayList<>();

            // 각 정책별 적용 여부 분석
            for (DiscountPolicy pol : policy.policies) {
                if (pol.isApplicable(context)) {
                    appliedPolicies.add(pol.getDescription());
                } else {
                    skippedPolicies.add(pol.getDescription());
                }
            }
        }

        // Getter methods
        public Money getOriginalPrice() { return originalPrice; }
        public Money getFinalPrice() { return finalPrice; }
        public Money getTotalDiscount() { return totalDiscount; }
        public List<String> getAppliedPolicies() { return List.copyOf(appliedPolicies); }
        public List<String> getSkippedPolicies() { return List.copyOf(skippedPolicies); }
        public DiscountLevel getLevel() { return level; }
        public CombinationStrategy getStrategy() { return strategy; }

        public double getDiscountRate() {
            if (originalPrice.isZero()) return 0.0;
            return totalDiscount.getAmount().doubleValue() / originalPrice.getAmount().doubleValue();
        }

        public String getSummary() {
            return String.format("할인 분석: %s → %s (할인액: %s, 할인율: %.1f%%)",
                    originalPrice, finalPrice, totalDiscount, getDiscountRate() * 100);
        }
    }

    // Getter methods
    public List<DiscountPolicy> getPolicies() { return List.copyOf(policies); }
    public String getName() { return name; }
    public DiscountLevel getLevel() { return level; }
    public CombinationStrategy getStrategy() { return strategy; }
    public Money getMaximumTotalDiscount() { return maximumTotalDiscount; }
    public Money getMinimumFinalAmount() { return minimumFinalAmount; }
    public Map<String, Object> getMetadata() { return Map.copyOf(metadata); }
}