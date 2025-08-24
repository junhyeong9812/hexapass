package com.hexapass.domain.policy.specification;

import com.hexapass.domain.policy.ReservationContext;
import com.hexapass.domain.policy.ReservationSpecification;

/**
 * 시간대별 예약 제한 사양
 * 특정 시간대에만 예약을 허용하는지 확인
 */
public class TimeSlotRestrictionSpecification implements ReservationSpecification {

    private final int earliestHour; // 가장 이른 시간 (24시간 형식)
    private final int latestHour;   // 가장 늦은 시간 (24시간 형식)

    public TimeSlotRestrictionSpecification(int earliestHour, int latestHour) {
        validateTimeRange(earliestHour, latestHour);
        this.earliestHour = earliestHour;
        this.latestHour = latestHour;
    }

    /**
     * 일반적인 운영 시간 (09:00 - 22:00)
     */
    public static TimeSlotRestrictionSpecification normalOperatingHours() {
        return new TimeSlotRestrictionSpecification(9, 22);
    }

    /**
     * 비즈니스 시간 (09:00 - 18:00)
     */
    public static TimeSlotRestrictionSpecification businessHours() {
        return new TimeSlotRestrictionSpecification(9, 18);
    }

    /**
     * 24시간 운영
     */
    public static TimeSlotRestrictionSpecification twentyFourHours() {
        return new TimeSlotRestrictionSpecification(0, 24);
    }

    /**
     * 야간 시간대 (18:00 - 06:00)
     */
    public static TimeSlotRestrictionSpecification nightHours() {
        return new TimeSlotRestrictionSpecification(18, 30); // 30은 다음날 6시를 의미 (24+6)
    }

    @Override
    public boolean isSatisfiedBy(ReservationContext context) {
        int reservationHour = context.getReservationTime().getHour();

        // 일반적인 경우 (earliestHour < latestHour)
        if (latestHour <= 24) {
            return reservationHour >= earliestHour && reservationHour < latestHour;
        }
        // 자정을 넘나드는 경우 (예: 18:00 - 06:00)
        else {
            return reservationHour >= earliestHour || reservationHour < (latestHour - 24);
        }
    }

    @Override
    public String getDescription() {
        if (latestHour <= 24) {
            return String.format("예약 가능 시간: %02d:00 ~ %02d:00", earliestHour, latestHour);
        } else {
            return String.format("예약 가능 시간: %02d:00 ~ %02d:00 (다음날)",
                    earliestHour, latestHour - 24);
        }
    }

    // =========================
    // Getter 메서드들
    // =========================

    public int getEarliestHour() {
        return earliestHour;
    }

    public int getLatestHour() {
        return latestHour;
    }

    // =========================
    // 검증 메서드들 (private)
    // =========================

    private void validateTimeRange(int earliestHour, int latestHour) {
        if (earliestHour < 0 || earliestHour > 23) {
            throw new IllegalArgumentException("시작 시간은 0-23 사이여야 합니다: " + earliestHour);
        }
        if (latestHour < 1 || latestHour > 30) { // 30은 다음날 6시까지 허용
            throw new IllegalArgumentException("종료 시간은 1-30 사이여야 합니다: " + latestHour);
        }
        if (earliestHour == latestHour) {
            throw new IllegalArgumentException("시작 시간과 종료 시간이 같을 수 없습니다");
        }
    }
}