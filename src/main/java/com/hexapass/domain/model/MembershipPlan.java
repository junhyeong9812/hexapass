package com.hexapass.domain.model;

import com.hexapass.domain.common.Money;
import com.hexapass.domain.type.PlanType;
import com.hexapass.domain.type.ResourceType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 멤버십 플랜을 나타내는 엔티티
 * 회원이 선택할 수 있는 구독 상품으로, 이용 권한과 혜택을 정의
 * planId를 기준으로 동일성 판단
 */
public class MembershipPlan {

    private final String planId;
    private final String name;
    private final PlanType type;
    private final Money price;
    private final int durationDays;
    private final Set<ResourceType> allowedResourceTypes;
    private final int maxSimultaneousReservations;
    private final int maxAdvanceReservationDays;
    private final BigDecimal discountRate; // 0.0 ~ 1.0
    private final LocalDateTime createdAt;
    private boolean isActive;

    /**
     * private 생성자 - 팩토리 메서드를 통해서만 생성 가능
     */
    private MembershipPlan(String planId, String name, PlanType type, Money price,
                           int durationDays, Set<ResourceType> allowedResourceTypes,
                           int maxSimultaneousReservations, int maxAdvanceReservationDays,
                           BigDecimal discountRate) {
        this.planId = validateNotBlank(planId, "플랜 ID");
        this.name = validateNotBlank(name, "플랜명");
        this.type = validateNotNull(type, "플랜 타입");
        this.price = validateNotNull(price, "가격");
        this.durationDays = validatePositive(durationDays, "이용 기간");
        this.allowedResourceTypes = Set.copyOf(validateNotEmpty(allowedResourceTypes, "이용 가능 리소스"));
        this.maxSimultaneousReservations = validatePositive(maxSimultaneousReservations, "최대 동시 예약 수");
        this.maxAdvanceReservationDays = validateNonNegative(maxAdvanceReservationDays, "최대 선예약 일수");
        this.discountRate = validateDiscountRate(discountRate);
        this.createdAt = LocalDateTime.now();
        this.isActive = true;

        validatePlanConsistency();
    }

    /**
     * 기본 멤버십 플랜 생성
     */
    public static MembershipPlan create(String planId, String name, PlanType type, Money price,
                                        int durationDays, Set<ResourceType> allowedResourceTypes) {
        return new MembershipPlan(
                planId, name, type, price, durationDays, allowedResourceTypes,
                3, // 기본 최대 동시 예약 수
                30, // 기본 최대 선예약 일수
                BigDecimal.ZERO // 기본 할인율 0%
        );
    }

    /**
     * 상세 옵션이 포함된 멤버십 플랜 생성
     */
    public static MembershipPlan createWithOptions(String planId, String name, PlanType type, Money price,
                                                   int durationDays, Set<ResourceType> allowedResourceTypes,
                                                   int maxSimultaneousReservations, int maxAdvanceReservationDays,
                                                   BigDecimal discountRate) {
        return new MembershipPlan(
                planId, name, type, price, durationDays, allowedResourceTypes,
                maxSimultaneousReservations, maxAdvanceReservationDays, discountRate
        );
    }

    /**
     * 기본 플랜들을 생성하는 팩토리 메서드들
     */
    public static MembershipPlan basicMonthly() {
        return create(
                "BASIC_MONTHLY",
                "기본 월간권",
                PlanType.MONTHLY,
                Money.won(50000),
                30,
                Set.of(ResourceType.GYM, ResourceType.STUDY_ROOM)
        );
    }

    public static MembershipPlan premiumMonthly() {
        return createWithOptions(
                "PREMIUM_MONTHLY",
                "프리미엄 월간권",
                PlanType.MONTHLY,
                Money.won(100000),
                30,
                Set.of(ResourceType.GYM, ResourceType.POOL, ResourceType.SAUNA,
                        ResourceType.STUDY_ROOM, ResourceType.MEETING_ROOM),
                5, // 동시 예약 5개
                45, // 45일 선예약
                new BigDecimal("0.1") // 10% 할인
        );
    }

    public static MembershipPlan vipYearly() {
        return createWithOptions(
                "VIP_YEARLY",
                "VIP 연간권",
                PlanType.YEARLY,
                Money.won(1000000),
                365,
                Arrays.stream(ResourceType.values()).collect(HashSet::new, Set::add, Set::addAll), // 모든 리소스
                10, // 동시 예약 10개
                90, // 90일 선예약
                new BigDecimal("0.2") // 20% 할인
        );
    }

    // =========================
    // 비즈니스 로직 메서드들
    // =========================

    /**
     * 특정 리소스 타입 이용 권한 확인
     */
    public boolean hasPrivilege(ResourceType resourceType) {
        return isActive && allowedResourceTypes.contains(resourceType);
    }

    /**
     * 여러 리소스 타입에 대한 권한 확인
     */
    public boolean hasPrivileges(Set<ResourceType> resourceTypes) {
        return isActive && allowedResourceTypes.containsAll(resourceTypes);
    }

    /**
     * 지정된 예약 수가 허용 범위 내인지 확인
     */
    public boolean canReserve(int currentReservationCount) {
        return isActive && currentReservationCount < maxSimultaneousReservations;
    }

    /**
     * 지정된 예약 날짜가 선예약 허용 범위 내인지 확인
     */
    public boolean canReserveInAdvance(int daysFromToday) {
        return isActive && daysFromToday <= maxAdvanceReservationDays;
    }

    /**
     * 일할 계산된 가격 반환 (남은 일수 기준)
     */
    public Money calculateProRatedPrice(int remainingDays) {
        if (remainingDays <= 0) {
            return Money.zero(price.getCurrency());
        }

        if (remainingDays >= durationDays) {
            return price;
        }

        BigDecimal ratio = BigDecimal.valueOf(remainingDays)
                .divide(BigDecimal.valueOf(durationDays), 4, RoundingMode.HALF_UP);
        return price.multiply(ratio);
    }

    /**
     * 할인이 적용된 가격 계산
     */
    public Money getDiscountedPrice() {
        if (discountRate.equals(BigDecimal.ZERO)) {
            return price;
        }

        BigDecimal multiplier = BigDecimal.ONE.subtract(discountRate);
        return price.multiply(multiplier);
    }

    /**
     * 할인 금액 계산
     */
    public Money getDiscountAmount() {
        return price.subtract(getDiscountedPrice());
    }

    /**
     * 업그레이드 비용 계산 (다른 플랜과의 차액)
     */
    public Money calculateUpgradeCost(MembershipPlan targetPlan, int remainingDays) {
        if (targetPlan == null) {
            throw new IllegalArgumentException("대상 플랜은 null일 수 없습니다");
        }

        Money currentRefund = this.calculateProRatedPrice(remainingDays);
        Money targetCost = targetPlan.calculateProRatedPrice(targetPlan.durationDays);

        return targetCost.subtract(currentRefund);
    }

    /**
     * 플랜 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 플랜 활성화
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * 플랜 등급 비교 (가격 기준)
     */
    public int compareTo(MembershipPlan other) {
        if (other == null) {
            return 1;
        }
        return this.price.compareTo(other.price);
    }

    /**
     * 상위 플랜인지 확인
     */
    public boolean isHigherTierThan(MembershipPlan other) {
        return compareTo(other) > 0;
    }

    /**
     * 플랜 정보 요약
     */
    public String getSummary() {
        return String.format("%s (%s) - %s, %d일, 리소스 %d개, 동시예약 %d개",
                name, type.getDisplayName(), getDiscountedPrice(),
                durationDays, allowedResourceTypes.size(), maxSimultaneousReservations);
    }

    // =========================
    // Object 메서드 오버라이드
    // =========================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        MembershipPlan that = (MembershipPlan) obj;
        return Objects.equals(planId, that.planId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(planId);
    }

    @Override
    public String toString() {
        return String.format("MembershipPlan{id='%s', name='%s', type=%s, price=%s, active=%s}",
                planId, name, type, price, isActive);
    }

    // =========================
    // Getter 메서드들
    // =========================

    public String getPlanId() {
        return planId;
    }

    public String getName() {
        return name;
    }

    public PlanType getType() {
        return type;
    }

    public Money getPrice() {
        return price;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public Set<ResourceType> getAllowedResourceTypes() {
        return Set.copyOf(allowedResourceTypes); // 불변 복사본 반환
    }

    public int getMaxSimultaneousReservations() {
        return maxSimultaneousReservations;
    }

    public int getMaxAdvanceReservationDays() {
        return maxAdvanceReservationDays;
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return isActive;
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

    private <T> T validateNotNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + "은 null일 수 없습니다");
        }
        return value;
    }

    private int validatePositive(int value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + "은 0보다 커야 합니다. 입력값: " + value);
        }
        return value;
    }

    private int validateNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + "은 0 이상이어야 합니다. 입력값: " + value);
        }
        return value;
    }

    private <T> Set<T> validateNotEmpty(Set<T> set, String fieldName) {
        if (set == null || set.isEmpty()) {
            throw new IllegalArgumentException(fieldName + "은 null이거나 빈 집합일 수 없습니다");
        }
        return set;
    }

    private BigDecimal validateDiscountRate(BigDecimal rate) {
        if (rate == null) {
            throw new IllegalArgumentException("할인율은 null일 수 없습니다");
        }
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("할인율은 0.0 이상 1.0 이하여야 합니다. 입력값: " + rate);
        }
        return rate;
    }

    private void validatePlanConsistency() {
        // 플랜 타입과 기간 일치 확인
        if (!type.isValidDuration(durationDays)) {
            throw new IllegalArgumentException(
                    String.format("플랜 타입 %s에 적합하지 않은 기간입니다. (기간: %d일)", type, durationDays));
        }

        // 선예약 일수가 플랜 기간보다 과도하게 긴지 확인
        if (maxAdvanceReservationDays > durationDays * 3) {
            throw new IllegalArgumentException("선예약 일수가 플랜 기간의 3배를 초과할 수 없습니다");
        }
    }
}