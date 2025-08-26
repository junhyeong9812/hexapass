package com.hexapass.domain.policy.cancellation;

import com.hexapass.domain.common.Money;
import com.hexapass.domain.policy.CancellationContext;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * 취소 수수료 계산 유틸리티
 * 시간대별, 조건별 수수료 계산을 담당
 */
public class CancellationFeeCalculator {

    /**
     * 시간대별 수수료 규칙
     */
    public static class FeeRule {
        private final Duration minimumHoursBefore;
        private final Duration maximumHoursBefore;
        private final BigDecimal feeRate;
        private final Money fixedFee;
        private final Money maximumFee;
        private final String description;

        public FeeRule(Duration minimumHoursBefore, Duration maximumHoursBefore,
                       BigDecimal feeRate, Money fixedFee, Money maximumFee, String description) {
            this.minimumHoursBefore = Objects.requireNonNull(minimumHoursBefore);
            this.maximumHoursBefore = maximumHoursBefore;
            this.feeRate = feeRate != null ? feeRate : BigDecimal.ZERO;
            this.fixedFee = fixedFee != null ? fixedFee : Money.zeroWon();
            this.maximumFee = maximumFee;
            this.description = Objects.requireNonNull(description);
        }

        public static FeeRule noFee(Duration minimumHoursBefore, Duration maximumHoursBefore, String description) {
            return new FeeRule(minimumHoursBefore, maximumHoursBefore,
                    BigDecimal.ZERO, Money.zeroWon(), null, description);
        }

        public static FeeRule rateOnly(Duration minimumHoursBefore, Duration maximumHoursBefore,
                                       BigDecimal feeRate, String description) {
            return new FeeRule(minimumHoursBefore, maximumHoursBefore,
                    feeRate, Money.zeroWon(), null, description);
        }

        public static FeeRule fixedOnly(Duration minimumHoursBefore, Duration maximumHoursBefore,
                                        Money fixedFee, String description) {
            return new FeeRule(minimumHoursBefore, maximumHoursBefore,
                    BigDecimal.ZERO, fixedFee, null, description);
        }

        public static FeeRule combined(Duration minimumHoursBefore, Duration maximumHoursBefore,
                                       BigDecimal feeRate, Money fixedFee, Money maximumFee, String description) {
            return new FeeRule(minimumHoursBefore, maximumHoursBefore,
                    feeRate, fixedFee, maximumFee, description);
        }

        public boolean applies(Duration hoursBeforeReservation) {
            boolean afterMinimum = hoursBeforeReservation.compareTo(minimumHoursBefore) >= 0;
            boolean beforeMaximum = maximumHoursBefore == null ||
                    hoursBeforeReservation.compareTo(maximumHoursBefore) < 0;
            return afterMinimum && beforeMaximum;
        }

        public Money calculateFee(Money originalPrice) {
            Money rateFee = originalPrice.multiply(feeRate);
            Money totalFee = rateFee.add(fixedFee);

            if (maximumFee != null && totalFee.compareTo(maximumFee) > 0) {
                return maximumFee;
            }

            return totalFee;
        }

        // Getters
        public Duration getMinimumHoursBefore() { return minimumHoursBefore; }
        public Duration getMaximumHoursBefore() { return maximumHoursBefore; }
        public BigDecimal getFeeRate() { return feeRate; }
        public Money getFixedFee() { return fixedFee; }
        public Money getMaximumFee() { return maximumFee; }
        public String getDescription() { return description; }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(description);
            sb.append(" (");

            if (feeRate.compareTo(BigDecimal.ZERO) > 0) {
                sb.append("정률 ").append(feeRate.multiply(BigDecimal.valueOf(100))).append("%");
            }

            if (fixedFee.compareTo(Money.zeroWon()) > 0) {
                if (feeRate.compareTo(BigDecimal.ZERO) > 0) sb.append(" + ");
                sb.append("정액 ").append(fixedFee);
            }

            if (maximumFee != null) {
                sb.append(", 최대 ").append(maximumFee);
            }

            if (feeRate.equals(BigDecimal.ZERO) && fixedFee.isZero()) {
                sb.append("수수료 없음");
            }

            sb.append(")");
            return sb.toString();
        }
    }

    private final List<FeeRule> feeRules;
    private final String policyDescription;

    public CancellationFeeCalculator(List<FeeRule> feeRules, String policyDescription) {
        this.feeRules = Objects.requireNonNull(feeRules);
        this.policyDescription = Objects.requireNonNull(policyDescription);

        if (feeRules.isEmpty()) {
            throw new IllegalArgumentException("최소 하나의 수수료 규칙이 필요합니다");
        }
    }

    /**
     * 취소 시점에 따른 수수료 계산
     */
    public Money calculateFee(Money originalPrice, CancellationContext context) {
        Duration hoursBeforeReservation = getHoursBeforeReservation(context);

        return feeRules.stream()
                .filter(rule -> rule.applies(hoursBeforeReservation))
                .findFirst()
                .map(rule -> rule.calculateFee(originalPrice))
                .orElse(originalPrice); // 규칙이 없으면 전액 수수료
    }

    /**
     * 적용되는 수수료 규칙 찾기
     */
    public FeeRule getApplicableRule(CancellationContext context) {
        Duration hoursBeforeReservation = getHoursBeforeReservation(context);

        return feeRules.stream()
                .filter(rule -> rule.applies(hoursBeforeReservation))
                .findFirst()
                .orElse(null);
    }

    /**
     * 수수료 계산 결과와 설명을 함께 제공
     */
    public CancellationResult calculateFeeWithDetails(Money originalPrice, CancellationContext context) {
        FeeRule applicableRule = getApplicableRule(context);
        Duration hoursBeforeReservation = getHoursBeforeReservation(context);

        if (applicableRule == null) {
            return new CancellationResult(
                    originalPrice,
                    Money.zeroWon(),
                    "해당하는 취소 규칙이 없어 전액 수수료 적용",
                    hoursBeforeReservation
            );
        }

        Money fee = applicableRule.calculateFee(originalPrice);
        Money refund = originalPrice.subtract(fee);

        return new CancellationResult(
                fee,
                refund,
                applicableRule.toString(),
                hoursBeforeReservation
        );
    }

    /**
     * 기존 CancellationContext에서 Duration 계산
     * getHoursUntilReservation() 메서드 활용
     */
    private Duration getHoursBeforeReservation(CancellationContext context) {
        // CancellationContext의 getTimeBetweenCancellationAndReservation() 사용
        return context.getTimeBetweenCancellationAndReservation();
    }

    public String getPolicyDescription() {
        return policyDescription;
    }

    public List<FeeRule> getFeeRules() {
        return List.copyOf(feeRules);
    }

    /**
     * 취소 수수료 계산 결과
     */
    public static class CancellationResult {
        private final Money fee;
        private final Money refundAmount;
        private final String ruleDescription;
        private final Duration hoursBeforeReservation;

        public CancellationResult(Money fee, Money refundAmount, String ruleDescription, Duration hoursBeforeReservation) {
            this.fee = Objects.requireNonNull(fee);
            this.refundAmount = Objects.requireNonNull(refundAmount);
            this.ruleDescription = Objects.requireNonNull(ruleDescription);
            this.hoursBeforeReservation = Objects.requireNonNull(hoursBeforeReservation);
        }

        public Money getFee() { return fee; }
        public Money getRefundAmount() { return refundAmount; }
        public String getRuleDescription() { return ruleDescription; }
        public Duration getHoursBeforeReservation() { return hoursBeforeReservation; }

        public boolean hasRefund() {
            return refundAmount.compareTo(Money.zeroWon()) > 0;
        }

        @Override
        public String toString() {
            long hours = hoursBeforeReservation.toHours();
            return String.format("취소 시점: 예약 %d시간 전 | 수수료: %s | 환불금: %s | 적용규칙: %s",
                    hours, fee, refundAmount, ruleDescription);
        }
    }
}