package com.hexapass.domain.policy.specification;

import com.hexapass.domain.model.Member;
import com.hexapass.domain.model.MembershipPlan;
import com.hexapass.domain.policy.ReservationContext;
import com.hexapass.domain.policy.ReservationSpecification;
import com.hexapass.domain.type.ResourceType;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * 선예약 기간 제한 확인 사양 - 개선된 버전
 * 회원의 멤버십에서 허용하는 선예약 기간 내에 예약하는지 확인
 * 리소스 타입별로 다른 선예약 기간을 적용하고, VIP 등급별 특별 혜택 지원
 */
public class AdvanceReservationLimitSpecification implements ReservationSpecification {

    private final boolean useGlobalLimit;                           // 전체 선예약 제한 사용
    private final boolean useResourceTypeLimit;                     // 리소스 타입별 제한 사용
    private final Map<ResourceType, Integer> resourceTypeLimits;    // 리소스 타입별 선예약 일수
    private final int bonusDaysForVip;                             // VIP 멤버 추가 선예약 일수
    private final boolean allowSameDayReservation;                 // 당일 예약 허용 여부
    private final int minimumAdvanceHours;                         // 최소 선예약 시간 (시간 단위)

    public AdvanceReservationLimitSpecification() {
        this(true, false, null, 0, true, 2);
    }

    public AdvanceReservationLimitSpecification(boolean useGlobalLimit, boolean useResourceTypeLimit,
                                                Map<ResourceType, Integer> resourceTypeLimits,
                                                int bonusDaysForVip, boolean allowSameDayReservation,
                                                int minimumAdvanceHours) {
        this.useGlobalLimit = useGlobalLimit;
        this.useResourceTypeLimit = useResourceTypeLimit;
        this.resourceTypeLimits = resourceTypeLimits != null ? Map.copyOf(resourceTypeLimits) : null;
        this.bonusDaysForVip = validateBonusDays(bonusDaysForVip);
        this.allowSameDayReservation = allowSameDayReservation;
        this.minimumAdvanceHours = validateMinimumHours(minimumAdvanceHours);
    }

    /**
     * 표준 선예약 제한 (플랜 기본값 사용)
     */
    public static AdvanceReservationLimitSpecification standard() {
        return new AdvanceReservationLimitSpecification(true, false, null, 0, true, 2);
    }

    /**
     * VIP 혜택 포함 (7일 추가)
     */
    public static AdvanceReservationLimitSpecification withVipBonus() {
        return new AdvanceReservationLimitSpecification(true, false, null, 7, true, 1);
    }

    /**
     * 엄격한 선예약 제한 (당일 예약 불허, 최소 24시간 전)
     */
    public static AdvanceReservationLimitSpecification strict() {
        return new AdvanceReservationLimitSpecification(true, false, null, 0, false, 24);
    }

    /**
     * 리소스 타입별 제한 적용
     */
    public static AdvanceReservationLimitSpecification withResourceTypeLimits(
            Map<ResourceType, Integer> limits) {
        return new AdvanceReservationLimitSpecification(true, true, limits, 0, true, 2);
    }

    /**
     * 관대한 선예약 제한 (최소 1시간 전)
     */
    public static AdvanceReservationLimitSpecification lenient() {
        return new AdvanceReservationLimitSpecification(true, false, null, 0, true, 1);
    }

    /**
     * 인기 시설 전용 제한 (헬스장은 3일 전부터, 수영장은 7일 전부터)
     */
    public static AdvanceReservationLimitSpecification popularFacilities() {
        Map<ResourceType, Integer> limits = Map.of(
                ResourceType.GYM, 3,
                ResourceType.POOL, 7,
                ResourceType.SAUNA, 1
        );
        return new AdvanceReservationLimitSpecification(false, true, limits, 3, true, 4);
    }

    @Override
    public boolean isSatisfiedBy(ReservationContext context) {
        Member member = context.getMember();
        if (member == null) {
            return false;
        }

        LocalDate reservationDate = context.getReservationDate();
        LocalDate today = LocalDate.now();

        // 최소 선예약 시간 확인
        if (!checkMinimumAdvanceTime(context)) {
            return false;
        }

        // 당일 예약 허용 여부 확인
        if (!allowSameDayReservation && reservationDate.equals(today)) {
            return false;
        }

        // 전체 선예약 제한 확인
        if (useGlobalLimit && !checkGlobalAdvanceLimit(context)) {
            return false;
        }

        // 리소스 타입별 제한 확인
        if (useResourceTypeLimit && !checkResourceTypeAdvanceLimit(context)) {
            return false;
        }

        return true;
    }

    @Override
    public String getDescription() {
        StringBuilder desc = new StringBuilder("선예약 기간 제한");

        if (useGlobalLimit && useResourceTypeLimit) {
            desc.append(" (전체 및 타입별)");
        } else if (useGlobalLimit) {
            desc.append(" (플랜 기준)");
        } else if (useResourceTypeLimit) {
            desc.append(" (타입별)");
        }

        if (!allowSameDayReservation) {
            desc.append(", 당일 예약 불허");
        }

        if (minimumAdvanceHours > 0) {
            desc.append(", 최소 ").append(minimumAdvanceHours).append("시간 전");
        }

        if (bonusDaysForVip > 0) {
            desc.append(", VIP 추가 ").append(bonusDaysForVip).append("일");
        }

        return desc.toString();
    }

    /**
     * 구체적인 실패 이유 반환
     */
    public String getFailureReason(ReservationContext context) {
        Member member = context.getMember();
        LocalDate reservationDate = context.getReservationDate();
        LocalDate today = LocalDate.now();

        if (member == null) {
            return "회원 정보가 없습니다";
        }

        // 최소 선예약 시간 확인
        if (!checkMinimumAdvanceTime(context)) {
            long hoursUntil = ChronoUnit.HOURS.between(
                    java.time.LocalDateTime.now(),
                    context.getReservationTime()
            );
            return String.format("예약은 최소 %d시간 전에 해야 합니다 (현재 %d시간 전)",
                    minimumAdvanceHours, hoursUntil);
        }

        // 당일 예약 확인
        if (!allowSameDayReservation && reservationDate.equals(today)) {
            return "당일 예약은 허용되지 않습니다";
        }

        // 전체 선예약 제한 확인
        if (useGlobalLimit && !checkGlobalAdvanceLimit(context)) {
            int daysFromToday = context.getDaysFromToday();
            int maxAdvanceDays = getEffectiveAdvanceDays(member);

            return String.format("선예약 기간을 초과했습니다 (예약일: %d일 후, 최대: %d일 전까지)",
                    daysFromToday, maxAdvanceDays);
        }

        // 리소스 타입별 제한 확인
        if (useResourceTypeLimit && !checkResourceTypeAdvanceLimit(context)) {
            ResourceType resourceType = context.getResourceType();
            int typeLimit = getResourceTypeLimit(resourceType);
            int daysFromToday = context.getDaysFromToday();

            return String.format("'%s'는 최대 %d일 전까지만 예약 가능합니다 (요청: %d일 후)",
                    resourceType.getDisplayName(), typeLimit, daysFromToday);
        }

        return null; // 실패하지 않음
    }

    /**
     * 선예약 가능 기간 정보
     */
    public String getAdvanceReservationInfo(ReservationContext context) {
        Member member = context.getMember();
        if (member == null || member.getCurrentPlan() == null) {
            return "선예약 정보 없음";
        }

        StringBuilder info = new StringBuilder();

        if (useGlobalLimit) {
            int maxDays = getEffectiveAdvanceDays(member);
            info.append("최대 선예약: ").append(maxDays).append("일 전까지").append("\n");
        }

        if (useResourceTypeLimit && resourceTypeLimits != null) {
            info.append("리소스별 제한:\n");
            for (Map.Entry<ResourceType, Integer> entry : resourceTypeLimits.entrySet()) {
                info.append("- ").append(entry.getKey().getDisplayName())
                        .append(": ").append(entry.getValue()).append("일\n");
            }
        }

        if (!allowSameDayReservation) {
            info.append("당일 예약: 불가\n");
        }

        if (minimumAdvanceHours > 0) {
            info.append("최소 예약 시간: ").append(minimumAdvanceHours).append("시간 전");
        }

        return info.toString().trim();
    }

    /**
     * VIP 혜택 정보
     */
    public String getVipBenefitInfo(ReservationContext context) {
        if (bonusDaysForVip <= 0) {
            return "VIP 혜택 없음";
        }

        Member member = context.getMember();
        if (member == null) {
            return "회원 정보 없음";
        }

        boolean isVip = isVipMember(member);
        if (isVip) {
            return String.format("VIP 혜택 적용: 추가 %d일 선예약 가능", bonusDaysForVip);
        } else {
            return String.format("VIP 승급 시 추가 %d일 선예약 혜택", bonusDaysForVip);
        }
    }

    /**
     * 다음 예약 가능한 날짜 계산
     */
    public LocalDate getEarliestReservationDate(ReservationContext context) {
        LocalDate today = LocalDate.now();

        if (!allowSameDayReservation) {
            return today.plusDays(1);
        }

        if (minimumAdvanceHours > 0) {
            java.time.LocalDateTime minTime = java.time.LocalDateTime.now().plusHours(minimumAdvanceHours);
            return minTime.toLocalDate();
        }

        return today;
    }

    /**
     * 최대 예약 가능한 날짜 계산
     */
    public LocalDate getLatestReservationDate(ReservationContext context) {
        Member member = context.getMember();
        if (member == null) {
            return LocalDate.now();
        }

        LocalDate today = LocalDate.now();
        int maxDays = 0;

        if (useGlobalLimit) {
            maxDays = Math.max(maxDays, getEffectiveAdvanceDays(member));
        }

        if (useResourceTypeLimit) {
            ResourceType resourceType = context.getResourceType();
            maxDays = Math.max(maxDays, getResourceTypeLimit(resourceType));
        }

        return today.plusDays(maxDays);
    }

    // =========================
    // 헬퍼 메서드들
    // =========================

    private boolean checkMinimumAdvanceTime(ReservationContext context) {
        if (minimumAdvanceHours <= 0) {
            return true;
        }

        long hoursUntil = ChronoUnit.HOURS.between(
                java.time.LocalDateTime.now(),
                context.getReservationTime()
        );

        return hoursUntil >= minimumAdvanceHours;
    }

    private boolean checkGlobalAdvanceLimit(ReservationContext context) {
        Member member = context.getMember();
        int daysFromToday = context.getDaysFromToday();
        int maxAdvanceDays = getEffectiveAdvanceDays(member);

        return daysFromToday <= maxAdvanceDays;
    }

    private boolean checkResourceTypeAdvanceLimit(ReservationContext context) {
        if (resourceTypeLimits == null) {
            return true;
        }

        ResourceType resourceType = context.getResourceType();
        int typeLimit = getResourceTypeLimit(resourceType);
        int daysFromToday = context.getDaysFromToday();

        return daysFromToday <= typeLimit;
    }

    private int getEffectiveAdvanceDays(Member member) {
        MembershipPlan plan = member.getCurrentPlan();
        if (plan == null) {
            return 0;
        }

        int baseDays = plan.getMaxAdvanceReservationDays();

        if (isVipMember(member)) {
            return baseDays + bonusDaysForVip;
        }

        return baseDays;
    }

    private int getResourceTypeLimit(ResourceType resourceType) {
        if (resourceTypeLimits == null) {
            return Integer.MAX_VALUE;
        }

        return resourceTypeLimits.getOrDefault(resourceType, Integer.MAX_VALUE);
    }

    private boolean isVipMember(Member member) {
        // 간단한 VIP 판정 로직 (실제로는 더 복잡할 수 있음)
        MembershipPlan plan = member.getCurrentPlan();
        if (plan == null) {
            return false;
        }

        return plan.getName().toUpperCase().contains("VIP") ||
                plan.getName().toUpperCase().contains("PREMIUM");
    }

    private int validateBonusDays(int days) {
        if (days < 0) {
            throw new IllegalArgumentException("보너스 일수는 0 이상이어야 합니다: " + days);
        }
        return days;
    }

    private int validateMinimumHours(int hours) {
        if (hours < 0) {
            throw new IllegalArgumentException("최소 선예약 시간은 0 이상이어야 합니다: " + hours);
        }
        return hours;
    }

    // =========================
    // Getter 메서드들
    // =========================

    public boolean isUseGlobalLimit() {
        return useGlobalLimit;
    }

    public boolean isUseResourceTypeLimit() {
        return useResourceTypeLimit;
    }

    public Map<ResourceType, Integer> getResourceTypeLimits() {
        return resourceTypeLimits != null ? Map.copyOf(resourceTypeLimits) : null;
    }

    public int getBonusDaysForVip() {
        return bonusDaysForVip;
    }

    public boolean isAllowSameDayReservation() {
        return allowSameDayReservation;
    }

    public int getMinimumAdvanceHours() {
        return minimumAdvanceHours;
    }
}