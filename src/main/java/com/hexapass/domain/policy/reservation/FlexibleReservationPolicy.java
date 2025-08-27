package com.hexapass.domain.policy.reservation;

import com.hexapass.domain.policy.ReservationContext;
import com.hexapass.domain.policy.ReservationPolicy;
import com.hexapass.domain.policy.ReservationSpecification;
import com.hexapass.domain.policy.specification.*;

import java.util.*;

/**
 * 유연한 예약 정책 - 개선된 버전
 * 사양들을 동적으로 조합할 수 있는 정책으로 런타임에 조건 변경 가능
 * 빌더 패턴과 체이닝을 지원하여 직관적인 정책 구성
 */
public class FlexibleReservationPolicy implements ReservationPolicy {

    private final ReservationSpecification specification;
    private final String name;
    private final String description;
    private final PolicyLevel level;
    private final Map<String, Object> metadata;
    private final List<ReservationSpecification> individualSpecs;

    public enum PolicyLevel {
        MINIMAL("최소한의 검증만 수행"),
        BASIC("기본적인 조건들 검증"),
        STANDARD("표준적인 모든 조건 검증"),
        STRICT("엄격한 조건들로 검증"),
        CUSTOM("사용자 정의 조건 조합");

        private final String description;
        PolicyLevel(String description) { this.description = description; }
        public String getDescription() { return description; }
    }

    private FlexibleReservationPolicy(Builder builder) {
        this.specification = builder.buildSpecification();
        this.name = builder.name != null ? builder.name : "유연한 예약 정책";
        this.description = builder.description != null ? builder.description : "동적으로 구성된 예약 정책";
        this.level = builder.level;
        this.metadata = Map.copyOf(builder.metadata);
        this.individualSpecs = List.copyOf(builder.specifications);
    }

    /**
     * 사양을 직접 조합한 유연한 정책 생성
     */
    public static FlexibleReservationPolicy create(ReservationSpecification specification, String name) {
        return new Builder(name)
                .withCustomSpecification(specification)
                .build();
    }

    /**
     * 빌더 패턴으로 정책 생성
     */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    /**
     * 사전 정의된 레벨별 정책 생성
     */
    public static FlexibleReservationPolicy ofLevel(PolicyLevel level, String name) {
        return new Builder(name).withLevel(level).build();
    }

    /**
     * 빌더 클래스
     */
    public static class Builder {
        private final String name;
        private String description;
        private PolicyLevel level = PolicyLevel.CUSTOM;
        private final List<ReservationSpecification> specifications = new ArrayList<>();
        private final Map<String, Object> metadata = new HashMap<>();
        private SpecificationCombineMode combineMode = SpecificationCombineMode.AND;

        public enum SpecificationCombineMode {
            AND, OR, CUSTOM
        }

        public Builder(String name) {
            this.name = validateNotBlank(name, "정책명");
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withLevel(PolicyLevel level) {
            this.level = validateNotNull(level, "정책 레벨");
            addLevelBasedSpecifications(level);
            return this;
        }

        public Builder withSpecification(ReservationSpecification spec) {
            if (spec != null) {
                this.specifications.add(spec);
            }
            return this;
        }

        public Builder withCustomSpecification(ReservationSpecification spec) {
            this.level = PolicyLevel.CUSTOM;
            return withSpecification(spec);
        }

        public Builder withActiveMemberCheck() {
            return withSpecification(ActiveMemberSpecification.standard());
        }

        public Builder withMembershipPrivilegeCheck() {
            return withSpecification(MembershipPrivilegeSpecification.standard());
        }

        public Builder withCapacityCheck() {
            return withSpecification(new ResourceCapacitySpecification());
        }

        public Builder withStrictCapacityCheck() {
            return withSpecification(ResourceCapacitySpecification.strict());
        }

        public Builder withLenientCapacityCheck() {
            return withSpecification(ResourceCapacitySpecification.lenient());
        }

        public Builder withTimeValidation(int maxDays, int minMinutes) {
            return withSpecification(new ValidReservationTimeSpecification(maxDays, minMinutes, false, false, 0, 24, true));
        }

        public Builder withWeekendPolicy(boolean allowWeekend) {
            if (allowWeekend) {
                return withSpecification(WeekendReservationSpecification.allowWeekend());
            } else {
                return withSpecification(WeekendReservationSpecification.restrictWeekend());
            }
        }

        public Builder withBusinessHoursOnly() {
            return withSpecification(TimeSlotRestrictionSpecification.businessHours());
        }

        public Builder withSimultaneousLimit() {
            return withSpecification(SimultaneousReservationLimitSpecification.standard());
        }

        public Builder withAdvanceLimit() {
            return withSpecification(AdvanceReservationLimitSpecification.standard());
        }

        public Builder withCombineMode(SpecificationCombineMode mode) {
            this.combineMode = mode;
            return this;
        }

        public Builder withMetadata(String key, Object value) {
            this.metadata.put(key, value);
            return this;
        }

        public FlexibleReservationPolicy build() {
            if (specifications.isEmpty()) {
                throw new IllegalStateException("최소 하나의 사양이 필요합니다");
            }
            return new FlexibleReservationPolicy(this);
        }

        private ReservationSpecification buildSpecification() {
            if (specifications.isEmpty()) {
                throw new IllegalStateException("사양이 없습니다");
            }

            if (specifications.size() == 1) {
                return specifications.get(0);
            }

            ReservationSpecification result = specifications.get(0);
            for (int i = 1; i < specifications.size(); i++) {
                if (combineMode == SpecificationCombineMode.AND) {
                    result = result.and(specifications.get(i));
                } else if (combineMode == SpecificationCombineMode.OR) {
                    result = result.or(specifications.get(i));
                }
            }

            return result;
        }

        private void addLevelBasedSpecifications(PolicyLevel level) {
            specifications.clear(); // 기존 사양들 초기화

            switch (level) {
                case MINIMAL:
                    withActiveMemberCheck();
                    break;

                case BASIC:
                    withActiveMemberCheck()
                            .withMembershipPrivilegeCheck()
                            .withLenientCapacityCheck();
                    break;

                case STANDARD:
                    withActiveMemberCheck()
                            .withMembershipPrivilegeCheck()
                            .withCapacityCheck()
                            .withTimeValidation(365, 30)
                            .withSimultaneousLimit()
                            .withAdvanceLimit();
                    break;

                case STRICT:
                    withActiveMemberCheck()
                            .withMembershipPrivilegeCheck()
                            .withStrictCapacityCheck()
                            .withTimeValidation(30, 120)
                            .withSimultaneousLimit()
                            .withAdvanceLimit()
                            .withWeekendPolicy(false)
                            .withBusinessHoursOnly();
                    break;

                case CUSTOM:
                    // 사용자가 직접 사양들을 추가해야 함
                    break;
            }
        }

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

        // 개별 사양들의 실패 이유를 수집
        List<String> violations = new ArrayList<>();

        for (ReservationSpecification spec : individualSpecs) {
            if (!spec.isSatisfiedBy(context)) {
                String reason = getSpecificationFailureReason(spec, context);
                if (reason != null) {
                    violations.add(reason);
                }
            }
        }

        if (violations.isEmpty()) {
            return String.format("조건 미충족: %s", specification.getDescription());
        }

        return String.join(" | ", violations);
    }

    @Override
    public String getDescription() {
        return String.format("%s (%s) - %s", name, level.getDescription(), description);
    }

    /**
     * 정책 구성 요소 분석
     */
    public PolicyAnalysis analyze(ReservationContext context) {
        return new PolicyAnalysis(context, this);
    }

    /**
     * 정책 분석 결과 클래스
     */
    public static class PolicyAnalysis {
        private final boolean canReserve;
        private final PolicyLevel level;
        private final int totalSpecifications;
        private final int passedSpecifications;
        private final int failedSpecifications;
        private final List<String> passedChecks;
        private final List<String> failedChecks;
        private final Map<String, Object> metadata;

        private PolicyAnalysis(ReservationContext context, FlexibleReservationPolicy policy) {
            this.canReserve = policy.canReserve(context);
            this.level = policy.level;
            this.totalSpecifications = policy.individualSpecs.size();
            this.passedChecks = new ArrayList<>();
            this.failedChecks = new ArrayList<>();
            this.metadata = policy.metadata;

            // 각 사양별 통과/실패 분석
            for (ReservationSpecification spec : policy.individualSpecs) {
                String specName = getSpecificationName(spec);
                if (spec.isSatisfiedBy(context)) {
                    passedChecks.add(specName);
                } else {
                    failedChecks.add(specName);
                }
            }

            this.passedSpecifications = passedChecks.size();
            this.failedSpecifications = failedChecks.size();
        }

        private String getSpecificationName(ReservationSpecification spec) {
            String className = spec.getClass().getSimpleName();
            return className.replace("Specification", "").replaceAll("([a-z])([A-Z])", "$1 $2");
        }

        // Getter methods
        public boolean canReserve() { return canReserve; }
        public PolicyLevel getLevel() { return level; }
        public int getTotalSpecifications() { return totalSpecifications; }
        public int getPassedSpecifications() { return passedSpecifications; }
        public int getFailedSpecifications() { return failedSpecifications; }
        public List<String> getPassedChecks() { return List.copyOf(passedChecks); }
        public List<String> getFailedChecks() { return List.copyOf(failedChecks); }
        public Map<String, Object> getMetadata() { return Map.copyOf(metadata); }

        public double getSuccessRate() {
            return totalSpecifications > 0 ? (double) passedSpecifications / totalSpecifications : 0.0;
        }

        public String getSummary() {
            return String.format("정책 분석: %s (성공률: %.1f%%, 통과: %d/%d)",
                    canReserve ? "예약 가능" : "예약 불가",
                    getSuccessRate() * 100, passedSpecifications, totalSpecifications);
        }
    }

    /**
     * 정책 복제 및 수정
     */
    public FlexibleReservationPolicy withAdditionalSpec(ReservationSpecification additionalSpec) {
        return builder(this.name + " (수정됨)")
                .withDescription(this.description)
                .withLevel(PolicyLevel.CUSTOM)
                .withCustomSpecification(this.specification.and(additionalSpec))
                .build();
    }

    /**
     * 더 관대한 버전의 정책 생성
     */
    public FlexibleReservationPolicy toLenientVersion() {
        return builder(this.name + " (관대함)")
                .withDescription("원본 정책의 관대한 버전")
                .withLevel(this.level == PolicyLevel.STRICT ? PolicyLevel.STANDARD : PolicyLevel.BASIC)
                .build();
    }

    /**
     * 더 엄격한 버전의 정책 생성
     */
    public FlexibleReservationPolicy toStrictVersion() {
        return builder(this.name + " (엄격함)")
                .withDescription("원본 정책의 엄격한 버전")
                .withLevel(this.level == PolicyLevel.MINIMAL ? PolicyLevel.BASIC : PolicyLevel.STRICT)
                .build();
    }

    // =========================
    // 헬퍼 메서드들
    // =========================

    private String getSpecificationFailureReason(ReservationSpecification spec, ReservationContext context) {
        // 각 사양 타입에 따른 실패 이유 추출
        if (spec instanceof ActiveMemberSpecification) {
            return ((ActiveMemberSpecification) spec).getFailureReason(context);
        } else if (spec instanceof MembershipPrivilegeSpecification) {
            return ((MembershipPrivilegeSpecification) spec).getFailureReason(context);
        } else if (spec instanceof ResourceCapacitySpecification) {
            return ((ResourceCapacitySpecification) spec).getFailureReason(context);
        } else if (spec instanceof ValidReservationTimeSpecification) {
            return ((ValidReservationTimeSpecification) spec).getFailureReason(context);
        } else if (spec instanceof SimultaneousReservationLimitSpecification) {
            return ((SimultaneousReservationLimitSpecification) spec).getFailureReason(context);
        } else if (spec instanceof AdvanceReservationLimitSpecification) {
            return ((AdvanceReservationLimitSpecification) spec).getFailureReason(context);
        }

        return spec.getDescription() + " 조건 위반";
    }

    // =========================
    // Getter 메서드들
    // =========================

    public ReservationSpecification getSpecification() {
        return specification;
    }

    public String getName() {
        return name;
    }

    public PolicyLevel getLevel() {
        return level;
    }

    public Map<String, Object> getMetadata() {
        return Map.copyOf(metadata);
    }

    public List<ReservationSpecification> getIndividualSpecs() {
        return List.copyOf(individualSpecs);
    }

    public int getSpecificationCount() {
        return individualSpecs.size();
    }
}