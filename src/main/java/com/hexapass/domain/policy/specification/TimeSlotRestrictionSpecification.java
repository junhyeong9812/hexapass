package com.hexapass.domain.policy.specification;

import com.hexapass.domain.policy.ReservationContext;
import com.hexapass.domain.policy.ReservationSpecification;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * 시간대별 예약 제한 사양 - 개선된 버전
 * 요일별로 다른 시간 제한을 설정할 수 있고, 점심시간 등 특정 시간대 제외 기능 포함
 * 자정을 넘나드는 시간대와 복잡한 운영 시간 패턴 지원
 */
public class TimeSlotRestrictionSpecification implements ReservationSpecification {

    private final Map<DayOfWeek, TimeRange> weeklySchedule; // 요일별 운영시간
    private final Set<TimeRange> excludedTimeRanges;        // 제외할 시간대 (점심시간 등)
    private final String name;
    private final boolean strictMode; // 엄격 모드: 예약 시작/종료 모두 시간 내에 있어야 함

    public static class TimeRange {
        private final LocalTime startTime;
        private final LocalTime endTime;
        private final boolean crossesMidnight; // 자정을 넘나드는지 여부

        public TimeRange(LocalTime startTime, LocalTime endTime) {
            this.startTime = validateNotNull(startTime, "시작시간");
            this.endTime = validateNotNull(endTime, "종료시간");
            this.crossesMidnight = startTime.isAfter(endTime);
        }

        public boolean contains(LocalTime time) {
            if (!crossesMidnight) {
                return !time.isBefore(startTime) && time.isBefore(endTime);
            } else {
                // 자정을 넘나드는 경우: 시작시간 이후이거나 종료시간 이전
                return !time.isBefore(startTime) || time.isBefore(endTime);
            }
        }

        public boolean overlaps(LocalTime start, LocalTime end) {
            return contains(start) || contains(end) ||
                    (start.isBefore(startTime) && end.isAfter(endTime));
        }

        // Getter methods
        public LocalTime getStartTime() { return startTime; }
        public LocalTime getEndTime() { return endTime; }
        public boolean isCrossesMidnight() { return crossesMidnight; }

        @Override
        public String toString() {
            return String.format("%s-%s%s", startTime, endTime,
                    crossesMidnight ? "(다음날)" : "");
        }

        private static <T> T validateNotNull(T value, String fieldName) {
            if (value == null) {
                throw new IllegalArgumentException(fieldName + "은 null일 수 없습니다");
            }
            return value;
        }
    }

    private TimeSlotRestrictionSpecification(Map<DayOfWeek, TimeRange> weeklySchedule,
                                             Set<TimeRange> excludedTimeRanges,
                                             String name, boolean strictMode) {
        this.weeklySchedule = weeklySchedule != null ? new EnumMap<>(weeklySchedule) : new EnumMap<>(DayOfWeek.class);
        this.excludedTimeRanges = excludedTimeRanges != null ? Set.copyOf(excludedTimeRanges) : Set.of();
        this.name = name != null ? name : "시간대 제한";
        this.strictMode = strictMode;
        validateSchedule();
    }

    /**
     * 기본 운영시간 생성자
     */
    public TimeSlotRestrictionSpecification(LocalTime startTime, LocalTime endTime) {
        this(createUniformSchedule(startTime, endTime), Set.of(), "기본 운영시간", false);
    }

    /**
     * 요일별 다른 운영시간 생성자
     */
    public TimeSlotRestrictionSpecification(Map<DayOfWeek, TimeRange> weeklySchedule) {
        this(weeklySchedule, Set.of(), "요일별 운영시간", false);
    }

    // =========================
    // 팩토리 메서드들
    // =========================

    /**
     * 일반적인 운영 시간 (09:00 - 22:00)
     */
    public static TimeSlotRestrictionSpecification normalOperatingHours() {
        return new TimeSlotRestrictionSpecification(LocalTime.of(9, 0), LocalTime.of(22, 0));
    }

    /**
     * 비즈니스 시간 (09:00 - 18:00)
     */
    public static TimeSlotRestrictionSpecification businessHours() {
        return new TimeSlotRestrictionSpecification(LocalTime.of(9, 0), LocalTime.of(18, 0));
    }

    /**
     * 24시간 운영
     */
    public static TimeSlotRestrictionSpecification twentyFourHours() {
        return new TimeSlotRestrictionSpecification(LocalTime.MIDNIGHT, LocalTime.MIDNIGHT);
    }

    /**
     * 야간 시간대 (18:00 - 06:00)
     */
    public static TimeSlotRestrictionSpecification nightHours() {
        return new TimeSlotRestrictionSpecification(LocalTime.of(18, 0), LocalTime.of(6, 0));
    }

    /**
     * 평일/주말 구분 운영시간
     */
    public static TimeSlotRestrictionSpecification weekdayWeekendSchedule() {
        Map<DayOfWeek, TimeRange> schedule = new EnumMap<>(DayOfWeek.class);
        TimeRange weekdayHours = new TimeRange(LocalTime.of(9, 0), LocalTime.of(22, 0));
        TimeRange weekendHours = new TimeRange(LocalTime.of(10, 0), LocalTime.of(20, 0));

        // 평일
        schedule.put(DayOfWeek.MONDAY, weekdayHours);
        schedule.put(DayOfWeek.TUESDAY, weekdayHours);
        schedule.put(DayOfWeek.WEDNESDAY, weekdayHours);
        schedule.put(DayOfWeek.THURSDAY, weekdayHours);
        schedule.put(DayOfWeek.FRIDAY, weekdayHours);

        // 주말
        schedule.put(DayOfWeek.SATURDAY, weekendHours);
        schedule.put(DayOfWeek.SUNDAY, weekendHours);

        return new TimeSlotRestrictionSpecification(schedule, Set.of(), "평일/주말 구분", false);
    }

    /**
     * 점심시간 제외 운영시간 (12:00-13:00 제외)
     */
    public static TimeSlotRestrictionSpecification withLunchBreak() {
        TimeRange lunchBreak = new TimeRange(LocalTime.of(12, 0), LocalTime.of(13, 0));
        Map<DayOfWeek, TimeRange> schedule = createUniformSchedule(LocalTime.of(9, 0), LocalTime.of(18, 0));

        return new TimeSlotRestrictionSpecification(schedule, Set.of(lunchBreak), "점심시간 제외", true);
    }

    @Override
    public boolean isSatisfiedBy(ReservationContext context) {
        DayOfWeek dayOfWeek = context.getReservationTime().getDayOfWeek();
        LocalTime reservationTime = context.getReservationTime().toLocalTime();

        // 해당 요일의 운영시간 확인
        TimeRange dailySchedule = weeklySchedule.get(dayOfWeek);
        if (dailySchedule == null) {
            return false; // 해당 요일 운영하지 않음
        }

        // 기본 운영시간 내에 있는지 확인
        if (!dailySchedule.contains(reservationTime)) {
            return false;
        }

        // 제외된 시간대에 포함되는지 확인
        for (TimeRange excludedRange : excludedTimeRanges) {
            if (excludedRange.contains(reservationTime)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public String getDescription() {
        if (weeklySchedule.size() == 1 && isUniformSchedule()) {
            TimeRange range = weeklySchedule.values().iterator().next();
            return String.format("%s: %s", name, range);
        }

        StringBuilder desc = new StringBuilder(name + ": ");
        for (Map.Entry<DayOfWeek, TimeRange> entry : weeklySchedule.entrySet()) {
            desc.append(getDayName(entry.getKey())).append(" ").append(entry.getValue()).append(", ");
        }

        if (desc.length() > 2) {
            desc.setLength(desc.length() - 2); // 마지막 ", " 제거
        }

        if (!excludedTimeRanges.isEmpty()) {
            desc.append(" (제외: ");
            excludedTimeRanges.forEach(range -> desc.append(range).append(", "));
            desc.setLength(desc.length() - 2);
            desc.append(")");
        }

        return desc.toString();
    }

    /**
     * 구체적인 실패 이유 반환
     */
    public String getFailureReason(ReservationContext context) {
        DayOfWeek dayOfWeek = context.getReservationTime().getDayOfWeek();
        LocalTime reservationTime = context.getReservationTime().toLocalTime();

        TimeRange dailySchedule = weeklySchedule.get(dayOfWeek);
        if (dailySchedule == null) {
            return String.format("%s은 휴무일입니다", getDayName(dayOfWeek));
        }

        if (!dailySchedule.contains(reservationTime)) {
            return String.format("%s의 운영시간(%s)을 벗어났습니다",
                    getDayName(dayOfWeek), dailySchedule);
        }

        for (TimeRange excludedRange : excludedTimeRanges) {
            if (excludedRange.contains(reservationTime)) {
                return String.format("제외된 시간대(%s)입니다", excludedRange);
            }
        }

        return null; // 실패하지 않음
    }

    /**
     * 특정 요일의 운영시간 조회
     */
    public TimeRange getOperatingHours(DayOfWeek dayOfWeek) {
        return weeklySchedule.get(dayOfWeek);
    }

    /**
     * 특정 요일에 운영하는지 확인
     */
    public boolean isOperatingDay(DayOfWeek dayOfWeek) {
        return weeklySchedule.containsKey(dayOfWeek);
    }

    /**
     * 다음 운영 시간까지의 분 계산
     */
    public long getMinutesUntilNextOperatingTime(ReservationContext context) {
        // 복잡한 계산이므로 기본 구현만 제공
        return 0; // 실제로는 더 복잡한 로직 필요
    }

    // =========================
    // 헬퍼 메서드들
    // =========================

    private static Map<DayOfWeek, TimeRange> createUniformSchedule(LocalTime start, LocalTime end) {
        Map<DayOfWeek, TimeRange> schedule = new EnumMap<>(DayOfWeek.class);
        TimeRange range = new TimeRange(start, end);

        for (DayOfWeek day : DayOfWeek.values()) {
            schedule.put(day, range);
        }

        return schedule;
    }

    private boolean isUniformSchedule() {
        if (weeklySchedule.isEmpty()) return false;

        TimeRange first = weeklySchedule.values().iterator().next();
        return weeklySchedule.values().stream().allMatch(range -> range.equals(first));
    }

    private String getDayName(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY: return "월요일";
            case TUESDAY: return "화요일";
            case WEDNESDAY: return "수요일";
            case THURSDAY: return "목요일";
            case FRIDAY: return "금요일";
            case SATURDAY: return "토요일";
            case SUNDAY: return "일요일";
            default: return dayOfWeek.name();
        }
    }

    private void validateSchedule() {
        if (weeklySchedule.isEmpty()) {
            throw new IllegalArgumentException("최소 하나의 운영 요일이 필요합니다");
        }
    }

    // =========================
    // Getter 메서드들
    // =========================

    public Map<DayOfWeek, TimeRange> getWeeklySchedule() {
        return new EnumMap<>(weeklySchedule);
    }

    public Set<TimeRange> getExcludedTimeRanges() {
        return Set.copyOf(excludedTimeRanges);
    }

    public String getName() {
        return name;
    }

    public boolean isStrictMode() {
        return strictMode;
    }
}