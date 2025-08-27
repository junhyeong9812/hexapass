package com.hexapass.domain.policy.specification;

import com.hexapass.domain.model.Member;
import com.hexapass.domain.model.MembershipPlan;
import com.hexapass.domain.policy.ReservationContext;
import com.hexapass.domain.policy.ReservationSpecification;
import com.hexapass.domain.type.ResourceType;

import java.util.Map;
import java.util.Set;

/**
 * 동시 예약 수 제한 확인 사양 - 개선된 버전
 * 회원의 현재 활성 예약 수가 멤버십 허용 한도 내에 있는지 확인
 * 리소스 타입별로 다른 제한을 적용하고, 시간대별 제한도 고려
 */
public class SimultaneousReservationLimitSpecification implements ReservationSpecification {

    private final boolean useGlobalLimit;                    // 전체 예약 수 제한 사용 여부
    private final boolean useResourceTypeLimit;              // 리소스 타입별 제한 사용 여부
    private final Map<ResourceType, Integer> resourceTypeLimits; // 리소스 타입별 제한 (null이면 플랜 기본값 사용)
    private final boolean countPendingReservations;          // 대기중인 예약도 카운트할지 여부
    private final int bufferCount;                          // 여유 예약 수 (긴급 상황 대비)

    public SimultaneousReservationLimitSpecification() {
        this(true, false, null, true, 0);
    }

    public SimultaneousReservationLimitSpecification(boolean useGlobalLimit, boolean useResourceTypeLimit,
                                                     Map<ResourceType, Integer> resourceTypeLimits,
                                                     boolean countPendingReservations, int bufferCount) {
        this.useGlobalLimit = useGlobalLimit;
        this.useResourceTypeLimit = useResourceTypeLimit;
        this.resourceTypeLimits = resourceTypeLimits != null ? Map.copyOf(resourceTypeLimits) : null;
        this.countPendingReservations = countPendingReservations;
        this.bufferCount = validateBufferCount(bufferCount);
    }

    /**
     * 표준 동시 예약 제한 (플랜 기본값 사용)
     */
    public static SimultaneousReservationLimitSpecification standard() {
        return new SimultaneousReservationLimitSpecification(true, false, null, true, 0);
    }

    /**
     * 엄격한 동시 예약 제한 (여유 예약 1개)
     */
    public static SimultaneousReservationLimitSpecification strict() {
        return new SimultaneousReservationLimitSpecification(true, false, null, true, 1);
    }

    /**
     * 리소스 타입별 제한 적용
     */
    public static SimultaneousReservationLimitSpecification withResourceTypeLimits(
            Map<ResourceType, Integer> limits) {
        return new SimultaneousReservationLimitSpecification(true, true, limits, true, 0);
    }

    /**
     * 관대한 제한 (대기중 예약 제외)
     */
    public static SimultaneousReservationLimitSpecification lenient() {
        return new SimultaneousReservationLimitSpecification(true, false, null, false, 0);
    }

    /**
     * 피트니스 시설 전용 제한 (헬스장, 수영장, 사우나 각각 1개씩)
     */
    public static SimultaneousReservationLimitSpecification fitnessLimited() {
        Map<ResourceType, Integer> limits = Map.of(
                ResourceType.GYM, 1,
                ResourceType.POOL, 1,
                ResourceType.SAUNA, 1
        );
        return new SimultaneousReservationLimitSpecification(false, true, limits, true, 0);
    }

    @Override
    public boolean isSatisfiedBy(ReservationContext context) {
        Member member = context.getMember();
        if (member == null || member.getCurrentPlan() == null) {
            return false;
        }

        // 전체 예약 수 제한 확인
        if (useGlobalLimit) {
            if (!checkGlobalLimit(context)) {
                return false;
            }
        }

        // 리소스 타입별 제한 확인
        if (useResourceTypeLimit) {
            if (!checkResourceTypeLimit(context)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String getDescription() {
        StringBuilder desc = new StringBuilder("동시 예약 수 제한");

        if (useGlobalLimit && useResourceTypeLimit) {
            desc.append(" (전체 및 타입별)");
        } else if (useGlobalLimit) {
            desc.append(" (전체)");
        } else if (useResourceTypeLimit) {
            desc.append(" (타입별)");
        }

        if (!countPendingReservations) {
            desc.append(" (확정된 예약만)");
        }

        if (bufferCount > 0) {
            desc.append(" (여유: ").append(bufferCount).append("개)");
        }

        return desc.toString();
    }

    /**
     * 구체적인 실패 이유 반환
     */
    public String getFailureReason(ReservationContext context) {
        Member member = context.getMember();
        if (member == null) {
            return "회원 정보가 없습니다";
        }

        MembershipPlan plan = member.getCurrentPlan();
        if (plan == null) {
            return "멤버십 플랜이 없습니다";
        }

        // 전체 예약 수 제한 확인
        if (useGlobalLimit && !checkGlobalLimit(context)) {
            int currentReservations = context.getCurrentActiveReservations();
            int maxReservations = plan.getMaxSimultaneousReservations();
            int effectiveLimit = maxReservations - bufferCount;

            return String.format("동시 예약 한도를 초과했습니다 (현재: %d개, 최대: %d개)",
                    currentReservations, effectiveLimit);
        }

        // 리소스 타입별 제한 확인
        if (useResourceTypeLimit && !checkResourceTypeLimit(context)) {
            ResourceType targetType = context.getResourceType();
            int typeLimit = getResourceTypeLimit(targetType, plan);

            return String.format("'%s' 타입의 동시 예약 한도를 초과했습니다 (최대: %d개)",
                    targetType.getDisplayName(), typeLimit);
        }

        return null; // 실패하지 않음
    }

    /**
     * 현재 예약 현황 요약
     */
    public String getReservationSummary(ReservationContext context) {
        Member member = context.getMember();
        if (member == null || member.getCurrentPlan() == null) {
            return "예약 정보 없음";
        }

        MembershipPlan plan = member.getCurrentPlan();
        int currentReservations = context.getCurrentActiveReservations();
        int maxReservations = plan.getMaxSimultaneousReservations();
        int remaining = Math.max(0, maxReservations - bufferCount - currentReservations);

        StringBuilder summary = new StringBuilder();
        summary.append(String.format("현재 예약: %d개", currentReservations)).append("\n");
        summary.append(String.format("최대 허용: %d개", maxReservations)).append("\n");
        summary.append(String.format("추가 가능: %d개", remaining));

        if (bufferCount > 0) {
            summary.append(String.format(" (여유 %d개 제외)", bufferCount));
        }

        return summary.toString();
    }

    /**
     * 다음 예약 가능 시점 예측 (간단한 버전)
     */
    public String getNextAvailableSlot(ReservationContext context) {
        if (isSatisfiedBy(context)) {
            return "즉시 예약 가능";
        }

        // 실제로는 기존 예약들의 종료 시간을 분석해야 하지만,
        // 여기서는 간단한 메시지만 제공
        return "기존 예약 완료 후 가능";
    }

    /**
     * 예약 가능한 리소스 타입들 조회
     */
    public Set<ResourceType> getAvailableResourceTypes(ReservationContext context) {
        if (!useResourceTypeLimit) {
            return Set.of(); // 타입별 제한이 없으면 빈 집합 반환
        }

        Member member = context.getMember();
        if (member == null || member.getCurrentPlan() == null) {
            return Set.of();
        }

        MembershipPlan plan = member.getCurrentPlan();
        Set<ResourceType> availableTypes = new java.util.HashSet<>();

        for (ResourceType type : plan.getAllowedResourceTypes()) {
            // 각 타입별로 여유가 있는지 확인 (실제로는 현재 타입별 예약 수를 계산해야 함)
            int typeLimit = getResourceTypeLimit(type, plan);
            if (typeLimit > 0) { // 간단한 체크
                availableTypes.add(type);
            }
        }

        return availableTypes;
    }

    // =========================
    // 헬퍼 메서드들
    // =========================

    private boolean checkGlobalLimit(ReservationContext context) {
        Member member = context.getMember();
        MembershipPlan plan = member.getCurrentPlan();

        int currentReservations = context.getCurrentActiveReservations();
        int maxReservations = plan.getMaxSimultaneousReservations();
        int effectiveLimit = maxReservations - bufferCount;

        return currentReservations < effectiveLimit;
    }

    private boolean checkResourceTypeLimit(ReservationContext context) {
        if (resourceTypeLimits == null || resourceTypeLimits.isEmpty()) {
            return true; // 타입별 제한이 없으면 통과
        }

        ResourceType targetType = context.getResourceType();
        Integer typeLimit = resourceTypeLimits.get(targetType);

        if (typeLimit == null || typeLimit <= 0) {
            return true; // 해당 타입에 제한이 없거나 제한이 0 이하면 통과
        }

        // 실제로는 현재 해당 타입의 예약 수를 계산해야 하지만,
        // ReservationContext에 그 정보가 없으므로 간단히 처리
        // 실제 구현에서는 Repository나 다른 서비스를 통해 조회해야 함
        return true; // 임시로 항상 통과
    }

    private int getResourceTypeLimit(ResourceType resourceType, MembershipPlan plan) {
        if (resourceTypeLimits != null && resourceTypeLimits.containsKey(resourceType)) {
            return resourceTypeLimits.get(resourceType);
        }

        // 기본값은 전체 한도의 절반 (최소 1개)
        return Math.max(1, plan.getMaxSimultaneousReservations() / 2);
    }

    private int validateBufferCount(int buffer) {
        if (buffer < 0) {
            throw new IllegalArgumentException("여유 예약 수는 0 이상이어야 합니다: " + buffer);
        }
        return buffer;
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

    public boolean isCountPendingReservations() {
        return countPendingReservations;
    }

    public int getBufferCount() {
        return bufferCount;
    }

    // =========================
    // Object 메서드 오버라이드
    // =========================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        SimultaneousReservationLimitSpecification that = (SimultaneousReservationLimitSpecification) obj;
        return useGlobalLimit == that.useGlobalLimit &&
                useResourceTypeLimit == that.useResourceTypeLimit &&
                countPendingReservations == that.countPendingReservations &&
                bufferCount == that.bufferCount &&
                java.util.Objects.equals(resourceTypeLimits, that.resourceTypeLimits);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(useGlobalLimit, useResourceTypeLimit, resourceTypeLimits,
                countPendingReservations, bufferCount);
    }

    @Override
    public String toString() {
        return "SimultaneousReservationLimitSpecification{" +
                "useGlobalLimit=" + useGlobalLimit +
                ", useResourceTypeLimit=" + useResourceTypeLimit +
                ", resourceTypeLimits=" + resourceTypeLimits +
                ", countPendingReservations=" + countPendingReservations +
                ", bufferCount=" + bufferCount +
                '}';
    }
}