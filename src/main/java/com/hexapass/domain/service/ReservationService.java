package com.hexapass.domain.service;

import com.hexapass.domain.common.Money;
import com.hexapass.domain.common.TimeSlot;
import com.hexapass.domain.model.*;
import com.hexapass.domain.policy.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 예약 도메인 서비스
 * 여러 도메인 객체들과 정책들을 조합하여 복잡한 예약 비즈니스 로직을 처리
 */
public class ReservationService {

    /**
     * 예약 생성 및 검증
     */
    public ReservationResult createReservation(
            Member member,
            Resource resource,
            TimeSlot timeSlot,
            ReservationPolicy reservationPolicy,
            DiscountPolicy discountPolicy,
            List<Reservation> existingReservations) {

        // 1. 기본 예약 가능성 검증
        if (!member.canMakeReservation()) {
            return ReservationResult.failed("회원의 예약 권한이 없습니다: " + member.getSummary());
        }

        if (!resource.isAvailable(timeSlot)) {
            return ReservationResult.failed("해당 시간대에 리소스를 사용할 수 없습니다");
        }

        // 2. 예약 정책 검증
        ReservationContext reservationContext = ReservationContext.builder()
                .member(member)
                .resource(resource)
                .requestedTimeSlot(timeSlot)
                .currentTime(LocalDateTime.now())
                .existingReservations(existingReservations)
                .build();

        if (!reservationPolicy.canReserve(reservationContext)) {
            return ReservationResult.failed(
                    reservationPolicy.getViolationReason(reservationContext)
            );
        }

        // 3. 특정 리소스 타입 예약 권한 확인
        if (!member.canReserve(resource.getType(), timeSlot.getStartTime().toLocalDate())) {
            return ReservationResult.failed("해당 리소스 타입에 대한 예약 권한이 없습니다");
        }

        // 4. 동시 예약 수 제한 확인
        int currentActiveReservations = (int) existingReservations.stream()
                .filter(r -> r.getMemberId().equals(member.getMemberId()) && r.isActive())
                .count();

        if (!member.canReserveSimultaneously(currentActiveReservations)) {
            return ReservationResult.failed("동시 예약 가능 수를 초과했습니다");
        }

        // 5. 선예약 제한 확인
        if (!member.canReserveInAdvance(timeSlot.getStartTime().toLocalDate())) {
            return ReservationResult.failed("선예약 가능 기간을 초과했습니다");
        }

        // 6. 가격 계산 및 할인 적용
        Money basePrice = calculateBasePrice(resource, timeSlot, member.getCurrentPlan());

        DiscountContext discountContext = DiscountContext.builder()
                .member(member)
                .plan(member.getCurrentPlan())
                .baseDate(timeSlot.getStartTime().toLocalDate())
                .build();

        Money finalPrice = discountPolicy.applyDiscount(basePrice, discountContext);

        // 7. 예약 객체 생성
        Reservation reservation = Reservation.create(
                generateReservationId(),
                member.getMemberId(),
                resource.getResourceId(),
                timeSlot
        );

        return ReservationResult.success(reservation, finalPrice, basePrice.subtract(finalPrice));
    }

    /**
     * 예약 수정
     */
    public ReservationResult modifyReservation(
            Reservation existingReservation,
            TimeSlot newTimeSlot,
            Resource resource,
            ReservationPolicy reservationPolicy,
            List<Reservation> otherReservations) {

        // 수정 가능 여부 확인
        if (!existingReservation.isModifiable()) {
            return ReservationResult.failed("예약 수정이 불가능한 상태입니다");
        }

        // 리소스 가용성 확인
        if (!resource.isAvailable(newTimeSlot)) {
            return ReservationResult.failed("새 시간대에 리소스를 사용할 수 없습니다");
        }

        // 새 시간대 예약 가능성 검증 (회원 정보는 예약에서 조회)
        // Member 객체가 필요하므로 실제 구현에서는 Repository를 통해 조회해야 함

        return ReservationResult.success(existingReservation, Money.zeroWon(), Money.zeroWon());
    }

    /**
     * 예약 확정 처리
     */
    public ReservationResult confirmReservation(Reservation reservation) {
        try {
            reservation.confirm();
            return ReservationResult.success(reservation, Money.zeroWon(), Money.zeroWon());
        } catch (IllegalStateException e) {
            return ReservationResult.failed("예약 확정 실패: " + e.getMessage());
        }
    }

    /**
     * 예약 완료 처리
     */
    public ReservationResult completeReservation(Reservation reservation) {
        try {
            if (reservation.getStatus() == com.hexapass.domain.type.ReservationStatus.CONFIRMED) {
                reservation.startUsing();
            }
            reservation.complete();
            return ReservationResult.success(reservation, Money.zeroWon(), Money.zeroWon());
        } catch (IllegalStateException e) {
            return ReservationResult.failed("예약 완료 처리 실패: " + e.getMessage());
        }
    }

    private Money calculateBasePrice(Resource resource, TimeSlot timeSlot, MembershipPlan plan) {
        // 기본 가격 계산 로직
        // 실제로는 리소스별 요금표나 플랜별 가격 정책이 필요
        long hours = timeSlot.getDurationMinutes() / 60;
        if (hours == 0) hours = 1; // 최소 1시간

        // 임시로 리소스 타입별 기본 요금 설정
        Money hourlyRate = switch (resource.getType()) {
            case GYM -> Money.won(10000);
            case POOL -> Money.won(15000);
            case STUDY_ROOM -> Money.won(5000);
            case MEETING_ROOM -> Money.won(20000);
            default -> Money.won(8000);
        };

        Money basePrice = hourlyRate.multiply(hours);

        // 플랜별 기본 할인 적용
        if (plan != null) {
            basePrice = plan.getDiscountedPrice().compareTo(plan.getPrice()) < 0 ?
                    basePrice.multiply(plan.getDiscountRate().subtract(java.math.BigDecimal.ONE).abs()) :
                    basePrice;
        }

        return basePrice;
    }

    private String generateReservationId() {
        return "RES_" + System.currentTimeMillis();
    }

    /**
     * 예약 결과를 담는 객체
     */
    public static class ReservationResult {
        private final boolean success;
        private final Reservation reservation;
        private final Money finalPrice;
        private final Money savedAmount;
        private final String errorMessage;

        private ReservationResult(boolean success, Reservation reservation,
                                  Money finalPrice, Money savedAmount, String errorMessage) {
            this.success = success;
            this.reservation = reservation;
            this.finalPrice = finalPrice;
            this.savedAmount = savedAmount;
            this.errorMessage = errorMessage;
        }

        public static ReservationResult success(Reservation reservation, Money finalPrice, Money savedAmount) {
            return new ReservationResult(true, reservation, finalPrice, savedAmount, null);
        }

        public static ReservationResult failed(String errorMessage) {
            return new ReservationResult(false, null, null, null, errorMessage);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public Reservation getReservation() { return reservation; }
        public Money getFinalPrice() { return finalPrice; }
        public Money getSavedAmount() { return savedAmount; }
        public String getErrorMessage() { return errorMessage; }
    }
}