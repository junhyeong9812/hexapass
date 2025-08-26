package com.hexapass.domain.service;

import com.hexapass.domain.common.Money;
import com.hexapass.domain.model.Reservation;
import com.hexapass.domain.policy.CancellationContext;
import com.hexapass.domain.policy.CancellationPolicy;

import java.time.LocalDateTime;

/**
 * 취소 도메인 서비스
 * 예약 취소와 관련된 복잡한 비즈니스 로직을 처리
 */
public class CancellationService {

    /**
     * 예약 취소 처리
     */
    public CancellationResult cancelReservation(
            Reservation reservation,
            CancellationPolicy cancellationPolicy,
            boolean isFirstTimeCancellation) {

        // 1. 취소 가능 여부 확인 (예약 상태 기준)
        if (!reservation.isCancellable()) {
            return CancellationResult.failed("예약 상태상 취소가 불가능합니다: " + reservation.getStatus());
        }

        // 2. 취소 컨텍스트 생성
        CancellationContext context = CancellationContext.create(
                reservation.getTimeSlot().getStartTime(),
                LocalDateTime.now(),
                Money.won(50000), // 임시 가격 - 실제로는 예약의 결제 정보에서 가져와야 함
                null, // 회원 정보 - 실제로는 Repository에서 조회
                isFirstTimeCancellation
        );

        // 3. 취소 정책 적용
        if (!cancellationPolicy.isCancellationAllowed(context)) {
            return CancellationResult.failed(
                    cancellationPolicy.getCancellationDenialReason(context)
            );
        }

        // 4. 취소 수수료 계산
        Money originalAmount = Money.won(50000); // 실제로는 예약의 결제 금액
        Money cancellationFee = cancellationPolicy.calculateCancellationFee(originalAmount, context);
        Money refundAmount = originalAmount.subtract(cancellationFee);

        // 5. 예약 상태 변경
        try {
            reservation.cancel(generateCancellationReason(context, cancellationPolicy));

            return CancellationResult.success(
                    reservation,
                    cancellationFee,
                    refundAmount,
                    generateCancellationReason(context, cancellationPolicy)
            );
        } catch (IllegalStateException e) {
            return CancellationResult.failed("예약 취소 처리 실패: " + e.getMessage());
        }
    }

    /**
     * 취소 수수료 미리 계산 (실제 취소 전 안내용)
     */
    public CancellationPreview previewCancellation(
            Reservation reservation,
            CancellationPolicy cancellationPolicy,
            boolean isFirstTimeCancellation) {

        // 취소 컨텍스트 생성
        CancellationContext context = CancellationContext.create(
                reservation.getTimeSlot().getStartTime(),
                LocalDateTime.now(),
                Money.won(50000), // 실제로는 예약의 결제 정보에서 가져와야 함
                null, // 회원 정보
                isFirstTimeCancellation
        );

        // 예약 상태 및 정책 기반 취소 가능성 확인
        boolean canCancel = reservation.isCancellable() &&
                cancellationPolicy.isCancellationAllowed(context);

        if (!canCancel) {
            String reason = !reservation.isCancellable() ?
                    "예약 상태상 취소 불가 (" + reservation.getStatus() + ")" :
                    cancellationPolicy.getCancellationDenialReason(context);

            return CancellationPreview.notAllowed(reason);
        }

        // 수수료 계산
        Money originalAmount = Money.won(50000);
        Money fee = cancellationPolicy.calculateCancellationFee(originalAmount, context);
        Money refund = originalAmount.subtract(fee);

        return CancellationPreview.allowed(fee, refund,
                generateCancellationReason(context, cancellationPolicy));
    }

    /**
     * 시스템 자동 취소 처리 (노쇼, 만료 등)
     */
    public CancellationResult autoCancel(Reservation reservation, String systemReason) {
        try {
            reservation.autoCancel(systemReason);

            return CancellationResult.success(
                    reservation,
                    Money.zeroWon(), // 시스템 취소는 수수료 없음
                    Money.zeroWon(), // 환불도 없음
                    "시스템 자동 취소: " + systemReason
            );
        } catch (IllegalStateException e) {
            return CancellationResult.failed("자동 취소 처리 실패: " + e.getMessage());
        }
    }

    /**
     * 노쇼 처리
     */
    public CancellationResult processNoShow(Reservation reservation) {
        if (!reservation.isNoShow()) {
            return CancellationResult.failed("노쇼 조건을 만족하지 않습니다");
        }

        return autoCancel(reservation, "노쇼로 인한 자동 취소");
    }

    private String generateCancellationReason(CancellationContext context, CancellationPolicy policy) {
        return String.format("취소 정책: %s, 예약까지 %d시간 남음",
                policy.getDescription(),
                context.getHoursUntilReservation()
        );
    }

    /**
     * 취소 결과
     */
    public static class CancellationResult {
        private final boolean success;
        private final Reservation cancelledReservation;
        private final Money cancellationFee;
        private final Money refundAmount;
        private final String reason;
        private final String errorMessage;

        private CancellationResult(boolean success, Reservation cancelledReservation,
                                   Money cancellationFee, Money refundAmount,
                                   String reason, String errorMessage) {
            this.success = success;
            this.cancelledReservation = cancelledReservation;
            this.cancellationFee = cancellationFee;
            this.refundAmount = refundAmount;
            this.reason = reason;
            this.errorMessage = errorMessage;
        }

        public static CancellationResult success(Reservation cancelledReservation,
                                                 Money cancellationFee, Money refundAmount, String reason) {
            return new CancellationResult(true, cancelledReservation,
                    cancellationFee, refundAmount, reason, null);
        }

        public static CancellationResult failed(String errorMessage) {
            return new CancellationResult(false, null, null, null, null, errorMessage);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public Reservation getCancelledReservation() { return cancelledReservation; }
        public Money getCancellationFee() { return cancellationFee; }
        public Money getRefundAmount() { return refundAmount; }
        public String getReason() { return reason; }
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * 취소 미리보기
     */
    public static class CancellationPreview {
        private final boolean allowed;
        private final Money estimatedFee;
        private final Money estimatedRefund;
        private final String reason;

        private CancellationPreview(boolean allowed, Money estimatedFee,
                                    Money estimatedRefund, String reason) {
            this.allowed = allowed;
            this.estimatedFee = estimatedFee;
            this.estimatedRefund = estimatedRefund;
            this.reason = reason;
        }

        public static CancellationPreview allowed(Money fee, Money refund, String reason) {
            return new CancellationPreview(true, fee, refund, reason);
        }

        public static CancellationPreview notAllowed(String reason) {
            return new CancellationPreview(false, null, null, reason);
        }

        // Getters
        public boolean isAllowed() { return allowed; }
        public Money getEstimatedFee() { return estimatedFee; }
        public Money getEstimatedRefund() { return estimatedRefund; }
        public String getReason() { return reason; }
    }
}