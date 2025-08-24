package com.hexapass.domain.common;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * 구체적인 시작시간과 종료시간을 가지는 시간대를 나타내는 값 객체
 * 불변 객체로 설계되어 생성 후 상태 변경 불가
 * 같은 날짜 내의 시간대만 허용하며 시작시간 < 종료시간 보장
 */
public final class TimeSlot {

    private final LocalDateTime startTime;
    private final LocalDateTime endTime;

    private static final DateTimeFormatter DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * private 생성자 - 팩토리 메서드를 통해서만 생성 가능
     */
    private TimeSlot(LocalDateTime startTime, LocalDateTime endTime) {
        this.startTime = validateNotNull(startTime, "시작시간");
        this.endTime = validateNotNull(endTime, "종료시간");
        validateTimeOrder(startTime, endTime);
        validateSameDate(startTime, endTime);
    }

    /**
     * 시작시간과 종료시간을 지정하여 TimeSlot 생성
     */
    public static TimeSlot of(LocalDateTime startTime, LocalDateTime endTime) {
        return new TimeSlot(startTime, endTime);
    }

    /**
     * 시작시간과 지속시간(분)으로 TimeSlot 생성
     */
    public static TimeSlot ofDuration(LocalDateTime startTime, int durationMinutes) {
        if (startTime == null) {
            throw new IllegalArgumentException("시작시간은 null일 수 없습니다");
        }
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("지속시간은 0보다 커야 합니다. 입력값: " + durationMinutes);
        }

        LocalDateTime endTime = startTime.plusMinutes(durationMinutes);
        return new TimeSlot(startTime, endTime);
    }

    /**
     * 1시간 단위 TimeSlot 생성 편의 메서드
     */
    public static TimeSlot oneHour(LocalDateTime startTime) {
        return ofDuration(startTime, 60);
    }

    /**
     * 30분 단위 TimeSlot 생성 편의 메서드
     */
    public static TimeSlot halfHour(LocalDateTime startTime) {
        return ofDuration(startTime, 30);
    }

    // =========================
    // 시간대 관계 확인 메서드들
    // =========================

    /**
     * 다른 시간대와 겹치는지 확인
     */
    public boolean overlaps(TimeSlot other) {
        if (other == null) {
            return false;
        }

        // A.start < B.end && B.start < A.end 일 때 겹침
        return this.startTime.isBefore(other.endTime) &&
                other.startTime.isBefore(this.endTime);
    }

    /**
     * 다른 시간대와 인접한지 확인 (바로 이어지는지)
     */
    public boolean isAdjacent(TimeSlot other) {
        if (other == null) {
            return false;
        }

        // 이 시간대의 종료시간이 다른 시간대의 시작시간과 같거나
        // 다른 시간대의 종료시간이 이 시간대의 시작시간과 같음
        return this.endTime.equals(other.startTime) ||
                other.endTime.equals(this.startTime);
    }

    /**
     * 지정된 시간이 이 시간대에 포함되는지 확인 (시작시간 포함, 종료시간 미포함)
     */
    public boolean contains(LocalDateTime time) {
        if (time == null) {
            return false;
        }

        // [시작시간, 종료시간) 구간 - 시작시간 포함, 종료시간 미포함
        return !time.isBefore(startTime) && time.isBefore(endTime);
    }

    /**
     * 다른 시간대가 이 시간대에 완전히 포함되는지 확인
     */
    public boolean contains(TimeSlot other) {
        if (other == null) {
            return false;
        }

        return !other.startTime.isBefore(this.startTime) &&
                !other.endTime.isAfter(this.endTime);
    }

    /**
     * 이 시간대가 다른 시간대 이전인지 확인
     */
    public boolean isBefore(TimeSlot other) {
        return other != null && this.endTime.isBefore(other.startTime);
    }

    /**
     * 이 시간대가 다른 시간대 이후인지 확인
     */
    public boolean isAfter(TimeSlot other) {
        return other != null && this.startTime.isAfter(other.endTime);
    }

    // =========================
    // 시간대 정보 메서드들
    // =========================

    /**
     * 시간대의 지속시간 계산
     */
    public Duration getDuration() {
        return Duration.between(startTime, endTime);
    }

    /**
     * 지속시간을 분 단위로 반환
     */
    public long getDurationMinutes() {
        return getDuration().toMinutes();
    }

    /**
     * 지속시간을 시간 단위로 반환
     */
    public long getDurationHours() {
        return getDuration().toHours();
    }

    /**
     * 현재 시간 기준으로 이 시간대가 과거인지 확인
     */
    public boolean isPast() {
        return endTime.isBefore(LocalDateTime.now());
    }

    /**
     * 현재 시간 기준으로 이 시간대가 미래인지 확인
     */
    public boolean isFuture() {
        return startTime.isAfter(LocalDateTime.now());
    }

    /**
     * 현재 시간이 이 시간대에 포함되는지 확인 (현재 진행중)
     */
    public boolean isCurrent() {
        LocalDateTime now = LocalDateTime.now();
        return contains(now);
    }

    /**
     * 오늘의 시간대인지 확인
     */
    public boolean isToday() {
        return startTime.toLocalDate().equals(LocalDateTime.now().toLocalDate());
    }

    // =========================
    // 시간대 변환 메서드들
    // =========================

    /**
     * 시작시간을 변경한 새로운 TimeSlot 반환
     */
    public TimeSlot withStartTime(LocalDateTime newStartTime) {
        return new TimeSlot(newStartTime, this.endTime);
    }

    /**
     * 종료시간을 변경한 새로운 TimeSlot 반환
     */
    public TimeSlot withEndTime(LocalDateTime newEndTime) {
        return new TimeSlot(this.startTime, newEndTime);
    }

    /**
     * 지정된 분만큼 시간대를 이동한 새로운 TimeSlot 반환
     */
    public TimeSlot moveBy(int minutes) {
        LocalDateTime newStartTime = this.startTime.plusMinutes(minutes);
        LocalDateTime newEndTime = this.endTime.plusMinutes(minutes);
        return new TimeSlot(newStartTime, newEndTime);
    }

    /**
     * 지정된 분만큼 시간대를 연장한 새로운 TimeSlot 반환
     */
    public TimeSlot extend(int minutes) {
        if (minutes < 0) {
            throw new IllegalArgumentException("연장 시간은 0 이상이어야 합니다. 입력값: " + minutes);
        }

        LocalDateTime newEndTime = this.endTime.plusMinutes(minutes);
        return new TimeSlot(this.startTime, newEndTime);
    }

    /**
     * 지정된 분만큼 시간대를 단축한 새로운 TimeSlot 반환
     */
    public TimeSlot shorten(int minutes) {
        if (minutes < 0) {
            throw new IllegalArgumentException("단축 시간은 0 이상이어야 합니다. 입력값: " + minutes);
        }

        LocalDateTime newEndTime = this.endTime.minusMinutes(minutes);
        if (newEndTime.isBefore(this.startTime) || newEndTime.equals(this.startTime)) {
            throw new IllegalArgumentException("단축 후 종료시간이 시작시간보다 이전이거나 같을 수 없습니다");
        }

        return new TimeSlot(this.startTime, newEndTime);
    }

    // =========================
    // Object 메서드 오버라이드
    // =========================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        TimeSlot timeSlot = (TimeSlot) obj;
        return Objects.equals(startTime, timeSlot.startTime) &&
                Objects.equals(endTime, timeSlot.endTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startTime, endTime);
    }

    @Override
    public String toString() {
        return String.format("%s ~ %s (%d분)",
                startTime.format(DISPLAY_FORMATTER),
                endTime.format(DISPLAY_FORMATTER),
                getDurationMinutes());
    }

    // =========================
    // Getter 메서드들
    // =========================

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    // =========================
    // 검증 메서드들 (private)
    // =========================

    private LocalDateTime validateNotNull(LocalDateTime time, String fieldName) {
        if (time == null) {
            throw new IllegalArgumentException(fieldName + "은 null일 수 없습니다");
        }
        return time;
    }

    private void validateTimeOrder(LocalDateTime startTime, LocalDateTime endTime) {
        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException(
                    String.format("시작시간은 종료시간보다 이전이어야 합니다. (시작시간: %s, 종료시간: %s)",
                            startTime, endTime));
        }
    }

    private void validateSameDate(LocalDateTime startTime, LocalDateTime endTime) {
        if (!startTime.toLocalDate().equals(endTime.toLocalDate())) {
            throw new IllegalArgumentException(
                    String.format("시작시간과 종료시간은 같은 날짜여야 합니다. (시작일: %s, 종료일: %s)",
                            startTime.toLocalDate(), endTime.toLocalDate()));
        }
    }
}