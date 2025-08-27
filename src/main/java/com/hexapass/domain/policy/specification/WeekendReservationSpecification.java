package com.hexapass.domain.policy.specification;

import com.hexapass.domain.policy.ReservationContext;
import com.hexapass.domain.policy.ReservationSpecification;
import com.hexapass.domain.type.ResourceType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/**
 * 주말 예약 제한 사양 - 개선된 버전
 * 주말 예약 허용 여부를 확인하고, 리소스 타입별로 다른 주말 정책 적용
 * 공휴일 및 특별 운영일 고려
 */
public class WeekendReservationSpecification implements ReservationSpecification {

    private final boolean allowWeekendReservation;
    private final boolean allowSaturdayOnly;           // 토요일만 허용 (일요일 제외)
    private final boolean allowSundayOnly;             // 일요일만 허용 (토요일 제외)
    private final Set<ResourceType> weekendOnlyResources;    // 주말에만 이용 가능한 리소스들
    private final Set<ResourceType> weekdayOnlyResources;    // 평일에만 이용 가능한 리소스들
    private final Set<LocalDate> specialOperatingDays;      // 특별 운영일 (평일이지만 주말 정책 적용)
    private final Set<LocalDate> specialClosedDays;         // 특별 휴무일 (주말이지만 운영 안함)

    public WeekendReservationSpecification(boolean allowWeekendReservation) {
        this(allowWeekendReservation, false, false, null, null, null, null);
    }

    public WeekendReservationSpecification(boolean allowWeekendReservation, boolean allowSaturdayOnly,
                                           boolean allowSundayOnly, Set<ResourceType> weekendOnlyResources,
                                           Set<ResourceType> weekdayOnlyResources,
                                           Set<LocalDate> specialOperatingDays,
                                           Set<LocalDate> specialClosedDays) {
        this.allowWeekendReservation = allowWeekendReservation;
        this.allowSaturdayOnly = allowSaturdayOnly;
        this.allowSundayOnly = allowSundayOnly;
        this.weekendOnlyResources = weekendOnlyResources != null ? Set.copyOf(weekendOnlyResources) : Set.of();
        this.weekdayOnlyResources = weekdayOnlyResources != null ? Set.copyOf(weekdayOnlyResources) : Set.of();
        this.specialOperatingDays = specialOperatingDays != null ? Set.copyOf(specialOperatingDays) : Set.of();
        this.specialClosedDays = specialClosedDays != null ? Set.copyOf(specialClosedDays) : Set.of();

        validateConfiguration();
    }

    /**
     * 주말 예약을 허용하는 사양
     */
    public static WeekendReservationSpecification allowWeekend() {
        return new WeekendReservationSpecification(true);
    }

    /**
     * 주말 예약을 제한하는 사양
     */
    public static WeekendReservationSpecification restrictWeekend() {
        return new WeekendReservationSpecification(false);
    }

    /**
     * 토요일만 허용하는 사양
     */
    public static WeekendReservationSpecification saturdayOnly() {
        return new WeekendReservationSpecification(false, true, false, null, null, null, null);
    }

    /**
     * 일요일만 허용하는 사양
     */
    public static WeekendReservationSpecification sundayOnly() {
        return new WeekendReservationSpecification(false, false, true, null, null, null, null);
    }

    /**
     * 리소스별 차등 주말 정책
     */
    public static WeekendReservationSpecification withResourceBasedPolicy(
            Set<ResourceType> weekendOnlyResources, Set<ResourceType> weekdayOnlyResources) {
        return new WeekendReservationSpecification(true, false, false,
                weekendOnlyResources, weekdayOnlyResources, null, null);
    }

    /**
     * 피트니스 시설 특화 정책 (헬스장은 평일만, 수영장은 주말에도 가능)
     */
    public static WeekendReservationSpecification fitnessPolicy() {
        Set<ResourceType> weekdayOnly = Set.of(ResourceType.GYM);
        return new WeekendReservationSpecification(false, false, false,
                null, weekdayOnly, null, null);
    }

    /**
     * 특별 운영일을 포함한 복합 정책
     */
    public static WeekendReservationSpecification withSpecialDays(
            boolean allowWeekend, Set<LocalDate> specialOperating, Set<LocalDate> specialClosed) {
        return new WeekendReservationSpecification(allowWeekend, false, false,
                null, null, specialOperating, specialClosed);
    }

    @Override
    public boolean isSatisfiedBy(ReservationContext context) {
        LocalDate reservationDate = context.getReservationDate();
        ResourceType resourceType = context.getResourceType();
        DayOfWeek dayOfWeek = reservationDate.getDayOfWeek();

        // 특별 휴무일 확인
        if (specialClosedDays.contains(reservationDate)) {
            return false;
        }

        // 특별 운영일 확인 (평일이지만 주말 정책 적용)
        if (specialOperatingDays.contains(reservationDate)) {
            return handleWeekendPolicy(dayOfWeek, resourceType, true);
        }

        // 평일인 경우
        if (!isWeekend(dayOfWeek)) {
            return handleWeekdayPolicy(resourceType);
        }

        // 주말인 경우
        return handleWeekendPolicy(dayOfWeek, resourceType, false);
    }

    @Override
    public String getDescription() {
        StringBuilder desc = new StringBuilder();

        if (allowWeekendReservation) {
            desc.append("주말 예약 허용");
        } else if (allowSaturdayOnly) {
            desc.append("토요일만 예약 허용");
        } else if (allowSundayOnly) {
            desc.append("일요일만 예약 허용");
        } else {
            desc.append("주말 예약 제한");
        }

        if (!weekendOnlyResources.isEmpty()) {
            desc.append(" (주말 전용: ").append(formatResourceTypes(weekendOnlyResources)).append(")");
        }

        if (!weekdayOnlyResources.isEmpty()) {
            desc.append(" (평일 전용: ").append(formatResourceTypes(weekdayOnlyResources)).append(")");
        }

        if (!specialOperatingDays.isEmpty()) {
            desc.append(" (특별 운영일 ").append(specialOperatingDays.size()).append("일)");
        }

        if (!specialClosedDays.isEmpty()) {
            desc.append(" (특별 휴무일 ").append(specialClosedDays.size()).append("일)");
        }

        return desc.toString();
    }

    /**
     * 구체적인 실패 이유 반환
     */
    public String getFailureReason(ReservationContext context) {
        LocalDate reservationDate = context.getReservationDate();
        ResourceType resourceType = context.getResourceType();
        DayOfWeek dayOfWeek = reservationDate.getDayOfWeek();

        // 특별 휴무일
        if (specialClosedDays.contains(reservationDate)) {
            return String.format("%s는 특별 휴무일입니다", reservationDate);
        }

        // 특별 운영일
        if (specialOperatingDays.contains(reservationDate)) {
            if (!handleWeekendPolicy(dayOfWeek, resourceType, true)) {
                return String.format("특별 운영일이지만 '%s' 이용이 제한됩니다",
                        resourceType.getDisplayName());
            }
        }

        // 평일 정책 위반
        if (!isWeekend(dayOfWeek) && !handleWeekdayPolicy(resourceType)) {
            if (weekendOnlyResources.contains(resourceType)) {
                return String.format("'%s'는 주말에만 이용 가능합니다", resourceType.getDisplayName());
            }
        }

        // 주말 정책 위반
        if (isWeekend(dayOfWeek)) {
            if (!allowWeekendReservation) {
                if (weekdayOnlyResources.contains(resourceType)) {
                    return String.format("'%s'는 평일에만 이용 가능합니다", resourceType.getDisplayName());
                } else if (allowSaturdayOnly && dayOfWeek == DayOfWeek.SUNDAY) {
                    return "일요일 예약은 허용되지 않습니다";
                } else if (allowSundayOnly && dayOfWeek == DayOfWeek.SATURDAY) {
                    return "토요일 예약은 허용되지 않습니다";
                } else {
                    return String.format("%s 예약은 허용되지 않습니다", getDayName(dayOfWeek));
                }
            }
        }

        return null; // 실패하지 않음
    }

    /**
     * 주말 운영 현황 정보
     */
    public String getWeekendOperatingInfo() {
        StringBuilder info = new StringBuilder();

        if (allowWeekendReservation) {
            info.append("주말 운영: 토요일, 일요일 모두 이용 가능\n");
        } else if (allowSaturdayOnly) {
            info.append("주말 운영: 토요일만 이용 가능\n");
        } else if (allowSundayOnly) {
            info.append("주말 운영: 일요일만 이용 가능\n");
        } else {
            info.append("주말 운영: 휴무\n");
        }

        if (!weekendOnlyResources.isEmpty()) {
            info.append("주말 전용 시설: ").append(formatResourceTypes(weekendOnlyResources)).append("\n");
        }

        if (!weekdayOnlyResources.isEmpty()) {
            info.append("평일 전용 시설: ").append(formatResourceTypes(weekdayOnlyResources)).append("\n");
        }

        return info.toString().trim();
    }

    /**
     * 특정 날짜의 운영 상태 확인
     */
    public OperatingStatus getOperatingStatus(LocalDate date, ResourceType resourceType) {
        if (specialClosedDays.contains(date)) {
            return OperatingStatus.CLOSED;
        }

        if (specialOperatingDays.contains(date)) {
            return OperatingStatus.SPECIAL_OPERATING;
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();

        if (!isWeekend(dayOfWeek)) {
            // 평일
            if (weekendOnlyResources.contains(resourceType)) {
                return OperatingStatus.RESOURCE_RESTRICTED;
            }
            return OperatingStatus.NORMAL_OPERATING;
        } else {
            // 주말
            if (weekdayOnlyResources.contains(resourceType)) {
                return OperatingStatus.RESOURCE_RESTRICTED;
            }

            if (!allowWeekendReservation) {
                if (allowSaturdayOnly && dayOfWeek == DayOfWeek.SATURDAY) {
                    return OperatingStatus.NORMAL_OPERATING;
                } else if (allowSundayOnly && dayOfWeek == DayOfWeek.SUNDAY) {
                    return OperatingStatus.NORMAL_OPERATING;
                } else {
                    return OperatingStatus.CLOSED;
                }
            }

            return OperatingStatus.NORMAL_OPERATING;
        }
    }

    /**
     * 운영 상태 열거형
     */
    public enum OperatingStatus {
        NORMAL_OPERATING("정상 운영"),
        CLOSED("휴무"),
        SPECIAL_OPERATING("특별 운영"),
        RESOURCE_RESTRICTED("시설 제한");

        private final String description;

        OperatingStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 다음 이용 가능한 날짜 제안
     */
    public LocalDate getNextAvailableDate(ReservationContext context) {
        LocalDate current = context.getReservationDate();
        ResourceType resourceType = context.getResourceType();

        // 최대 30일까지만 검색
        for (int i = 0; i < 30; i++) {
            LocalDate candidate = current.plusDays(i);

            // 임시 컨텍스트로 확인
            ReservationContext tempContext = ReservationContext.create(
                    context.getMember(), context.getResourceId(), resourceType,
                    candidate.atTime(context.getReservationTime().toLocalTime()),
                    context.getCurrentActiveReservations(),
                    context.getResourceCurrentOccupancy(),
                    context.getResourceCapacity()
            );

            if (isSatisfiedBy(tempContext)) {
                return candidate;
            }
        }

        return null; // 30일 내에 이용 가능한 날이 없음
    }

    // =========================
    // 헬퍼 메서드들
    // =========================

    private boolean isWeekend(DayOfWeek dayOfWeek) {
        return dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY;
    }

    private boolean handleWeekdayPolicy(ResourceType resourceType) {
        // 주말 전용 리소스인 경우 평일에는 사용 불가
        return !weekendOnlyResources.contains(resourceType);
    }

    private boolean handleWeekendPolicy(DayOfWeek dayOfWeek, ResourceType resourceType, boolean isSpecialDay) {
        // 평일 전용 리소스인 경우 주말에는 사용 불가
        if (weekdayOnlyResources.contains(resourceType)) {
            return false;
        }

        // 기본 주말 정책 적용
        if (allowWeekendReservation) {
            return true;
        }

        if (allowSaturdayOnly && dayOfWeek == DayOfWeek.SATURDAY) {
            return true;
        }

        if (allowSundayOnly && dayOfWeek == DayOfWeek.SUNDAY) {
            return true;
        }

        return false;
    }

    private String formatResourceTypes(Set<ResourceType> types) {
        return types.stream()
                .map(ResourceType::getDisplayName)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private String getDayName(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case SATURDAY: return "토요일";
            case SUNDAY: return "일요일";
            default: return dayOfWeek.name();
        }
    }

    private void validateConfiguration() {
        if (allowSaturdayOnly && allowSundayOnly) {
            throw new IllegalArgumentException("토요일만 허용과 일요일만 허용을 동시에 설정할 수 없습니다");
        }

        if (allowWeekendReservation && (allowSaturdayOnly || allowSundayOnly)) {
            throw new IllegalArgumentException("전체 주말 허용과 부분 주말 허용을 동시에 설정할 수 없습니다");
        }
    }

    // =========================
    // Getter 메서드들
    // =========================

    public boolean isWeekendAllowed() {
        return allowWeekendReservation;
    }

    public boolean isSaturdayOnly() {
        return allowSaturdayOnly;
    }

    public boolean isSundayOnly() {
        return allowSundayOnly;
    }

    public Set<ResourceType> getWeekendOnlyResources() {
        return Set.copyOf(weekendOnlyResources);
    }

    public Set<ResourceType> getWeekdayOnlyResources() {
        return Set.copyOf(weekdayOnlyResources);
    }

    public Set<LocalDate> getSpecialOperatingDays() {
        return Set.copyOf(specialOperatingDays);
    }

    public Set<LocalDate> getSpecialClosedDays() {
        return Set.copyOf(specialClosedDays);
    }

    // =========================
    // Object 메서드 오버라이드
    // =========================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        WeekendReservationSpecification that = (WeekendReservationSpecification) obj;
        return allowWeekendReservation == that.allowWeekendReservation &&
                allowSaturdayOnly == that.allowSaturdayOnly &&
                allowSundayOnly == that.allowSundayOnly &&
                java.util.Objects.equals(weekendOnlyResources, that.weekendOnlyResources) &&
                java.util.Objects.equals(weekdayOnlyResources, that.weekdayOnlyResources) &&
                java.util.Objects.equals(specialOperatingDays, that.specialOperatingDays) &&
                java.util.Objects.equals(specialClosedDays, that.specialClosedDays);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(allowWeekendReservation, allowSaturdayOnly, allowSundayOnly,
                weekendOnlyResources, weekdayOnlyResources, specialOperatingDays, specialClosedDays);
    }

    @Override
    public String toString() {
        return "WeekendReservationSpecification{" +
                "allowWeekendReservation=" + allowWeekendReservation +
                ", allowSaturdayOnly=" + allowSaturdayOnly +
                ", allowSundayOnly=" + allowSundayOnly +
                ", weekendOnlyResources=" + weekendOnlyResources.size() +
                ", weekdayOnlyResources=" + weekdayOnlyResources.size() +
                ", specialOperatingDays=" + specialOperatingDays.size() +
                ", specialClosedDays=" + specialClosedDays.size() +
                '}';
    }
}