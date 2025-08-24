package com.hexapass.domain.policy.specification;

import com.hexapass.domain.policy.ReservationContext;
import com.hexapass.domain.policy.ReservationSpecification;

import java.time.LocalDateTime;

/**
 * 예약 시간 유효성 확인 사양
 * 예약 시간이 현재 시간 이후이고 합리적인 범위 내에 있는지 확인
 */
public class ValidReservationTimeSpecification implements ReservationSpecification {

    private final int maxAdvanceDays; // 최대 선예약 일수

    public ValidReservationTimeSpecification() {
        this(365); // 기본값: 1년
    }

    public ValidReservationTimeSpecification(int maxAdvanceDays) {
        if (maxAdvanceDays <= 0) {
            throw new IllegalArgumentException("최대 선예약 일수는 0보다 커야 합니다: " + maxAdvanceDays);
        }
        this.maxAdvanceDays = maxAdvanceDays;
    }

    @Override
    public boolean isSatisfiedBy(ReservationContext context) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reservationTime = context.getReservationTime();

        // 현재 시간 이후이고, 최대 선예약 기간 이내여야 함
        return reservationTime.isAfter(now) &&
                reservationTime.isBefore(now.plusDays(maxAdvanceDays));
    }

    @Override
    public String getDescription() {
        return String.format("유효한 예약 시간 (최대 %d일 선예약)", maxAdvanceDays);
    }

    public int getMaxAdvanceDays() {
        return maxAdvanceDays;
    }
}