package com.hexapass.domain.policy.reservation;

import com.hexapass.domain.policy.ReservationContext;
import com.hexapass.domain.policy.ReservationPolicy;
import com.hexapass.domain.policy.ReservationSpecification;
import com.hexapass.domain.policy.specification.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 프리미엄 예약 정책 - 개선된 버전
 * VIP/프리미엄 회원을 위한 관대한 조건들과 추가 혜택 제공
 * 더 긴 선예약 기간, 더 많은 동시 예약, 수용률 제한 완화
 */
public class PremiumReservationPolicy implements ReservationPolicy {

    private final ReservationSpecification specification;

    // 프리미엄 전용 사양들
    private final ActiveMemberSpecification activeMemberSpec;
    private final MembershipPrivilegeSpecification membershipPrivilegeSpec;
    private final ResourceCapacitySpecification resourceCapacitySpec;
    private final ValidReservationTimeSpecification validTimeSpec;
    private final SimultaneousReservationLimitSpecification simultaneousLimitSpec;
    private final AdvanceReservationLimitSpecification advanceLimitSpec;
    private final WeekendReservationSpecification weekendSpec;

    public PremiumReservationPolicy() {
        // 프리미엄 멤버를 위한 관대한 사양들 설정
        this.activeMemberSpec = ActiveMemberSpecification.lenient(); // 정지 회원도 허용
        this.membershipPrivilegeSpec = MembershipPrivilegeSpecification.withGracePeriod(); // 7일 유예
        this.resourceCapacitySpec = ResourceCapacitySpecification.lenient(); // 100% 수용률 허용
        this.validTimeSpec = new ValidReservationTimeSpecification(730, 15, false, false, 0, 24, true); // 2년, 15분 후
        this.simultaneousLimitSpec = SimultaneousReservationLimitSpecification.lenient(); // 대기중 예약 제외
        this.advanceLimitSpec = AdvanceReservationLimitSpecification.withVipBonus(); // VIP 7일 보너스
        this.weekendSpec = WeekendReservationSpecification.allowWeekend(); // 주말 허용

        // 모든 사양을 AND 조건으로 결합
        this.specification = activeMemberSpec
                .and(membershipPrivilegeSpec)
                .and(resourceCapacitySpec)
                .and(validTimeSpec)
                .and(simultaneousLimitSpec)
                .and(advanceLimitSpec)
                .and(weekendSpec);
    }

    @Override
    public boolean canReserve(ReservationContext context) {
        return specification.isSatisfiedBy(context);
    }

    @Override
    public String getViolationReason(ReservationContext context) {
        if (canReserve(context)) {
            return "예약 가능 (프리미엄 혜택 적용)";
        }

        List<String> violations = new ArrayList<>();

        // 프리미엄 정책에 맞는 상세한 실패 사유 수집
        if (!activeMemberSpec.isSatisfiedBy(context)) {
            String reason = activeMemberSpec.getFailureReason(context);
            violations.add("회원 상태: " + (reason != null ? reason : "심각한 회원 상태 문제"));
        }

        if (!membershipPrivilegeSpec.isSatisfiedBy(context)) {
            String reason = membershipPrivilegeSpec.getFailureReason(context);
            if (membershipPrivilegeSpec.isInGracePeriod(context)) {
                violations.add("멤버십 권한: " + reason + " (유예기간 중)");
            } else {
                violations.add("멤버십 권한: " + (reason != null ? reason : "권한 없음"));
            }
        }

        if (!resourceCapacitySpec.isSatisfiedBy(context)) {
            String reason = resourceCapacitySpec.getFailureReason(context);
            violations.add("리소스: " + (reason != null ? reason : "이용 불가"));
        }

        if (!validTimeSpec.isSatisfiedBy(context)) {
            String reason = validTimeSpec.getFailureReason(context);
            violations.add("예약시간: " + (reason != null ? reason : "시간 제약"));
        }

        if (!simultaneousLimitSpec.isSatisfiedBy(context)) {
            String reason = simultaneousLimitSpec.getFailureReason(context);
            violations.add("동시예약: " + (reason != null ? reason : "한도 초과"));
        }

        if (!advanceLimitSpec.isSatisfiedBy(context)) {
            String reason = advanceLimitSpec.getFailureReason(context);
            violations.add("선예약: " + (reason != null ? reason : "기간 초과"));
        }

        return String.join(" | ", violations);
    }

    @Override
    public String getDescription() {
        return "프리미엄 예약 정책 - VIP/프리미엄 회원을 위한 관대한 예약 조건";
    }

    /**
     * 프리미엄 혜택 상세 정보
     */
    public String getPremiumBenefits() {
        StringBuilder benefits = new StringBuilder();
        benefits.append("프리미엄 회원 혜택:\n");
        benefits.append("- 멤버십 만료 후 7일 유예 기간\n");
        benefits.append("- 리소스 100% 수용률까지 예약 가능\n");
        benefits.append("- 최대 2년까지 선예약 가능\n");
        benefits.append("- 15분 전까지 즉시 예약 가능\n");
        benefits.append("- VIP 회원 추가 7일 선예약 보너스\n");
        benefits.append("- 주말 예약 제한 없음\n");
        benefits.append("- 대기중 예약은 동시 예약 수에 미포함");

        return benefits.toString();
    }

    /**
     * 표준 정책과의 차이점
     */
    public String getAdvantagesOverStandard() {
        StringBuilder advantages = new StringBuilder();
        advantages.append("표준 정책 대비 혜택:\n");
        advantages.append("- 선예약 기간: 1년 → 2년 (VIP는 +7일)\n");
        advantages.append("- 최소 예약 시간: 30분 전 → 15분 전\n");
        advantages.append("- 리소스 수용률: 90% → 100%\n");
        advantages.append("- 멤버십 유예: 없음 → 7일\n");
        advantages.append("- 동시 예약 계산: 모든 예약 → 확정된 예약만\n");
        advantages.append("- 정지 회원: 불가 → 특별한 경우 허용");

        return advantages.toString();
    }

    /**
     * 프리미엄 예약 가능성 진단
     */
    public PremiumReservationDiagnosis diagnose(ReservationContext context) {
        return new PremiumReservationDiagnosis(context, this);
    }

    /**
     * 프리미엄 예약 진단 결과 클래스
     */
    public static class PremiumReservationDiagnosis {
        private final boolean canReserve;
        private final List<String> appliedBenefits;
        private final List<String> availableBenefits;
        private final String recommendedUpgrade;

        private PremiumReservationDiagnosis(ReservationContext context, PremiumReservationPolicy policy) {
            this.canReserve = policy.canReserve(context);
            this.appliedBenefits = new ArrayList<>();
            this.availableBenefits = new ArrayList<>();

            // 적용된 혜택과 이용 가능한 혜택 분석
            analyzeBenefits(context, policy);

            this.recommendedUpgrade = canReserve ? null :
                    "더 높은 등급의 멤버십으로 업그레이드를 고려해보세요";
        }

        private void analyzeBenefits(ReservationContext context, PremiumReservationPolicy policy) {
            // 유예 기간 혜택
            if (policy.membershipPrivilegeSpec.isInGracePeriod(context)) {
                appliedBenefits.add("멤버십 만료 후 유예 기간 적용");
            }

            // VIP 선예약 보너스
            if (context.getMember() != null) {
                String vipInfo = policy.advanceLimitSpec.getVipBenefitInfo(context);
                if (vipInfo.contains("적용")) {
                    appliedBenefits.add("VIP 선예약 보너스 적용");
                } else if (vipInfo.contains("승급")) {
                    availableBenefits.add("VIP 승급 시 추가 선예약 혜택");
                }
            }

            // 기본적으로 제공되는 혜택들
            appliedBenefits.add("프리미엄 예약 조건 적용");
            appliedBenefits.add("주말 예약 허용");
        }

        public boolean canReserve() { return canReserve; }
        public List<String> getAppliedBenefits() { return List.copyOf(appliedBenefits); }
        public List<String> getAvailableBenefits() { return List.copyOf(availableBenefits); }
        public String getRecommendedUpgrade() { return recommendedUpgrade; }

        public String getSummary() {
            return String.format("프리미엄 진단: %s (적용 혜택: %d개)",
                    canReserve ? "예약 가능" : "예약 불가", appliedBenefits.size());
        }
    }

    /**
     * 프리미엄 정책 통계
     */
    public PremiumPolicyStatistics getStatistics() {
        return new PremiumPolicyStatistics();
    }

    /**
     * 프리미엄 정책 통계 클래스
     */
    public static class PremiumPolicyStatistics {
        private final int totalBenefits = 7;
        private final String[] benefitNames = {
                "유예 기간 7일", "100% 수용률", "2년 선예약",
                "15분 전 예약", "VIP 보너스", "주말 허용", "관대한 동시 예약"
        };

        public int getTotalBenefits() { return totalBenefits; }
        public String[] getBenefitNames() { return benefitNames.clone(); }

        public String getDescription() {
            return String.format("프리미엄 정책은 총 %d개의 특별 혜택을 제공합니다", totalBenefits);
        }
    }

    // =========================
    // Getter 메서드들
    // =========================

    public ReservationSpecification getSpecification() {
        return specification;
    }

    public ResourceCapacitySpecification getResourceCapacitySpec() {
        return resourceCapacitySpec;
    }

    public ValidReservationTimeSpecification getValidTimeSpec() {
        return validTimeSpec;
    }

    public AdvanceReservationLimitSpecification getAdvanceLimitSpec() {
        return advanceLimitSpec;
    }
}