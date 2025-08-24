package com.hexapass.domain.common;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * 시작일과 종료일을 포함하는 날짜 범위를 나타내는 값 객체
 * 불변 객체로 설계되어 생성 후 상태 변경 불가
 * 양 끝 날짜를 모두 포함하는 구간으로 정의
 */
public final class DateRange {

    private final LocalDate startDate;
    private final LocalDate endDate;

    /**
     * private 생성자 - 팩토리 메서드를 통해서만 생성 가능
     */
    private DateRange(LocalDate startDate, LocalDate endDate) {
        this.startDate = validateNotNull(startDate, "시작일");
        this.endDate = validateNotNull(endDate, "종료일");
        validateDateOrder(startDate, endDate);
    }

    /**
     * 시작일과 종료일을 지정하여 DateRange 생성
     */
    public static DateRange of(LocalDate startDate, LocalDate endDate) {
        return new DateRange(startDate, endDate);
    }

    /**
     * 단일 날짜로 DateRange 생성 (시작일 = 종료일)
     */
    public static DateRange singleDay(LocalDate date) {
        return new DateRange(date, date);
    }

    /**
     * 오늘부터 지정된 일수만큼의 DateRange 생성
     */
    public static DateRange fromTodayFor(int days) {
        if (days < 1) {
            throw new IllegalArgumentException("일수는 1 이상이어야 합니다. 입력값: " + days);
        }

        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(days - 1); // days일 포함하려면 -1
        return new DateRange(today, endDate);
    }

    /**
     * 지정된 날짜부터 일정 기간의 DateRange 생성
     */
    public static DateRange fromDateFor(LocalDate startDate, int days) {
        if (startDate == null) {
            throw new IllegalArgumentException("시작일은 null일 수 없습니다");
        }
        if (days < 1) {
            throw new IllegalArgumentException("일수는 1 이상이어야 합니다. 입력값: " + days);
        }

        LocalDate endDate = startDate.plusDays(days - 1);
        return new DateRange(startDate, endDate);
    }

    // =========================
    // 날짜 범위 연산 메서드들
    // =========================

    /**
     * 다른 날짜 범위와 겹치는지 확인
     */
    public boolean overlaps(DateRange other) {
        if (other == null) {
            return false;
        }

        // A.end >= B.start && B.end >= A.start 일 때 겹침
        return !this.endDate.isBefore(other.startDate) &&
                !other.endDate.isBefore(this.startDate);
    }

    /**
     * 지정된 날짜가 이 범위에 포함되는지 확인 (양 끝 포함)
     */
    public boolean contains(LocalDate date) {
        if (date == null) {
            return false;
        }

        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /**
     * 다른 날짜 범위가 이 범위에 완전히 포함되는지 확인
     */
    public boolean contains(DateRange other) {
        if (other == null) {
            return false;
        }

        return !other.startDate.isBefore(this.startDate) &&
                !other.endDate.isAfter(this.endDate);
    }

    /**
     * 이 날짜 범위가 다른 범위에 완전히 포함되는지 확인
     */
    public boolean isContainedBy(DateRange other) {
        return other != null && other.contains(this);
    }

    /**
     * 다른 날짜 범위와 인접한지 확인 (바로 이어지는지)
     */
    public boolean isAdjacentTo(DateRange other) {
        if (other == null) {
            return false;
        }

        // 이 범위의 다음날이 다른 범위의 시작일이거나, 다른 범위의 다음날이 이 범위의 시작일
        return this.endDate.plusDays(1).equals(other.startDate) ||
                other.endDate.plusDays(1).equals(this.startDate);
    }

    // =========================
    // 날짜 범위 정보 메서드들
    // =========================

    /**
     * 날짜 범위의 일수 계산 (양 끝 포함)
     */
    public long getDays() {
        return ChronoUnit.DAYS.between(startDate, endDate) + 1; // 양끝 포함이므로 +1
    }

    /**
     * 현재 날짜 기준으로 이 범위가 과거인지 확인
     */
    public boolean isPast() {
        return endDate.isBefore(LocalDate.now());
    }

    /**
     * 현재 날짜 기준으로 이 범위가 미래인지 확인
     */
    public boolean isFuture() {
        return startDate.isAfter(LocalDate.now());
    }

    /**
     * 현재 날짜가 이 범위에 포함되는지 확인 (현재 진행중)
     */
    public boolean isCurrent() {
        return contains(LocalDate.now());
    }

    /**
     * 단일 날짜 범위인지 확인 (시작일 = 종료일)
     */
    public boolean isSingleDay() {
        return startDate.equals(endDate);
    }

    // =========================
    // 날짜 범위 변환 메서드들
    // =========================

    /**
     * 시작일을 변경한 새로운 DateRange 반환
     */
    public DateRange withStartDate(LocalDate newStartDate) {
        return new DateRange(newStartDate, this.endDate);
    }

    /**
     * 종료일을 변경한 새로운 DateRange 반환
     */
    public DateRange withEndDate(LocalDate newEndDate) {
        return new DateRange(this.startDate, newEndDate);
    }

    /**
     * 지정된 일수만큼 연장한 새로운 DateRange 반환
     */
    public DateRange extend(int days) {
        if (days < 0) {
            throw new IllegalArgumentException("연장 일수는 0 이상이어야 합니다. 입력값: " + days);
        }

        return new DateRange(this.startDate, this.endDate.plusDays(days));
    }

    /**
     * 지정된 일수만큼 단축한 새로운 DateRange 반환
     */
    public DateRange shorten(int days) {
        if (days < 0) {
            throw new IllegalArgumentException("단축 일수는 0 이상이어야 합니다. 입력값: " + days);
        }

        LocalDate newEndDate = this.endDate.minusDays(days);
        if (newEndDate.isBefore(this.startDate)) {
            throw new IllegalArgumentException("단축 후 종료일이 시작일보다 이전이 될 수 없습니다");
        }

        return new DateRange(this.startDate, newEndDate);
    }

    // =========================
    // Object 메서드 오버라이드
    // =========================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        DateRange dateRange = (DateRange) obj;
        return Objects.equals(startDate, dateRange.startDate) &&
                Objects.equals(endDate, dateRange.endDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startDate, endDate);
    }

    @Override
    public String toString() {
        if (isSingleDay()) {
            return startDate.toString();
        }
        return String.format("%s ~ %s (%d일)", startDate, endDate, getDays());
    }

    // =========================
    // Getter 메서드들
    // =========================

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    // =========================
    // 검증 메서드들 (private)
    // =========================

    private LocalDate validateNotNull(LocalDate date, String fieldName) {
        if (date == null) {
            throw new IllegalArgumentException(fieldName + "은 null일 수 없습니다");
        }
        return date;
    }

    private void validateDateOrder(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    String.format("시작일은 종료일보다 이전이거나 같아야 합니다. (시작일: %s, 종료일: %s)",
                            startDate, endDate));
        }
    }
}