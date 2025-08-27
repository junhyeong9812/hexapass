package com.hexapass.domain.policy.specification;

import com.hexapass.domain.model.Member;
import com.hexapass.domain.model.MembershipPlan;
import com.hexapass.domain.policy.ReservationContext;
import com.hexapass.domain.policy.ReservationSpecification;
import com.hexapass.domain.type.ResourceType;

import java.util.Set;

/**
 * 멤버십 권한 확인 사양 - 개선된 버전
 * 회원의 멤버십에서 해당 리소스 타입에 대한 이용 권한이 있는지 확인
 * 권한 부족 시 구체적인 안내와 업그레이드 제안 제공
 */
public class MembershipPrivilegeSpecification implements ReservationSpecification {

    private final boolean checkDateValidity;        // 예약 날짜가 멤버십 기간 내인지 확인
    private final boolean allowGracePeriod;         // 멤버십 만료 후 유예 기간 허용
    private final int gracePeriodDays;             // 유예 기간 (일)
    private final Set<ResourceType> requiredPrivileges; // 필요한 권한들 (null이면 컨텍스트의 리소스 타입 사용)

    public MembershipPrivilegeSpecification() {
        this(true, false, 0, null);
    }

    public MembershipPrivilegeSpecification(boolean checkDateValidity, boolean allowGracePeriod,
                                            int gracePeriodDays, Set<ResourceType> requiredPrivileges) {
        this.checkDateValidity = checkDateValidity;
        this.allowGracePeriod = allowGracePeriod;
        this.gracePeriodDays = validateGracePeriod(gracePeriodDays);
        this.requiredPrivileges = requiredPrivileges != null ? Set.copyOf(requiredPrivileges) : null;
    }

    /**
     * 표준 권한 검증 (날짜 유효성 포함)
     */
    public static MembershipPrivilegeSpecification standard() {
        return new MembershipPrivilegeSpecification(true, false, 0, null);
    }

    /**
     * 관대한 권한 검증 (7일 유예 기간)
     */
    public static MembershipPrivilegeSpecification withGracePeriod() {
        return new MembershipPrivilegeSpecification(true, true, 7, null);
    }

    /**
     * 권한만 검증 (날짜 유효성 제외)
     */
    public static MembershipPrivilegeSpecification privilegeOnly() {
        return new MembershipPrivilegeSpecification(false, false, 0, null);
    }

    /**
     * 특정 리소스 타입들에 대한 권한 검증
     */
    public static MembershipPrivilegeSpecification forResources(Set<ResourceType> resourceTypes) {
        return new MembershipPrivilegeSpecification(true, false, 0, resourceTypes);
    }

    @Override
    public boolean isSatisfiedBy(ReservationContext context) {
        Member member = context.getMember();
        if (member == null) {
            return false;
        }

        MembershipPlan plan = member.getCurrentPlan();
        if (plan == null || !plan.isActive()) {
            return false;
        }

        // 필요한 권한 결정
        Set<ResourceType> privilegesToCheck = requiredPrivileges != null ?
                requiredPrivileges : Set.of(context.getResourceType());

        // 권한 확인
        for (ResourceType resourceType : privilegesToCheck) {
            if (!plan.hasPrivilege(resourceType)) {
                return false;
            }
        }

        // 날짜 유효성 검증
        if (checkDateValidity) {
            boolean canReserve = member.canReserve(context.getResourceType(), context.getReservationDate());

            // 멤버십 만료된 경우 유예 기간 확인
            if (!canReserve && allowGracePeriod && member.isMembershipExpired()) {
                int daysSinceExpiry = getDaysSinceExpiry(member);
                if (daysSinceExpiry <= gracePeriodDays) {
                    return true; // 유예 기간 내
                }
            }

            return canReserve;
        }

        return true;
    }

    @Override
    public String getDescription() {
        StringBuilder desc = new StringBuilder("멤버십 리소스 이용 권한");

        if (requiredPrivileges != null && !requiredPrivileges.isEmpty()) {
            desc.append(" (").append(formatResourceTypes(requiredPrivileges)).append(")");
        }

        if (checkDateValidity) {
            desc.append(" 및 기간 유효성");
        }

        if (allowGracePeriod) {
            desc.append(" (").append(gracePeriodDays).append("일 유예)");
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
            return "멤버십 플랜이 할당되지 않았습니다";
        }

        if (!plan.isActive()) {
            return "비활성화된 멤버십 플랜입니다: " + plan.getName();
        }

        // 필요한 권한 결정
        Set<ResourceType> privilegesToCheck = requiredPrivileges != null ?
                requiredPrivileges : Set.of(context.getResourceType());

        // 권한 부족 확인
        for (ResourceType resourceType : privilegesToCheck) {
            if (!plan.hasPrivilege(resourceType)) {
                return String.format("현재 멤버십('%s')으로는 '%s' 이용 권한이 없습니다",
                        plan.getName(), resourceType.getDisplayName());
            }
        }

        // 날짜 유효성 문제 확인
        if (checkDateValidity) {
            if (member.getMembershipPeriod() == null) {
                return "멤버십 기간이 설정되지 않았습니다";
            }

            if (member.isMembershipExpired()) {
                int daysSinceExpiry = getDaysSinceExpiry(member);

                if (allowGracePeriod && daysSinceExpiry <= gracePeriodDays) {
                    return null; // 유예 기간 내이므로 실패하지 않음
                }

                return String.format("멤버십이 만료되었습니다 (만료일: %s, %d일 경과)",
                        member.getMembershipPeriod().getEndDate(), daysSinceExpiry);
            }

            if (!member.getMembershipPeriod().contains(context.getReservationDate())) {
                return String.format("예약 날짜가 멤버십 기간(%s)을 벗어났습니다",
                        member.getMembershipPeriod());
            }
        }

        return null; // 실패하지 않음
    }

    /**
     * 업그레이드 제안 메시지
     */
    public String getUpgradeSuggestion(ReservationContext context) {
        Member member = context.getMember();
        if (member == null || member.getCurrentPlan() == null) {
            return "적절한 멤버십을 선택해주세요";
        }

        ResourceType targetResource = context.getResourceType();
        String currentPlan = member.getCurrentPlan().getName();

        if (targetResource.isFitnessRelated()) {
            return String.format("'%s'을(를) 이용하려면 피트니스 권한이 포함된 플랜으로 업그레이드하세요",
                    targetResource.getDisplayName());
        } else if (targetResource.isWorkspaceRelated()) {
            return String.format("'%s'을(를) 이용하려면 워크스페이스 권한이 포함된 플랜으로 업그레이드하세요",
                    targetResource.getDisplayName());
        }

        return String.format("현재 플랜('%s')으로는 '%s' 이용이 제한됩니다. 상위 플랜을 고려해보세요",
                currentPlan, targetResource.getDisplayName());
    }

    /**
     * 권한 상세 정보
     */
    public String getPrivilegeDetails(ReservationContext context) {
        Member member = context.getMember();
        if (member == null || member.getCurrentPlan() == null) {
            return "멤버십 정보 없음";
        }

        MembershipPlan plan = member.getCurrentPlan();
        Set<ResourceType> allowedTypes = plan.getAllowedResourceTypes();

        StringBuilder details = new StringBuilder();
        details.append("현재 플랜: ").append(plan.getName()).append("\n");
        details.append("이용 가능 리소스: ").append(formatResourceTypes(allowedTypes)).append("\n");
        details.append("멤버십 기간: ").append(member.getMembershipPeriod()).append("\n");
        details.append("잔여 일수: ").append(member.getRemainingMembershipDays()).append("일");

        return details.toString();
    }

    /**
     * 유예 기간 상태 확인
     */
    public boolean isInGracePeriod(ReservationContext context) {
        if (!allowGracePeriod) {
            return false;
        }

        Member member = context.getMember();
        if (member == null || !member.isMembershipExpired()) {
            return false;
        }

        return getDaysSinceExpiry(member) <= gracePeriodDays;
    }

    // =========================
    // 헬퍼 메서드들
    // =========================

    private int getDaysSinceExpiry(Member member) {
        if (member.getMembershipPeriod() == null || !member.isMembershipExpired()) {
            return 0;
        }

        return (int) member.getMembershipPeriod().getEndDate()
                .until(java.time.LocalDate.now()).getDays();
    }

    private String formatResourceTypes(Set<ResourceType> types) {
        return types.stream()
                .map(ResourceType::getDisplayName)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private int validateGracePeriod(int days) {
        if (days < 0) {
            throw new IllegalArgumentException("유예 기간은 0 이상이어야 합니다: " + days);
        }
        return days;
    }

    // =========================
    // Getter 메서드들
    // =========================

    public boolean isCheckDateValidity() {
        return checkDateValidity;
    }

    public boolean isAllowGracePeriod() {
        return allowGracePeriod;
    }

    public int getGracePeriodDays() {
        return gracePeriodDays;
    }

    public Set<ResourceType> getRequiredPrivileges() {
        return requiredPrivileges != null ? Set.copyOf(requiredPrivileges) : null;
    }

    // =========================
    // Object 메서드 오버라이드
    // =========================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        MembershipPrivilegeSpecification that = (MembershipPrivilegeSpecification) obj;
        return checkDateValidity == that.checkDateValidity &&
                allowGracePeriod == that.allowGracePeriod &&
                gracePeriodDays == that.gracePeriodDays &&
                java.util.Objects.equals(requiredPrivileges, that.requiredPrivileges);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(checkDateValidity, allowGracePeriod, gracePeriodDays, requiredPrivileges);
    }

    @Override
    public String toString() {
        return "MembershipPrivilegeSpecification{" +
                "checkDateValidity=" + checkDateValidity +
                ", allowGracePeriod=" + allowGracePeriod +
                ", gracePeriodDays=" + gracePeriodDays +
                ", requiredPrivileges=" + requiredPrivileges +
                '}';
    }
}