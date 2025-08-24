package com.hexapass.domain.policy;

import com.hexapass.domain.common.Money;
import com.hexapass.domain.model.Member;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 취소 컨텍스트 정보
 * 취소 정책 적용에 필요한 모든 정보를 담고 있는 객체
 */
public class CancellationContext {

    private final LocalDateTime reservationTime;
    private final LocalDateTime cancellationTime;
    private final Money originalPrice;
    private final Member member;
    private final boolean isFirstTimeCancellation;

    private CancellationContext(LocalDateTime reservationTime, LocalDateTime cancellationTime,
                                Money originalPrice, Member member, boolean isFirstTimeCancellation) {
        this.reservationTime = validateNotNull(reservationTime, "예약 시간");
        this.cancellationTime = validateNotNull(cancellationTime, "취소 시간");
        this.originalPrice = validateNotNull(originalPrice, "원 가격");
        this.member = member;
        this.isFirstTimeCancellation = isFirstTimeCancellation;
    }

    /**
     * 취소 컨텍스트 생성
     */
    public static CancellationContext create(LocalDateTime reservationTime, LocalDateTime cancellationTime,
                                             Money originalPrice, Member member, boolean isFirstTimeCancellation) {
        return new CancellationContext(reservationTime, cancellationTime, originalPrice, member, isFirstTimeCancellation);
    }

    /**
     * 현재 시간을 취소 시간으로 하는 컨텍스트 생성
     */
    public static CancellationContext createNow(LocalDateTime reservationTime, Money originalPrice,
                                                Member member, boolean isFirstTimeCancellation) {
        return create(reservationTime, LocalDateTime.now(), originalPrice, member, isFirstTimeCancellation);
    }

    // =========================
    // Getter 메서드들
    // =========================

    public LocalDateTime getReservationTime() {
        return reservationTime;
    }

    public LocalDateTime getCancellationTime() {
        return cancellationTime;
    }

    public Money getOriginalPrice() {
        return originalPrice;
    }

    public Member getMember() {
        return member;
    }

    public boolean isFirstTimeCancellation() {
        return isFirstTimeCancellation;
    }

    // =========================
    // Helper 메서드들
    // =========================

    /**
     * 예약 시간까지 남은 시간 (시간 단위)
     */
    public long getHoursUntilReservation() {
        return Duration.between(cancellationTime, reservationTime).toHours();
    }

    /**
     * 예약 시간까지 남은 시간 (분 단위)
     */
    public long getMinutesUntilReservation() {
        return Duration.between(cancellationTime, reservationTime).toMinutes();
    }

    /**
     * 예약 시간까지 남은 시간 (일 단위)
     */
    public long getDaysUntilReservation() {
        return Duration.between(cancellationTime, reservationTime).toDays();
    }

    /**
     * 이미 예약 시간이 지났는지 확인
     */
    public boolean isAfterReservationTime() {
        return cancellationTime.isAfter(reservationTime);
    }

    /**
     * 예약 당일 취소인지 확인
     */
    public boolean isSameDayAsCancellation() {
        return cancellationTime.toLocalDate().equals(reservationTime.toLocalDate());
    }

    /**
     * 취소와 예약 사이의 기간
     */
    public Duration getTimeBetweenCancellationAndReservation() {
        return Duration.between(cancellationTime, reservationTime);
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