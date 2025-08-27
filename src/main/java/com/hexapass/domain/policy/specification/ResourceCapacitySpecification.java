package com.hexapass.domain.policy.specification;

import com.hexapass.domain.policy.ReservationContext;
import com.hexapass.domain.policy.ReservationSpecification;

/**
 * 리소스 수용 인원 확인 사양 - 개선된 버전
 * 리소스에 여유 공간이 있는지 확인하고 상세한 수용률 정보 제공
 * 버퍼 공간과 최소 여유 인원 설정 지원
 */
public class ResourceCapacitySpecification implements ReservationSpecification {

    private final int minimumAvailableCapacity; // 최소 여유 인원
    private final double maxOccupancyRate;      // 최대 수용률 (0.0 ~ 1.0)
    private final boolean allowFullCapacity;    // 100% 수용률 허용 여부

    public ResourceCapacitySpecification() {
        this(1, 1.0, false); // 기본: 최소 1명 여유, 100% 허용하지 않음
    }

    public ResourceCapacitySpecification(int minimumAvailableCapacity,
                                         double maxOccupancyRate,
                                         boolean allowFullCapacity) {
        this.minimumAvailableCapacity = validateMinimumCapacity(minimumAvailableCapacity);
        this.maxOccupancyRate = validateOccupancyRate(maxOccupancyRate);
        this.allowFullCapacity = allowFullCapacity;
    }

    /**
     * 표준 수용률 제한 (90% 최대, 최소 1명 여유)
     */
    public static ResourceCapacitySpecification standard() {
        return new ResourceCapacitySpecification(1, 0.9, false);
    }

    /**
     * 엄격한 수용률 제한 (80% 최대, 최소 2명 여유)
     */
    public static ResourceCapacitySpecification strict() {
        return new ResourceCapacitySpecification(2, 0.8, false);
    }

    /**
     * 관대한 수용률 제한 (100% 허용)
     */
    public static ResourceCapacitySpecification lenient() {
        return new ResourceCapacitySpecification(0, 1.0, true);
    }

    /**
     * 커스텀 여유 인원 기준
     */
    public static ResourceCapacitySpecification withMinimumAvailable(int minimumAvailable) {
        return new ResourceCapacitySpecification(minimumAvailable, 1.0, false);
    }

    /**
     * 커스텀 수용률 기준
     */
    public static ResourceCapacitySpecification withMaxOccupancyRate(double maxRate) {
        return new ResourceCapacitySpecification(0, maxRate, false);
    }

    @Override
    public boolean isSatisfiedBy(ReservationContext context) {
        int currentOccupancy = context.getResourceCurrentOccupancy();
        int capacity = context.getResourceCapacity();

        // 수용률 계산
        double occupancyRate = capacity > 0 ? (double) currentOccupancy / capacity : 1.0;

        // 100% 수용률 허용 여부 확인
        if (!allowFullCapacity && currentOccupancy >= capacity) {
            return false;
        }

        // 최대 수용률 확인
        if (occupancyRate > maxOccupancyRate) {
            return false;
        }

        // 최소 여유 인원 확인
        int availableCapacity = capacity - currentOccupancy;
        if (availableCapacity < minimumAvailableCapacity) {
            return false;
        }

        return true;
    }

    @Override
    public String getDescription() {
        StringBuilder desc = new StringBuilder("리소스 수용 인원 여유");

        if (minimumAvailableCapacity > 0) {
            desc.append(" (최소 ").append(minimumAvailableCapacity).append("명 여유)");
        }

        if (maxOccupancyRate < 1.0) {
            desc.append(" (수용률 ").append((int)(maxOccupancyRate * 100)).append("% 이하)");
        }

        if (!allowFullCapacity) {
            desc.append(" (만석 불허)");
        }

        return desc.toString();
    }

    /**
     * 구체적인 실패 이유 반환
     */
    public String getFailureReason(ReservationContext context) {
        int currentOccupancy = context.getResourceCurrentOccupancy();
        int capacity = context.getResourceCapacity();
        double occupancyRate = capacity > 0 ? (double) currentOccupancy / capacity : 1.0;
        int availableCapacity = capacity - currentOccupancy;

        if (!allowFullCapacity && currentOccupancy >= capacity) {
            return "리소스가 만석입니다";
        }

        if (occupancyRate > maxOccupancyRate) {
            return String.format("수용률이 한도를 초과했습니다 (현재: %.1f%%, 최대: %.1f%%)",
                    occupancyRate * 100, maxOccupancyRate * 100);
        }

        if (availableCapacity < minimumAvailableCapacity) {
            return String.format("여유 인원이 부족합니다 (현재 여유: %d명, 최소 필요: %d명)",
                    availableCapacity, minimumAvailableCapacity);
        }

        return null; // 실패하지 않음
    }

    /**
     * 현재 수용률 계산
     */
    public double getCurrentOccupancyRate(ReservationContext context) {
        int capacity = context.getResourceCapacity();
        if (capacity <= 0) {
            return 1.0; // 수용 인원이 0이면 100%로 간주
        }

        return (double) context.getResourceCurrentOccupancy() / capacity;
    }

    /**
     * 남은 수용 인원 계산
     */
    public int getRemainingCapacity(ReservationContext context) {
        return Math.max(0, context.getResourceCapacity() - context.getResourceCurrentOccupancy());
    }

    /**
     * 추가 수용 가능 인원 (정책 제한 고려)
     */
    public int getAdditionalCapacity(ReservationContext context) {
        int totalCapacity = context.getResourceCapacity();
        int currentOccupancy = context.getResourceCurrentOccupancy();

        // 최대 수용률 제한 적용
        int maxAllowedOccupancy = (int) Math.floor(totalCapacity * maxOccupancyRate);

        // 100% 수용률 불허 시 -1
        if (!allowFullCapacity && maxAllowedOccupancy >= totalCapacity) {
            maxAllowedOccupancy = totalCapacity - 1;
        }

        // 최소 여유 인원 확보
        maxAllowedOccupancy = Math.min(maxAllowedOccupancy, totalCapacity - minimumAvailableCapacity);

        return Math.max(0, maxAllowedOccupancy - currentOccupancy);
    }

    /**
     * 수용률이 경고 수준인지 확인
     */
    public boolean isCapacityWarning(ReservationContext context, double warningThreshold) {
        double currentRate = getCurrentOccupancyRate(context);
        return currentRate >= warningThreshold;
    }

    /**
     * 리소스 수용 현황 요약
     */
    public String getCapacitySummary(ReservationContext context) {
        int current = context.getResourceCurrentOccupancy();
        int total = context.getResourceCapacity();
        double rate = getCurrentOccupancyRate(context);
        int remaining = getRemainingCapacity(context);
        int additional = getAdditionalCapacity(context);

        return String.format("수용현황: %d/%d명 (%.1f%%) | 남은자리: %d명 | 추가가능: %d명",
                current, total, rate * 100, remaining, additional);
    }

    // =========================
    // Getter 메서드들
    // =========================

    public int getMinimumAvailableCapacity() {
        return minimumAvailableCapacity;
    }

    public double getMaxOccupancyRate() {
        return maxOccupancyRate;
    }

    public boolean isAllowFullCapacity() {
        return allowFullCapacity;
    }

    // =========================
    // 검증 메서드들
    // =========================

    private int validateMinimumCapacity(int minimumCapacity) {
        if (minimumCapacity < 0) {
            throw new IllegalArgumentException("최소 여유 인원은 0 이상이어야 합니다: " + minimumCapacity);
        }
        return minimumCapacity;
    }

    private double validateOccupancyRate(double rate) {
        if (rate < 0.0 || rate > 1.0) {
            throw new IllegalArgumentException("최대 수용률은 0.0-1.0 사이여야 합니다: " + rate);
        }
        return rate;
    }

    // =========================
    // Object 메서드 오버라이드
    // =========================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ResourceCapacitySpecification that = (ResourceCapacitySpecification) obj;
        return minimumAvailableCapacity == that.minimumAvailableCapacity &&
                Double.compare(that.maxOccupancyRate, maxOccupancyRate) == 0 &&
                allowFullCapacity == that.allowFullCapacity;
    }

    @Override
    public int hashCode() {
        int result = Integer.hashCode(minimumAvailableCapacity);
        result = 31 * result + Double.hashCode(maxOccupancyRate);
        result = 31 * result + Boolean.hashCode(allowFullCapacity);
        return result;
    }

    @Override
    public String toString() {
        return "ResourceCapacitySpecification{" +
                "minimumAvailableCapacity=" + minimumAvailableCapacity +
                ", maxOccupancyRate=" + maxOccupancyRate +
                ", allowFullCapacity=" + allowFullCapacity +
                '}';
    }
}