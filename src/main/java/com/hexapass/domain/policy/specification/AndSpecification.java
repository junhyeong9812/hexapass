package com.hexapass.domain.policy.specification;

import com.hexapass.domain.policy.ReservationContext;
import com.hexapass.domain.policy.ReservationSpecification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * AND 조건 사양 - 개선된 버전
 * 두 개 이상의 사양이 모두 만족되어야 하는 조건을 표현
 * 실패한 조건들을 추적하여 더 자세한 피드백 제공
 */
public class AndSpecification implements ReservationSpecification {

    private final List<ReservationSpecification> specifications;

    public AndSpecification(ReservationSpecification left, ReservationSpecification right) {
        this.specifications = Arrays.asList(
                validateNotNull(left, "좌측 사양"),
                validateNotNull(right, "우측 사양")
        );
    }

    public AndSpecification(List<ReservationSpecification> specifications) {
        if (specifications == null || specifications.isEmpty()) {
            throw new IllegalArgumentException("사양 목록은 null이거나 빈 목록일 수 없습니다");
        }
        if (specifications.size() < 2) {
            throw new IllegalArgumentException("AND 조건은 최소 2개의 사양이 필요합니다");
        }

        this.specifications = new ArrayList<>();
        for (ReservationSpecification spec : specifications) {
            this.specifications.add(validateNotNull(spec, "사양"));
        }
    }

    /**
     * 여러 사양을 AND 조건으로 결합하는 편의 메서드
     */
    public static AndSpecification of(ReservationSpecification... specifications) {
        return new AndSpecification(Arrays.asList(specifications));
    }

    @Override
    public boolean isSatisfiedBy(ReservationContext context) {
        return specifications.stream().allMatch(spec -> spec.isSatisfiedBy(context));
    }

    @Override
    public String getDescription() {
        if (specifications.size() == 2) {
            return String.format("(%s) AND (%s)",
                    specifications.get(0).getDescription(),
                    specifications.get(1).getDescription());
        }

        StringBuilder desc = new StringBuilder();
        for (int i = 0; i < specifications.size(); i++) {
            if (i > 0) {
                desc.append(" AND ");
            }
            desc.append("(").append(specifications.get(i).getDescription()).append(")");
        }
        return desc.toString();
    }

    /**
     * 실패한 사양들의 목록을 반환
     */
    public List<ReservationSpecification> getFailedSpecifications(ReservationContext context) {
        List<ReservationSpecification> failed = new ArrayList<>();
        for (ReservationSpecification spec : specifications) {
            if (!spec.isSatisfiedBy(context)) {
                failed.add(spec);
            }
        }
        return failed;
    }

    /**
     * 실패한 사양들의 설명을 반환
     */
    public List<String> getFailedReasons(ReservationContext context) {
        List<String> reasons = new ArrayList<>();
        for (ReservationSpecification spec : specifications) {
            if (!spec.isSatisfiedBy(context)) {
                reasons.add(spec.getDescription());
            }
        }
        return reasons;
    }

    /**
     * 만족된 사양의 개수
     */
    public int getSatisfiedCount(ReservationContext context) {
        return (int) specifications.stream()
                .mapToLong(spec -> spec.isSatisfiedBy(context) ? 1 : 0)
                .sum();
    }

    /**
     * 전체 사양의 개수
     */
    public int getTotalCount() {
        return specifications.size();
    }

    // =========================
    // Getter 메서드들
    // =========================

    public List<ReservationSpecification> getSpecifications() {
        return new ArrayList<>(specifications); // 불변 복사본 반환
    }

    public ReservationSpecification getLeft() {
        return specifications.size() >= 2 ? specifications.get(0) : null;
    }

    public ReservationSpecification getRight() {
        return specifications.size() >= 2 ? specifications.get(1) : null;
    }

    // =========================
    // Object 메서드 오버라이드
    // =========================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        AndSpecification that = (AndSpecification) obj;
        return specifications.equals(that.specifications);
    }

    @Override
    public int hashCode() {
        return specifications.hashCode();
    }

    @Override
    public String toString() {
        return "AndSpecification{" + getDescription() + "}";
    }

    // =========================
    // 검증 메서드들 (private)
    // =========================

    private <T> T validateNotNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + "은 null일 수 없습니다");
        }
        return value;
    }
}