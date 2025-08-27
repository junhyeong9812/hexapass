package com.hexapass.domain.policy.specification;

import com.hexapass.domain.policy.ReservationContext;
import com.hexapass.domain.policy.ReservationSpecification;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 예약 시간 유효성 확인 사양 - 개선된 버전
 * 예약 시간이 현재 시간 이후이고 합리적인 범위 내에 있는지 확인
 * 시간 단위별 세밀한 제어와 비즈니스 시간 고려
 */
public class ValidReservationTimeSpecification implements ReservationSpecification {

    private final int maxAdvanceDays;           // 최대 선예약 일수
    private final int minAdvanceMinutes;        // 최소 선예약 분수
    private final boolean allowPastTime;        // 과거 시간 허용 (특별한 경우)
    private final boolean checkBusinessHours;   // 영업시간 확인 여부
    private final int businessStartHour;        // 영업 시작 시간 (24시간 형식)
    private final int businessEndHour;          // 영업 종료 시간 (24시간 형식)
    private final boolean allowWeekends;        // 주말 허용 여부

    public ValidReservationTimeSpecification() {
        this(365, 30, false, false, 0, 24, true);
    }

    public ValidReservationTimeSpecification(int maxAdvanceDays, int minAdvanceMinutes,
                                             boolean allowPastTime, boolean checkBusinessHours,
                                             int businessStartHour, int businessEndHour,
                                             boolean allowWeekends) {
        this.maxAdvanceDays = validateMaxAdvanceDays(maxAdvanceDays);
        this.minAdvanceMinutes = validateMinAdvanceMinutes(minAdvanceMinutes);
        this.allowPastTime = allowPastTime;
        this.checkBusinessHours = checkBusinessHours;
        this.businessStartHour = validateBusinessHour(businessStartHour, "영업 시작 시간");
        this.businessEndHour = validateBusinessHour(businessEndHour, "영업 종료 시간");
        this.allowWeekends = allowWeekends;
        validateBusinessHours(businessStartHour, businessEndHour);
    }

    /**
     * 표준 예약 시간 검증 (1년 이내, 30분 이후)
     */
    public static ValidReservationTimeSpecification standard() {
        return new ValidReservationTimeSpecification(365, 30, false, false, 0, 24, true);
    }

    /**
     * 엄격한 예약 시간 검증 (3개월 이내, 2시간 이후, 영업시간 확인)
     */
    public static ValidReservationTimeSpecification strict() {
        return new ValidReservationTimeSpecification(90, 120, false, true, 9, 21, false);
    }

    /**
     * 관대한 예약 시간 검증 (2년 이내, 즉시 가능)
     */
    public static ValidReservationTimeSpecification lenient() {
        return new ValidReservationTimeSpecification(730, 0, false, false, 0, 24, true);
    }

    /**
     * 비즈니스 시간 전용 (평일 9-18시)
     */
    public static ValidReservationTimeSpecification businessHours() {
        return new ValidReservationTimeSpecification(180, 60, false, true, 9, 18, false);
    }

    /**
     * 응급 상황용 (과거 시간 허용)
     */
    public static ValidReservationTimeSpecification emergency() {
        return new ValidReservationTimeSpecification(30, 0, true, false, 0, 24, true);
    }

    /**
     * 24시간 서비스용
     */
    public static ValidReservationTimeSpecification twentyFourSeven() {
        return new ValidReservationTimeSpecification(365, 15, false, false, 0, 24, true);
    }

    @Override
    public boolean isSatisfiedBy(ReservationContext context) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reservationTime = context.getReservationTime();

        // 과거 시간 확인
        if (!allowPastTime && reservationTime.isBefore(now)) {
            return false;
        }

        // 최소 선예약 시간 확인
        if (!allowPastTime) {
            long minutesUntil = ChronoUnit.MINUTES.between(now, reservationTime);
            if (minutesUntil < minAdvanceMinutes) {
                return false;
            }
        }

        // 최대 선예약 기간 확인
        if (reservationTime.isAfter(now.plusDays(maxAdvanceDays))) {
            return false;
        }

        // 영업시간 확인
        if (checkBusinessHours && !isWithinBusinessHours(reservationTime)) {
            return false;
        }

        // 주말 확인
        if (!allowWeekends && isWeekend(reservationTime)) {
            return false;
        }

        return true;
    }

    @Override
    public String getDescription() {
        StringBuilder desc = new StringBuilder("유효한 예약 시간");

        if (!allowPastTime) {
            if (minAdvanceMinutes > 0) {
                if (minAdvanceMinutes >= 60) {
                    desc.append(" (").append(minAdvanceMinutes / 60).append("시간 이후)");
                } else {
                    desc.append(" (").append(minAdvanceMinutes).append("분 이후)");
                }
            }
        } else {
            desc.append(" (과거 시간 허용)");
        }

        desc.append(" (최대 ").append(maxAdvanceDays).append("일 선예약)");

        if (checkBusinessHours) {
            desc.append(" (영업시간: ").append(businessStartHour).append("-").append(businessEndHour).append("시)");
        }

        if (!allowWeekends) {
            desc.append(" (평일만)");
        }

        return desc.toString();
    }

    /**
     * 구체적인 실패 이유 반환
     */
    public String getFailureReason(ReservationContext context) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reservationTime = context.getReservationTime();

        // 과거 시간 확인
        if (!allowPastTime && reservationTime.isBefore(now)) {
            long minutesAgo = ChronoUnit.MINUTES.between(reservationTime, now);
            return String.format("과거 시간으로는 예약할 수 없습니다 (%d분 전)", minutesAgo);
        }

        // 최소 선예약 시간 확인
        if (!allowPastTime) {
            long minutesUntil = ChronoUnit.MINUTES.between(now, reservationTime);
            if (minutesUntil < minAdvanceMinutes) {
                return String.format("예약은 최소 %d분 이후부터 가능합니다 (현재 %d분 후)",
                        minAdvanceMinutes, minutesUntil);
            }
        }

        // 최대 선예약 기간 확인
        if (reservationTime.isAfter(now.plusDays(maxAdvanceDays))) {
            long daysAfter = ChronoUnit.DAYS.between(now, reservationTime);
            return String.format("예약은 최대 %d일 후까지만 가능합니다 (요청: %d일 후)",
                    maxAdvanceDays, daysAfter);
        }

        // 영업시간 확인
        if (checkBusinessHours && !isWithinBusinessHours(reservationTime)) {
            int hour = reservationTime.getHour();
            return String.format("영업시간(%d시-%d시) 외의 시간입니다 (요청: %d시)",
                    businessStartHour, businessEndHour, hour);
        }

        // 주말 확인
        if (!allowWeekends && isWeekend(reservationTime)) {
            return "주말에는 예약할 수 없습니다";
        }

        return null; // 실패하지 않음
    }

    /**
     * 다음 가능한 예약 시간 제안
     */
    public LocalDateTime getNextAvailableTime(ReservationContext context) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime candidate = now.plusMinutes(minAdvanceMinutes);

        // 영업시간 조정
        if (checkBusinessHours) {
            candidate = adjustToBusinessHours(candidate);
        }

        // 주말 조정
        if (!allowWeekends) {
            candidate = adjustToWeekdays(candidate);
        }

        return candidate;
    }

    /**
     * 예약 가능한 시간대 정보
     */
    public String getAvailableTimeInfo() {
        StringBuilder info = new StringBuilder();

        if (!allowPastTime && minAdvanceMinutes > 0) {
            if (minAdvanceMinutes >= 60) {
                info.append("최소 ").append(minAdvanceMinutes / 60).append("시간 후부터 예약 가능\n");
            } else {
                info.append("최소 ").append(minAdvanceMinutes).append("분 후부터 예약 가능\n");
            }
        }

        info.append("최대 ").append(maxAdvanceDays).append("일 후까지 예약 가능\n");

        if (checkBusinessHours) {
            info.append("영업시간: ").append(businessStartHour).append("시 - ").append(businessEndHour).append("시\n");
        }

        if (!allowWeekends) {
            info.append("평일만 이용 가능");
        } else {
            info.append("연중무휴 이용 가능");
        }

        return info.toString().trim();
    }

    /**
     * 시간 유효성 상세 검사 결과
     */
    public TimeValidationResult validateTime(ReservationContext context) {
        return new TimeValidationResult(context, this);
    }

    /**
     * 시간 유효성 검사 결과 클래스
     */
    public static class TimeValidationResult {
        private final boolean isValid;
        private final String reason;
        private final LocalDateTime suggestedTime;

        private TimeValidationResult(ReservationContext context, ValidReservationTimeSpecification spec) {
            this.isValid = spec.isSatisfiedBy(context);
            this.reason = spec.getFailureReason(context);
            this.suggestedTime = spec.getNextAvailableTime(context);
        }

        public boolean isValid() { return isValid; }
        public String getReason() { return reason; }
        public LocalDateTime getSuggestedTime() { return suggestedTime; }
    }

    // =========================
    // 헬퍼 메서드들
    // =========================

    private boolean isWithinBusinessHours(LocalDateTime time) {
        int hour = time.getHour();

        if (businessStartHour <= businessEndHour) {
            // 일반적인 경우: 9시-18시
            return hour >= businessStartHour && hour < businessEndHour;
        } else {
            // 자정을 넘나드는 경우: 22시-6시
            return hour >= businessStartHour || hour < businessEndHour;
        }
    }

    private boolean isWeekend(LocalDateTime time) {
        java.time.DayOfWeek dayOfWeek = time.getDayOfWeek();
        return dayOfWeek == java.time.DayOfWeek.SATURDAY ||
                dayOfWeek == java.time.DayOfWeek.SUNDAY;
    }

    private LocalDateTime adjustToBusinessHours(LocalDateTime time) {
        if (!checkBusinessHours || isWithinBusinessHours(time)) {
            return time;
        }

        int hour = time.getHour();

        if (businessStartHour <= businessEndHour) {
            // 일반적인 경우
            if (hour < businessStartHour) {
                return time.withHour(businessStartHour).withMinute(0).withSecond(0);
            } else {
                // 영업 종료 후면 다음날 영업 시작 시간으로
                return time.plusDays(1).withHour(businessStartHour).withMinute(0).withSecond(0);
            }
        } else {
            // 자정을 넘나드는 경우 - 복잡한 로직 필요
            return time.withHour(businessStartHour).withMinute(0).withSecond(0);
        }
    }

    private LocalDateTime adjustToWeekdays(LocalDateTime time) {
        if (allowWeekends || !isWeekend(time)) {
            return time;
        }

        // 다음 월요일로 조정
        while (isWeekend(time)) {
            time = time.plusDays(1);
        }

        return time;
    }

    // =========================
    // 검증 메서드들
    // =========================

    private int validateMaxAdvanceDays(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("최대 선예약 일수는 0보다 커야 합니다: " + days);
        }
        return days;
    }

    private int validateMinAdvanceMinutes(int minutes) {
        if (minutes < 0) {
            throw new IllegalArgumentException("최소 선예약 분수는 0 이상이어야 합니다: " + minutes);
        }
        return minutes;
    }

    private int validateBusinessHour(int hour, String fieldName) {
        if (hour < 0 || hour > 24) {
            throw new IllegalArgumentException(fieldName + "은 0-24 사이여야 합니다: " + hour);
        }
        return hour;
    }

    private void validateBusinessHours(int startHour, int endHour) {
        if (startHour == endHour) {
            throw new IllegalArgumentException("영업 시작 시간과 종료 시간이 같을 수 없습니다");
        }
    }

    // =========================
    // Getter 메서드들
    // =========================

    public int getMaxAdvanceDays() {
        return maxAdvanceDays;
    }

    public int getMinAdvanceMinutes() {
        return minAdvanceMinutes;
    }

    public boolean isAllowPastTime() {
        return allowPastTime;
    }

    public boolean isCheckBusinessHours() {
        return checkBusinessHours;
    }

    public int getBusinessStartHour() {
        return businessStartHour;
    }

    public int getBusinessEndHour() {
        return businessEndHour;
    }

    public boolean isAllowWeekends() {
        return allowWeekends;
    }
}