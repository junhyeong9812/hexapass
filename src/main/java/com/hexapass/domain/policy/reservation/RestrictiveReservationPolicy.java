package com.hexapass.domain.policy.reservation;

import com.hexapass.domain.policy.ReservationContext;
import com.hexapass.domain.policy.ReservationPolicy;
import com.hexapass.domain.policy.ReservationSpecification;
import com.hexapass.domain.policy.specification.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 제한적 예약 정책 - 개선된 버전
 * 더 엄격한 조건들과 추가 제약사항을 적용하여 리소스 관리 최적화
 * 피크 시간대나 인기 시설에 적용하여 공정한 이용 기회 보장
 */
public class RestrictiveReservationPolicy implements ReservationPolicy {

    private final ReservationSpecification specification;

    // 제한적 정책 전용 사양들
    private final ActiveMemberSpecification activeMemberSpec;
    private final MembershipPrivilegeSpecification membershipPrivilegeSpec;
    private final ResourceCapacitySpecification resourceCapacitySpec;
    private final ValidReservationTimeSpecification validTimeSpec;
    private final SimultaneousReservationLimitSpecification simultaneousLimitSpec;
    private final AdvanceReservationLimitSpecification advanceLimitSpec;
    private final WeekendReservationSpecification weekendSpec;
    private final TimeSlotRestrictionSpecification timeSlotSpec;

    public RestrictiveReservationPolicy() {
        // 엄격한 제한 조건들 설정
        this.activeMemberSpec = new ActiveMemberSpecification(true, false); // 멤버십 유효성 엄격 확인
        this.membershipPrivilegeSpec = MembershipPrivilegeSpecification.standard(); // 유예 기간 없음
        this.resourceCapacitySpec = ResourceCapacitySpecification.strict(); // 80% 수용률, 2명 여유
        this.validTimeSpec = new ValidReservationTimeSpecification(30, 120, false, true, 9, 21, false); // 30일, 2시간 전, 영업시간, 평일만
        this.simultaneousLimitSpec = SimultaneousReservationLimitSpecification.strict(); // 여유 예약 1개
        this.advanceLimitSpec = AdvanceReservationLimitSpecification.strict(); // 당일 예약 불허, 24시간 전
        this.weekendSpec = WeekendReservationSpecification.restrictWeekend(); // 주말 제한
        this.timeSlotSpec = TimeSlotRestrictionSpecification.businessHours(); // 비즈니스 시간만

        // 모든 엄격한 사양들을 AND 조건으로 결합
        this.specification = activeMemberSpec
                .and(membershipPrivilegeSpec)
                .and(resourceCapacitySpec)
                .and(validTimeSpec)
                .and(simultaneousLimitSpec)
                .and(advanceLimitSpec)
                .and(weekendSpec)
                .and(timeSlotSpec);
    }

    @Override
    public boolean canReserve(ReservationContext context) {
        return specification.isSatisfiedBy(context);
    }

    @Override
    public String getViolationReason(ReservationContext context) {
        if (canReserve(context)) {
            return "예약 가능 (모든 제한 조건 만족)";
        }

        List<String> violations = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        // 각 제한 조건별 상세한 실패 사유와 개선 제안 수집
        if (!activeMemberSpec.isSatisfiedBy(context)) {
            String reason = activeMemberSpec.getFailureReason(context);
            violations.add("회원상태: " + (reason != null ? reason : "회원 자격 미충족"));
            suggestions.add("회원 상태를 확인하고 멤버십을 갱신하세요");
        }

        if (!membershipPrivilegeSpec.isSatisfiedBy(context)) {
            String reason = membershipPrivilegeSpec.getFailureReason(context);
            violations.add("권한: " + (reason != null ? reason : "이용 권한 없음"));
            String upgrade = membershipPrivilegeSpec.getUpgradeSuggestion(context);
            if (upgrade != null) {
                suggestions.add(upgrade);
            }
        }

        if (!resourceCapacitySpec.isSatisfiedBy(context)) {
            String reason = resourceCapacitySpec.getFailureReason(context);
            violations.add("수용력: " + (reason != null ? reason : "여유 공간 부족"));
            suggestions.add("다른 시간대를 선택하거나 대기 등록하세요");
        }

        if (!validTimeSpec.isSatisfiedBy(context)) {
            String reason = validTimeSpec.getFailureReason(context);
            violations.add("시간: " + (reason != null ? reason : "예약 시간 제한"));
            suggestions.add("영업시간(9-21시) 내 평일에 예약하세요");
        }

        if (!simultaneousLimitSpec.isSatisfiedBy(context)) {
            String reason = simultaneousLimitSpec.getFailureReason(context);
            violations.add("동시예약: " + (reason != null ? reason : "예약 한도 초과"));
            suggestions.add("기존 예약을 완료한 후 새로 예약하세요");
        }

        if (!advanceLimitSpec.isSatisfiedBy(context)) {
            String reason = advanceLimitSpec.getFailureReason(context);
            violations.add("선예약: " + (reason != null ? reason : "선예약 기간 위반"));
            suggestions.add("최소 24시간 전에 미리 예약하세요");
        }

        if (!weekendSpec.isSatisfiedBy(context)) {
            violations.add("요일제한: 주말 예약 불가");
            suggestions.add("평일(월-금)에 예약하세요");
        }

        if (!timeSlotSpec.isSatisfiedBy(context)) {
            violations.add("시간대제한: 영업시간 외");
            suggestions.add("9시-18시 사이에 예약하세요");
        }

        String result = String.join(" | ", violations);
        if (!suggestions.isEmpty()) {
            result += "\n제안사항: " + String.join(", ", suggestions);
        }

        return result;
    }

    @Override
    public String getDescription() {
        return "제한적 예약 정책 - 엄격한 조건으로 리소스 이용의 공정성 보장";
    }

    /**
     * 제한 정책의 목적과 효과
     */
    public String getPolicyObjectives() {
        StringBuilder objectives = new StringBuilder();
        objectives.append("제한적 정책의 목적:\n");
        objectives.append("- 리소스의 공정한 배분 보장\n");
        objectives.append("- 피크 시간대 과부하 방지\n");
        objectives.append("- 모든 회원에게 균등한 이용 기회 제공\n");
        objectives.append("- 시설 관리 효율성 향상\n");
        objectives.append("- 높은 서비스 품질 유지");

        return objectives.toString();
    }

    /**
     * 제한 조건 상세 설명
     */
    public String getRestrictionDetails() {
        StringBuilder details = new StringBuilder();
        details.append("적용되는 제한 조건들:\n");
        details.append("- 회원: 활성 상태 + 유효한 멤버십 필수\n");
        details.append("- 수용률: 최대 80% (여유 2명 확보)\n");
        details.append("- 예약시간: 30일 이내, 최소 2시간 전\n");
        details.append("- 운영시간: 평일 9-21시만 가능\n");
        details.append("- 동시예약: 엄격한 한도 (여유 1개)\n");
        details.append("- 선예약: 당일 예약 불허, 24시간 전 필수\n");
        details.append("- 요일: 주말 이용 제한\n");
        details.append("- 시간대: 비즈니스 시간 (9-18시)만");

        return details.toString();
    }

    /**
     * 다른 정책 대비 차이점
     */
    public String getRestrictionsVsOtherPolicies() {
        StringBuilder comparison = new StringBuilder();
        comparison.append("다른 정책 대비 제한사항:\n");
        comparison.append("표준 정책 대비:\n");
        comparison.append("- 수용률: 90% → 80%\n");
        comparison.append("- 최소 예약 시간: 30분 → 2시간\n");
        comparison.append("- 주말 예약: 허용 → 제한\n");
        comparison.append("- 영업시간: 제한 없음 → 9-21시\n\n");
        comparison.append("프리미엄 정책 대비:\n");
        comparison.append("- 선예약 기간: 2년 → 30일\n");
        comparison.append("- 유예 기간: 7일 → 없음\n");
        comparison.append("- 당일 예약: 허용 → 불허\n");
        comparison.append("- 시간 제한: 거의 없음 → 엄격");

        return comparison.toString();
    }

    /**
     * 제한적 예약 진단
     */
    public RestrictiveReservationDiagnosis diagnose(ReservationContext context) {
        return new RestrictiveReservationDiagnosis(context, this);
    }

    /**
     * 제한적 예약 진단 결과 클래스
     */
    public static class RestrictiveReservationDiagnosis {
        private final boolean canReserve;
        private final int totalRestrictions = 8;
        private final int violatedRestrictions;
        private final List<String> passedRestrictions;
        private final List<String> violatedRestrictionsList;
        private final String severityLevel;
        private final List<String> improvementSteps;

        private RestrictiveReservationDiagnosis(ReservationContext context, RestrictiveReservationPolicy policy) {
            this.canReserve = policy.canReserve(context);
            this.passedRestrictions = new ArrayList<>();
            this.violatedRestrictionsList = new ArrayList<>();
            this.improvementSteps = new ArrayList<>();

            // 각 제한 조건별 상태 분석
            analyzeRestrictions(context, policy);

            this.violatedRestrictions = violatedRestrictionsList.size();
            this.severityLevel = determineSeverityLevel();

            generateImprovementSteps(context);
        }

        private void analyzeRestrictions(ReservationContext context, RestrictiveReservationPolicy policy) {
            checkRestriction("회원 상태", policy.activeMemberSpec, context);
            checkRestriction("멤버십 권한", policy.membershipPrivilegeSpec, context);
            checkRestriction("리소스 수용력", policy.resourceCapacitySpec, context);
            checkRestriction("예약 시간 유효성", policy.validTimeSpec, context);
            checkRestriction("동시 예약 제한", policy.simultaneousLimitSpec, context);
            checkRestriction("선예약 기간", policy.advanceLimitSpec, context);
            checkRestriction("주말 제한", policy.weekendSpec, context);
            checkRestriction("시간대 제한", policy.timeSlotSpec, context);
        }

        private void checkRestriction(String name, ReservationSpecification spec, ReservationContext context) {
            if (spec.isSatisfiedBy(context)) {
                passedRestrictions.add(name);
            } else {
                violatedRestrictionsList.add(name);
            }
        }

        private String determineSeverityLevel() {
            if (violatedRestrictions == 0) return "완전 준수";
            else if (violatedRestrictions <= 2) return "경미한 위반";
            else if (violatedRestrictions <= 4) return "중간 위반";
            else return "심각한 위반";
        }

        private void generateImprovementSteps(ReservationContext context) {
            if (canReserve) {
                improvementSteps.add("모든 조건을 만족합니다");
                return;
            }

            improvementSteps.add("다음 단계를 따라 예약 조건을 개선하세요:");

            if (violatedRestrictionsList.contains("회원 상태")) {
                improvementSteps.add("1. 회원 상태 및 멤버십 유효성 확인");
            }
            if (violatedRestrictionsList.contains("멤버십 권한")) {
                improvementSteps.add("2. 필요한 리소스 이용 권한이 포함된 플랜으로 업그레이드");
            }
            if (violatedRestrictionsList.contains("주말 제한") || violatedRestrictionsList.contains("시간대 제한")) {
                improvementSteps.add("3. 평일 비즈니스 시간(9-18시) 내 예약 선택");
            }
            if (violatedRestrictionsList.contains("선예약 기간")) {
                improvementSteps.add("4. 최소 24시간 전 미리 예약");
            }
            if (violatedRestrictionsList.contains("리소스 수용력")) {
                improvementSteps.add("5. 다른 시간대 선택 또는 대기 등록");
            }
        }

        // Getter methods
        public boolean canReserve() { return canReserve; }
        public int getTotalRestrictions() { return totalRestrictions; }
        public int getViolatedRestrictions() { return violatedRestrictions; }
        public List<String> getPassedRestrictions() { return List.copyOf(passedRestrictions); }
        public List<String> getViolatedRestrictionsList() { return List.copyOf(violatedRestrictionsList); }
        public String getSeverityLevel() { return severityLevel; }
        public List<String> getImprovementSteps() { return List.copyOf(improvementSteps); }

        public String getSummary() {
            return String.format("제한적 정책 진단: %s (위반: %d/%d, 심각도: %s)",
                    canReserve ? "예약 가능" : "예약 불가",
                    violatedRestrictions, totalRestrictions, severityLevel);
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

    public WeekendReservationSpecification getWeekendSpec() {
        return weekendSpec;
    }

    public TimeSlotRestrictionSpecification getTimeSlotSpec() {
        return timeSlotSpec;
    }
}