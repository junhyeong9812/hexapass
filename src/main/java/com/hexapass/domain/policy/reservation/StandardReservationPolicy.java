package com.hexapass.domain.policy.reservation;

import com.hexapass.domain.policy.ReservationContext;
import com.hexapass.domain.policy.ReservationPolicy;
import com.hexapass.domain.policy.ReservationSpecification;
import com.hexapass.domain.policy.specification.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 표준 예약 정책 - 개선된 버전
 * 모든 기본 조건들을 포함하며, 상세한 위반 사유 분석 제공
 * 사양들을 재사용하여 성능 최적화
 */
public class StandardReservationPolicy implements ReservationPolicy {

    private final ReservationSpecification specification;

    // 개별 사양들을 필드로 저장하여 재사용 (성능 최적화)
    private final ActiveMemberSpecification activeMemberSpec;
    private final MembershipPrivilegeSpecification membershipPrivilegeSpec;
    private final ResourceCapacitySpecification resourceCapacitySpec;
    private final ValidReservationTimeSpecification validTimeSpec;
    private final SimultaneousReservationLimitSpecification simultaneousLimitSpec;
    private final AdvanceReservationLimitSpecification advanceLimitSpec;

    public StandardReservationPolicy() {
        // 개별 사양들 초기화
        this.activeMemberSpec = ActiveMemberSpecification.standard();
        this.membershipPrivilegeSpec = MembershipPrivilegeSpecification.standard();
        this.resourceCapacitySpec = ResourceCapacitySpecification.standard();
        this.validTimeSpec = new ValidReservationTimeSpecification();
        this.simultaneousLimitSpec = SimultaneousReservationLimitSpecification.standard();
        this.advanceLimitSpec = AdvanceReservationLimitSpecification.standard();

        // 모든 사양을 AND 조건으로 결합
        this.specification = activeMemberSpec
                .and(membershipPrivilegeSpec)
                .and(resourceCapacitySpec)
                .and(validTimeSpec)
                .and(simultaneousLimitSpec)
                .and(advanceLimitSpec);
    }

    @Override
    public boolean canReserve(ReservationContext context) {
        return specification.isSatisfiedBy(context);
    }

    @Override
    public String getViolationReason(ReservationContext context) {
        if (canReserve(context)) {
            return "예약 가능";
        }

        List<String> violations = new ArrayList<>();

        // 각 사양별 상세한 실패 사유 수집
        if (!activeMemberSpec.isSatisfiedBy(context)) {
            String failureReason = activeMemberSpec.getFailureReason(context);
            violations.add("회원 상태: " + (failureReason != null ? failureReason : "회원 상태 문제"));
        }

        if (!membershipPrivilegeSpec.isSatisfiedBy(context)) {
            String failureReason = membershipPrivilegeSpec.getFailureReason(context);
            violations.add("멤버십 권한: " + (failureReason != null ? failureReason : "권한 부족"));
        }

        if (!resourceCapacitySpec.isSatisfiedBy(context)) {
            String failureReason = resourceCapacitySpec.getFailureReason(context);
            violations.add("리소스 수용력: " + (failureReason != null ? failureReason : "수용 인원 초과"));
        }

        if (!validTimeSpec.isSatisfiedBy(context)) {
            String failureReason = validTimeSpec.getFailureReason(context);
            violations.add("예약 시간: " + (failureReason != null ? failureReason : "유효하지 않은 시간"));
        }

        if (!simultaneousLimitSpec.isSatisfiedBy(context)) {
            String failureReason = simultaneousLimitSpec.getFailureReason(context);
            violations.add("동시 예약: " + (failureReason != null ? failureReason : "동시 예약 한도 초과"));
        }

        if (!advanceLimitSpec.isSatisfiedBy(context)) {
            String failureReason = advanceLimitSpec.getFailureReason(context);
            violations.add("선예약 기간: " + (failureReason != null ? failureReason : "선예약 기간 초과"));
        }

        return String.join(" | ", violations);
    }

    @Override
    public String getDescription() {
        return "표준 예약 정책 - 기본적인 예약 조건들을 모두 포함";
    }

    /**
     * 정책 상세 정보 반환
     */
    public String getPolicyDetails() {
        StringBuilder details = new StringBuilder();
        details.append("표준 예약 정책 상세:\n");
        details.append("- 회원 상태: 활성 회원 및 유효한 멤버십 필요\n");
        details.append("- 멤버십 권한: 리소스 이용 권한 확인\n");
        details.append("- 리소스 수용력: 90% 이하 수용률 유지\n");
        details.append("- 예약 시간: 1년 이내, 30분 후부터 가능\n");
        details.append("- 동시 예약: 플랜별 제한 적용\n");
        details.append("- 선예약 기간: 플랜별 제한 적용");

        return details.toString();
    }

    /**
     * 예약 가능성 진단 정보
     */
    public ReservationDiagnosis diagnose(ReservationContext context) {
        return new ReservationDiagnosis(context, this);
    }

    /**
     * 예약 진단 결과 클래스
     */
    public static class ReservationDiagnosis {
        private final boolean canReserve;
        private final List<String> passedChecks;
        private final List<String> failedChecks;
        private final String overallStatus;

        private ReservationDiagnosis(ReservationContext context, StandardReservationPolicy policy) {
            this.canReserve = policy.canReserve(context);
            this.passedChecks = new ArrayList<>();
            this.failedChecks = new ArrayList<>();

            // 각 조건별 상태 확인
            checkCondition("회원 상태", policy.activeMemberSpec, context);
            checkCondition("멤버십 권한", policy.membershipPrivilegeSpec, context);
            checkCondition("리소스 수용력", policy.resourceCapacitySpec, context);
            checkCondition("예약 시간", policy.validTimeSpec, context);
            checkCondition("동시 예약 제한", policy.simultaneousLimitSpec, context);
            checkCondition("선예약 기간", policy.advanceLimitSpec, context);

            this.overallStatus = canReserve ? "예약 가능" :
                    String.format("예약 불가 (%d개 조건 위반)", failedChecks.size());
        }

        private void checkCondition(String name, ReservationSpecification spec, ReservationContext context) {
            if (spec.isSatisfiedBy(context)) {
                passedChecks.add(name);
            } else {
                failedChecks.add(name);
            }
        }

        // Getter methods
        public boolean canReserve() { return canReserve; }
        public List<String> getPassedChecks() { return List.copyOf(passedChecks); }
        public List<String> getFailedChecks() { return List.copyOf(failedChecks); }
        public String getOverallStatus() { return overallStatus; }

        public String getSummary() {
            return String.format("진단 결과: %s (통과: %d, 실패: %d)",
                    overallStatus, passedChecks.size(), failedChecks.size());
        }
    }

    /**
     * 정책 적용 통계
     */
    public PolicyStatistics getStatistics() {
        return new PolicyStatistics();
    }

    /**
     * 정책 통계 클래스
     */
    public static class PolicyStatistics {
        private final int totalChecks = 6;
        private final String[] checkNames = {
                "회원 상태", "멤버십 권한", "리소스 수용력",
                "예약 시간", "동시 예약 제한", "선예약 기간"
        };

        public int getTotalChecks() { return totalChecks; }
        public String[] getCheckNames() { return checkNames.clone(); }

        public String getDescription() {
            return String.format("표준 정책은 총 %d개의 조건을 검사합니다", totalChecks);
        }
    }

    // =========================
    // Getter 메서드들
    // =========================

    public ReservationSpecification getSpecification() {
        return specification;
    }

    public ActiveMemberSpecification getActiveMemberSpec() {
        return activeMemberSpec;
    }

    public MembershipPrivilegeSpecification getMembershipPrivilegeSpec() {
        return membershipPrivilegeSpec;
    }

    public ResourceCapacitySpecification getResourceCapacitySpec() {
        return resourceCapacitySpec;
    }

    public ValidReservationTimeSpecification getValidTimeSpec() {
        return validTimeSpec;
    }

    public SimultaneousReservationLimitSpecification getSimultaneousLimitSpec() {
        return simultaneousLimitSpec;
    }

    public AdvanceReservationLimitSpecification getAdvanceLimitSpec() {
        return advanceLimitSpec;
    }
}