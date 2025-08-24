package com.hexapass.domain.policy.specification;

import com.hexapass.domain.policy.ReservationContext;
import com.hexapass.domain.policy.ReservationSpecification;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * 주말 예약 제한 사양
 * 주말 예약 허용 여부를 확인
 */
public class WeekendReservationSpecification implements ReservationSpecification {

    private final boolean allowWeekendReservation;

    public WeekendReservationSpecification(boolean allowWeekendReservation) {
        this.allowWeekendReservation = allowWeekendReservation;
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

    @Override
    public boolean isSatisfiedBy(ReservationContext context) {
        if (allowWeekendReservation) {
            return true; // 주말 예약 허용
        }

        LocalDate reservationDate = context.getReservationDate();
        DayOfWeek dayOfWeek = reservationDate.getDayOfWeek();

        // 토요일(6)과 일요일(7)이 아닌 경우만 허용
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    @Override
    public String getDescription() {
        return allowWeekendReservation ? "주말 예약 허용" : "주말 예약 제한";
    }

    public boolean isWeekendAllowed() {
        return allowWeekendReservation;
    }
}