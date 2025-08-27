package com.hexapass.domain.policy.specification;

import com.hexapass.domain.model.Member;
import com.hexapass.domain.policy.ReservationContext;
import com.hexapass.domain.policy.ReservationSpecification;
import com.hexapass.domain.type.MemberStatus;

/**
 * 회원 상태 확인 사양 - 개선된 버전
 * 회원이 활성 상태이고 멤버십이 유효한지 포괄적으로 확인
 * 실패 이유를 구체적으로 제공하여 디버깅과 사용자 피드백에 도움
 */
public class ActiveMemberSpecification implements ReservationSpecification {

    private final boolean checkMembershipValidity;
    private final boolean allowSuspendedMembers; // 정지된 회원도 허용할지 여부 (특별 상황용)

    public ActiveMemberSpecification() {
        this(true, false);
    }

    public ActiveMemberSpecification(boolean checkMembershipValidity, boolean allowSuspendedMembers) {
        this.checkMembershipValidity = checkMembershipValidity;
        this.allowSuspendedMembers = allowSuspendedMembers;
    }

    /**
     * 기본 활성 회원 검증 (멤버십 유효성 포함)
     */
    public static ActiveMemberSpecification standard() {
        return new ActiveMemberSpecification(true, false);
    }

    /**
     * 회원 상태만 검증 (멤버십 유효성 제외)
     */
    public static ActiveMemberSpecification statusOnly() {
        return new ActiveMemberSpecification(false, false);
    }

    /**
     * 정지된 회원도 허용하는 관대한 검증
     */
    public static ActiveMemberSpecification lenient() {
        return new ActiveMemberSpecification(true, true);
    }

    @Override
    public boolean isSatisfiedBy(ReservationContext context) {
        Member member = context.getMember();

        // 회원 존재 여부 확인
        if (member == null) {
            return false;
        }

        // 회원 상태 확인
        if (!isValidMemberStatus(member.getStatus())) {
            return false;
        }

        // 멤버십 유효성 확인 (옵션)
        if (checkMembershipValidity && !member.hasMembershipActive()) {
            return false;
        }

        return true;
    }

    @Override
    public String getDescription() {
        StringBuilder desc = new StringBuilder("활성 회원");

        if (allowSuspendedMembers) {
            desc.append(" (정지 회원 포함)");
        }

        if (checkMembershipValidity) {
            desc.append(" 및 유효한 멤버십");
        }

        desc.append(" 여부");

        return desc.toString();
    }

    /**
     * 구체적인 실패 이유를 반환
     */
    public String getFailureReason(ReservationContext context) {
        Member member = context.getMember();

        if (member == null) {
            return "회원 정보가 없습니다";
        }

        MemberStatus status = member.getStatus();
        if (!isValidMemberStatus(status)) {
            if (status == MemberStatus.WITHDRAWN) {
                return "탈퇴한 회원입니다";
            } else if (status == MemberStatus.SUSPENDED && !allowSuspendedMembers) {
                return "정지된 회원입니다" +
                        (member.getSuspensionReason() != null ?
                                " (사유: " + member.getSuspensionReason() + ")" : "");
            } else {
                return "유효하지 않은 회원 상태입니다: " + status.getDisplayName();
            }
        }

        if (checkMembershipValidity && !member.hasMembershipActive()) {
            if (member.getCurrentPlan() == null) {
                return "멤버십 플랜이 할당되지 않았습니다";
            } else if (member.isMembershipExpired()) {
                return "멤버십이 만료되었습니다 (만료일: " +
                        (member.getMembershipPeriod() != null ?
                                member.getMembershipPeriod().getEndDate() : "알 수 없음") + ")";
            } else if (!member.getCurrentPlan().isActive()) {
                return "비활성화된 멤버십 플랜입니다: " + member.getCurrentPlan().getName();
            } else {
                return "멤버십이 유효하지 않습니다";
            }
        }

        return null; // 실패하지 않음
    }

    /**
     * 회원의 멤버십 만료일까지 남은 일수 반환
     */
    public int getRemainingMembershipDays(ReservationContext context) {
        Member member = context.getMember();
        if (member == null || !checkMembershipValidity) {
            return -1;
        }

        return member.getRemainingMembershipDays();
    }

    /**
     * 회원이 멤버십 만료 경고 대상인지 확인
     */
    public boolean isMembershipExpiryWarning(ReservationContext context, int warningDays) {
        Member member = context.getMember();
        if (member == null || !checkMembershipValidity) {
            return false;
        }

        return member.isMembershipExpiryWarning(warningDays);
    }

    /**
     * 회원 상태 요약 정보 반환
     */
    public String getMemberStatusSummary(ReservationContext context) {
        Member member = context.getMember();
        if (member == null) {
            return "회원 없음";
        }

        return member.getSummary();
    }

    // =========================
    // 헬퍼 메서드들
    // =========================

    private boolean isValidMemberStatus(MemberStatus status) {
        if (status == MemberStatus.ACTIVE) {
            return true;
        }

        if (status == MemberStatus.SUSPENDED && allowSuspendedMembers) {
            return true;
        }

        return false; // WITHDRAWN는 항상 거부
    }

    // =========================
    // Getter 메서드들
    // =========================

    public boolean isCheckMembershipValidity() {
        return checkMembershipValidity;
    }

    public boolean isAllowSuspendedMembers() {
        return allowSuspendedMembers;
    }

    // =========================
    // Object 메서드 오버라이드
    // =========================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        ActiveMemberSpecification that = (ActiveMemberSpecification) obj;
        return checkMembershipValidity == that.checkMembershipValidity &&
                allowSuspendedMembers == that.allowSuspendedMembers;
    }

    @Override
    public int hashCode() {
        int result = Boolean.hashCode(checkMembershipValidity);
        result = 31 * result + Boolean.hashCode(allowSuspendedMembers);
        return result;
    }

    @Override
    public String toString() {
        return "ActiveMemberSpecification{" +
                "checkMembershipValidity=" + checkMembershipValidity +
                ", allowSuspendedMembers=" + allowSuspendedMembers +
                '}';
    }
}