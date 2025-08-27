package com.hexapass.domain.policy.discount;

import com.hexapass.domain.common.Money;
import com.hexapass.domain.policy.DiscountContext;
import com.hexapass.domain.policy.DiscountPolicy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.*;

/**
 * 계절별/시기별 할인 정책
 * 특정 시기에만 적용되는 할인 (성수기/비수기, 홀리데이 등)
 */
public class SeasonalDiscountPolicy implements DiscountPolicy {

    private final Map<SeasonalPeriod, DiscountRule> seasonalRules;
    private final String description;
    private final int priority;

    public enum SeasonalPeriod {
        SPRING("봄철", Month.MARCH, Month.MAY),
        SUMMER("여름철", Month.JUNE, Month.AUGUST),
        AUTUMN("가을철", Month.SEPTEMBER, Month.NOVEMBER),
        WINTER("겨울철", Month.DECEMBER, Month.FEBRUARY),
        HOLIDAY_SEASON("연말연시", null, null), // 12월 20일 - 1월 10일
        BACK_TO_SCHOOL("개학시즌", null, null), // 2월 말 - 3월 초
        SUMMER_VACATION("여름휴가", null, null), // 7월 말 - 8월 말
        OFF_SEASON("비수기", null, null); // 커스텀 정의

        private final String displayName;
        private final Month startMonth;
        private final Month endMonth;

        SeasonalPeriod(String displayName, Month startMonth, Month endMonth) {
            this.displayName = displayName;
            this.startMonth = startMonth;
            this.endMonth = endMonth;
        }

        public String getDisplayName() { return displayName; }
        public Month getStartMonth() { return startMonth; }
        public Month getEndMonth() { return endMonth; }
    }

    public static class DiscountRule {
        private final BigDecimal discountRate;
        private final Money discountAmount;
        private final Money minimumPurchase;
        private final boolean isRateDiscount;
        private final String description;

        private DiscountRule(BigDecimal discountRate, Money discountAmount, Money minimumPurchase,
                             boolean isRateDiscount, String description) {
            this.discountRate = discountRate;
            this.discountAmount = discountAmount;
            this.minimumPurchase = minimumPurchase;
            this.isRateDiscount = isRateDiscount;
            this.description = description;
        }

        public static DiscountRule rate(BigDecimal rate, String description) {
            return new DiscountRule(rate, null, null, true, description);
        }

        public static DiscountRule amount(Money amount, String description) {
            return new DiscountRule(null, amount, null, false, description);
        }

        public static DiscountRule rateWithMinimum(BigDecimal rate, Money minimumPurchase, String description) {
            return new DiscountRule(rate, null, minimumPurchase, true, description);
        }

        public Money apply(Money originalPrice) {
            if (minimumPurchase != null && originalPrice.isLessThan(minimumPurchase)) {
                return originalPrice;
            }

            if (isRateDiscount) {
                Money discount = originalPrice.multiply(discountRate);
                return originalPrice.subtract(discount);
            } else {
                return originalPrice.subtract(discountAmount);
            }
        }

        // Getters
        public BigDecimal getDiscountRate() { return discountRate; }
        public Money getDiscountAmount() { return discountAmount; }
        public Money getMinimumPurchase() { return minimumPurchase; }
        public boolean isRateDiscount() { return isRateDiscount; }
        public String getDescription() { return description; }
    }

    private SeasonalDiscountPolicy(Map<SeasonalPeriod, DiscountRule> seasonalRules, String description, int priority) {
        this.seasonalRules = Map.copyOf(seasonalRules);
        this.description = description;
        this.priority = priority;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<SeasonalPeriod, DiscountRule> seasonalRules = new EnumMap<>(SeasonalPeriod.class);
        private String description = "계절별 할인";
        private int priority = 30;

        public Builder withSpringDiscount(BigDecimal rate, String desc) {
            seasonalRules.put(SeasonalPeriod.SPRING, DiscountRule.rate(rate, desc));
            return this;
        }

        public Builder withSummerDiscount(BigDecimal rate, String desc) {
            seasonalRules.put(SeasonalPeriod.SUMMER, DiscountRule.rate(rate, desc));
            return this;
        }

        public Builder withAutumnDiscount(BigDecimal rate, String desc) {
            seasonalRules.put(SeasonalPeriod.AUTUMN, DiscountRule.rate(rate, desc));
            return this;
        }

        public Builder withWinterDiscount(BigDecimal rate, String desc) {
            seasonalRules.put(SeasonalPeriod.WINTER, DiscountRule.rate(rate, desc));
            return this;
        }

        public Builder withHolidayDiscount(BigDecimal rate, String desc) {
            seasonalRules.put(SeasonalPeriod.HOLIDAY_SEASON, DiscountRule.rate(rate, desc));
            return this;
        }

        public Builder withOffSeasonDiscount(Money amount, String desc) {
            seasonalRules.put(SeasonalPeriod.OFF_SEASON, DiscountRule.amount(amount, desc));
            return this;
        }

        public Builder withCustomPeriod(SeasonalPeriod period, DiscountRule rule) {
            seasonalRules.put(period, rule);
            return this;
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withPriority(int priority) {
            this.priority = priority;
            return this;
        }

        public SeasonalDiscountPolicy build() {
            if (seasonalRules.isEmpty()) {
                throw new IllegalStateException("최소 하나의 계절별 할인 규칙이 필요합니다");
            }
            return new SeasonalDiscountPolicy(seasonalRules, description, priority);
        }
    }

    /**
     * 미리 정의된 계절별 할인 정책들
     */
    public static SeasonalDiscountPolicy standardSeasonalDiscount() {
        return builder()
                .withSpringDiscount(new BigDecimal("0.10"), "봄맞이 10% 할인")
                .withSummerDiscount(new BigDecimal("0.15"), "여름 성수기 15% 할인")
                .withAutumnDiscount(new BigDecimal("0.05"), "가을 단풍 5% 할인")
                .withWinterDiscount(new BigDecimal("0.12"), "겨울 따뜻함 12% 할인")
                .withDescription("표준 사계절 할인")
                .build();
    }

    public static SeasonalDiscountPolicy holidaySpecial() {
        return builder()
                .withHolidayDiscount(new BigDecimal("0.20"), "연말연시 특별 20% 할인")
                .withDescription("연말연시 특가")
                .withPriority(10)
                .build();
    }

    public static SeasonalDiscountPolicy offSeasonSpecial() {
        return builder()
                .withOffSeasonDiscount(Money.won(5000), "비수기 특별 5,000원 할인")
                .withDescription("비수기 고객 유치")
                .build();
    }

    @Override
    public Money applyDiscount(Money originalPrice, DiscountContext context) {
        if (!isApplicable(context)) {
            return originalPrice;
        }

        SeasonalPeriod currentPeriod = getCurrentSeasonalPeriod(context.getPurchaseDate());
        DiscountRule rule = seasonalRules.get(currentPeriod);

        if (rule != null) {
            return rule.apply(originalPrice);
        }

        return originalPrice;
    }

    @Override
    public boolean isApplicable(DiscountContext context) {
        SeasonalPeriod currentPeriod = getCurrentSeasonalPeriod(context.getPurchaseDate());
        return seasonalRules.containsKey(currentPeriod);
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    /**
     * 현재 날짜의 계절 판단
     */
    private SeasonalPeriod getCurrentSeasonalPeriod(LocalDate date) {
        Month month = date.getMonth();
        int day = date.getDayOfMonth();

        // 특별 기간 먼저 체크
        if (isHolidaySeason(month, day)) {
            return SeasonalPeriod.HOLIDAY_SEASON;
        }

        if (isBackToSchoolSeason(month, day)) {
            return SeasonalPeriod.BACK_TO_SCHOOL;
        }

        if (isSummerVacation(month, day)) {
            return SeasonalPeriod.SUMMER_VACATION;
        }

        // 일반 계절 체크
        return switch (month) {
            case MARCH, APRIL, MAY -> SeasonalPeriod.SPRING;
            case JUNE, JULY, AUGUST -> SeasonalPeriod.SUMMER;
            case SEPTEMBER, OCTOBER, NOVEMBER -> SeasonalPeriod.AUTUMN;
            case DECEMBER, JANUARY, FEBRUARY -> SeasonalPeriod.WINTER;
        };
    }

    private boolean isHolidaySeason(Month month, int day) {
        return (month == Month.DECEMBER && day >= 20) ||
                (month == Month.JANUARY && day <= 10);
    }

    private boolean isBackToSchoolSeason(Month month, int day) {
        return (month == Month.FEBRUARY && day >= 25) ||
                (month == Month.MARCH && day <= 10);
    }

    private boolean isSummerVacation(Month month, int day) {
        return (month == Month.JULY && day >= 25) ||
                (month == Month.AUGUST);
    }

    /**
     * 현재 적용 가능한 계절별 할인 정보
     */
    public String getCurrentDiscountInfo(LocalDate date) {
        SeasonalPeriod currentPeriod = getCurrentSeasonalPeriod(date);
        DiscountRule rule = seasonalRules.get(currentPeriod);

        if (rule != null) {
            return String.format("%s 기간: %s",
                    currentPeriod.getDisplayName(),
                    rule.getDescription());
        }

        return "현재 적용 가능한 계절별 할인이 없습니다";
    }

    public Map<SeasonalPeriod, DiscountRule> getSeasonalRules() {
        return Map.copyOf(seasonalRules);
    }
}