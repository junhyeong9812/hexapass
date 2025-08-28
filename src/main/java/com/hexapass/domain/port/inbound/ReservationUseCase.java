package com.hexapass.domain.port.inbound;

import com.hexapass.domain.common.Money;
import com.hexapass.domain.common.TimeSlot;
import com.hexapass.domain.model.Reservation;
import com.hexapass.domain.type.ReservationStatus;
import com.hexapass.domain.type.ResourceType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 예약 관련 Use Case 인터페이스 (Inbound Port)
 * 예약 생성, 수정, 취소 등의 비즈니스 로직을 정의
 */
public interface ReservationUseCase {

    /**
     * 예약 생성
     */
    ReservationResult createReservation(CreateReservationCommand command);

    /**
     * 예약 확정
     */
    ReservationResult confirmReservation(ConfirmReservationCommand command);

    /**
     * 예약 수정
     */
    ReservationResult modifyReservation(ModifyReservationCommand command);

    /**
     * 예약 취소
     */
    CancellationResult cancelReservation(CancelReservationCommand command);

    /**
     * 예약 완료 처리
     */
    ReservationResult completeReservation(CompleteReservationCommand command);

    /**
     * 예약 조회
     */
    ReservationDetails getReservation(String reservationId);

    /**
     * 회원의 예약 목록 조회
     */
    List<ReservationSummary> getMemberReservations(
            String memberId,
            ReservationStatus status,
            LocalDate from,
            LocalDate to
    );

    /**
     * 리소스의 예약 목록 조회
     */
    List<ReservationSummary> getResourceReservations(
            String resourceId,
            LocalDate date
    );

    /**
     * 예약 가능한 시간대 조회
     */
    List<TimeSlot> getAvailableTimeSlots(
            String resourceId,
            LocalDate date,
            int durationMinutes
    );

    /**
     * 예약 가능 여부 확인
     */
    ReservationAvailabilityResult checkReservationAvailability(
            CheckAvailabilityCommand command
    );

    /**
     * 예약 충돌 검사
     */
    List<ConflictingReservation> checkReservationConflicts(
            String resourceId,
            TimeSlot timeSlot
    );

    /**
     * 노쇼 처리
     */
    ReservationResult processNoShow(String reservationId);

    /**
     * 예약 통계 조회
     */
    ReservationStatistics getReservationStatistics(
            String memberId,
            LocalDate from,
            LocalDate to
    );

    // =========================
    // Command Objects
    // =========================

    /**
     * 예약 생성 명령
     */
    class CreateReservationCommand {
        private final String memberId;
        private final String resourceId;
        private final TimeSlot timeSlot;
        private final String notes;
        private final String couponCode;

        public CreateReservationCommand(String memberId, String resourceId,
                                        TimeSlot timeSlot, String notes, String couponCode) {
            this.memberId = memberId;
            this.resourceId = resourceId;
            this.timeSlot = timeSlot;
            this.notes = notes;
            this.couponCode = couponCode;
        }

        // Getters
        public String getMemberId() { return memberId; }
        public String getResourceId() { return resourceId; }
        public TimeSlot getTimeSlot() { return timeSlot; }
        public String getNotes() { return notes; }
        public String getCouponCode() { return couponCode; }
    }

    /**
     * 예약 확정 명령
     */
    class ConfirmReservationCommand {
        private final String reservationId;
        private final String confirmedBy;
        private final String notes;

        public ConfirmReservationCommand(String reservationId, String confirmedBy, String notes) {
            this.reservationId = reservationId;
            this.confirmedBy = confirmedBy;
            this.notes = notes;
        }

        // Getters
        public String getReservationId() { return reservationId; }
        public String getConfirmedBy() { return confirmedBy; }
        public String getNotes() { return notes; }
    }

    /**
     * 예약 수정 명령
     */
    class ModifyReservationCommand {
        private final String reservationId;
        private final TimeSlot newTimeSlot;
        private final String newResourceId;
        private final String notes;
        private final String modifiedBy;

        public ModifyReservationCommand(String reservationId, TimeSlot newTimeSlot,
                                        String newResourceId, String notes, String modifiedBy) {
            this.reservationId = reservationId;
            this.newTimeSlot = newTimeSlot;
            this.newResourceId = newResourceId;
            this.notes = notes;
            this.modifiedBy = modifiedBy;
        }

        // Getters
        public String getReservationId() { return reservationId; }
        public TimeSlot getNewTimeSlot() { return newTimeSlot; }
        public String getNewResourceId() { return newResourceId; }
        public String getNotes() { return notes; }
        public String getModifiedBy() { return modifiedBy; }
    }

    /**
     * 예약 취소 명령
     */
    class CancelReservationCommand {
        private final String reservationId;
        private final String reason;
        private final String cancelledBy;
        private final boolean isSystemCancellation;

        public CancelReservationCommand(String reservationId, String reason,
                                        String cancelledBy, boolean isSystemCancellation) {
            this.reservationId = reservationId;
            this.reason = reason;
            this.cancelledBy = cancelledBy;
            this.isSystemCancellation = isSystemCancellation;
        }

        public static CancelReservationCommand userCancellation(String reservationId,
                                                                String reason, String cancelledBy) {
            return new CancelReservationCommand(reservationId, reason, cancelledBy, false);
        }

        public static CancelReservationCommand systemCancellation(String reservationId,
                                                                  String reason) {
            return new CancelReservationCommand(reservationId, reason, "SYSTEM", true);
        }

        // Getters
        public String getReservationId() { return reservationId; }
        public String getReason() { return reason; }
        public String getCancelledBy() { return cancelledBy; }
        public boolean isSystemCancellation() { return isSystemCancellation; }
    }

    /**
     * 예약 완료 명령
     */
    class CompleteReservationCommand {
        private final String reservationId;
        private final String completedBy;
        private final String notes;
        private final LocalDateTime actualEndTime;

        public CompleteReservationCommand(String reservationId, String completedBy,
                                          String notes, LocalDateTime actualEndTime) {
            this.reservationId = reservationId;
            this.completedBy = completedBy;
            this.notes = notes;
            this.actualEndTime = actualEndTime;
        }

        // Getters
        public String getReservationId() { return reservationId; }
        public String getCompletedBy() { return completedBy; }
        public String getNotes() { return notes; }
        public LocalDateTime getActualEndTime() { return actualEndTime; }
    }

    /**
     * 예약 가능성 확인 명령
     */
    class CheckAvailabilityCommand {
        private final String memberId;
        private final String resourceId;
        private final TimeSlot timeSlot;
        private final boolean checkMemberEligibility;
        private final boolean checkResourceAvailability;
        private final boolean checkTimeConflict;

        public CheckAvailabilityCommand(String memberId, String resourceId, TimeSlot timeSlot,
                                        boolean checkMemberEligibility, boolean checkResourceAvailability,
                                        boolean checkTimeConflict) {
            this.memberId = memberId;
            this.resourceId = resourceId;
            this.timeSlot = timeSlot;
            this.checkMemberEligibility = checkMemberEligibility;
            this.checkResourceAvailability = checkResourceAvailability;
            this.checkTimeConflict = checkTimeConflict;
        }

        public static CheckAvailabilityCommand fullCheck(String memberId, String resourceId,
                                                         TimeSlot timeSlot) {
            return new CheckAvailabilityCommand(memberId, resourceId, timeSlot, true, true, true);
        }

        // Getters
        public String getMemberId() { return memberId; }
        public String getResourceId() { return resourceId; }
        public TimeSlot getTimeSlot() { return timeSlot; }
        public boolean isCheckMemberEligibility() { return checkMemberEligibility; }
        public boolean isCheckResourceAvailability() { return checkResourceAvailability; }
        public boolean isCheckTimeConflict() { return checkTimeConflict; }
    }

    // =========================
    // Result Objects
    // =========================

    /**
     * 예약 결과
     */
    class ReservationResult {
        private final boolean success;
        private final Reservation reservation;
        private final Money finalPrice;
        private final Money discountAmount;
        private final String errorMessage;
        private final List<String> warnings;

        public ReservationResult(boolean success, Reservation reservation, Money finalPrice,
                                 Money discountAmount, String errorMessage, List<String> warnings) {
            this.success = success;
            this.reservation = reservation;
            this.finalPrice = finalPrice;
            this.discountAmount = discountAmount;
            this.errorMessage = errorMessage;
            this.warnings = warnings != null ? warnings : List.of();
        }

        public static ReservationResult success(Reservation reservation, Money finalPrice,
                                                Money discountAmount) {
            return new ReservationResult(true, reservation, finalPrice, discountAmount, null, null);
        }

        public static ReservationResult success(Reservation reservation, Money finalPrice,
                                                Money discountAmount, List<String> warnings) {
            return new ReservationResult(true, reservation, finalPrice, discountAmount, null, warnings);
        }

        public static ReservationResult failure(String errorMessage) {
            return new ReservationResult(false, null, null, null, errorMessage, null);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public Reservation getReservation() { return reservation; }
        public Money getFinalPrice() { return finalPrice; }
        public Money getDiscountAmount() { return discountAmount; }
        public String getErrorMessage() { return errorMessage; }
        public List<String> getWarnings() { return warnings; }
    }

    /**
     * 취소 결과
     */
    class CancellationResult {
        private final boolean success;
        private final Reservation cancelledReservation;
        private final Money refundAmount;
        private final Money cancellationFee;
        private final String errorMessage;

        public CancellationResult(boolean success, Reservation cancelledReservation,
                                  Money refundAmount, Money cancellationFee, String errorMessage) {
            this.success = success;
            this.cancelledReservation = cancelledReservation;
            this.refundAmount = refundAmount;
            this.cancellationFee = cancellationFee;
            this.errorMessage = errorMessage;
        }

        public static CancellationResult success(Reservation cancelledReservation,
                                                 Money refundAmount, Money cancellationFee) {
            return new CancellationResult(true, cancelledReservation, refundAmount,
                    cancellationFee, null);
        }

        public static CancellationResult failure(String errorMessage) {
            return new CancellationResult(false, null, null, null, errorMessage);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public Reservation getCancelledReservation() { return cancelledReservation; }
        public Money getRefundAmount() { return refundAmount; }
        public Money getCancellationFee() { return cancellationFee; }
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * 예약 가능성 결과
     */
    class ReservationAvailabilityResult {
        private final boolean available;
        private final List<String> restrictions;
        private final List<String> warnings;
        private final Money estimatedPrice;

        public ReservationAvailabilityResult(boolean available, List<String> restrictions,
                                             List<String> warnings, Money estimatedPrice) {
            this.available = available;
            this.restrictions = restrictions != null ? restrictions : List.of();
            this.warnings = warnings != null ? warnings : List.of();
            this.estimatedPrice = estimatedPrice;
        }

        public static ReservationAvailabilityResult available(Money estimatedPrice) {
            return new ReservationAvailabilityResult(true, null, null, estimatedPrice);
        }

        public static ReservationAvailabilityResult unavailable(List<String> restrictions) {
            return new ReservationAvailabilityResult(false, restrictions, null, null);
        }

        // Getters
        public boolean isAvailable() { return available; }
        public List<String> getRestrictions() { return restrictions; }
        public List<String> getWarnings() { return warnings; }
        public Money getEstimatedPrice() { return estimatedPrice; }
    }

    /**
     * 충돌하는 예약 정보
     */
    class ConflictingReservation {
        private final String reservationId;
        private final String memberId;
        private final TimeSlot timeSlot;
        private final ReservationStatus status;

        public ConflictingReservation(String reservationId, String memberId,
                                      TimeSlot timeSlot, ReservationStatus status) {
            this.reservationId = reservationId;
            this.memberId = memberId;
            this.timeSlot = timeSlot;
            this.status = status;
        }

        // Getters
        public String getReservationId() { return reservationId; }
        public String getMemberId() { return memberId; }
        public TimeSlot getTimeSlot() { return timeSlot; }
        public ReservationStatus getStatus() { return status; }
    }

    /**
     * 예약 상세 정보
     */
    class ReservationDetails {
        private final Reservation reservation;
        private final String memberName;
        private final String resourceName;
        private final ResourceType resourceType;
        private final Money finalPrice;
        private final Money discountAmount;
        private final String paymentStatus;
        private final List<String> statusHistory;

        public ReservationDetails(Reservation reservation, String memberName, String resourceName,
                                  ResourceType resourceType, Money finalPrice, Money discountAmount,
                                  String paymentStatus, List<String> statusHistory) {
            this.reservation = reservation;
            this.memberName = memberName;
            this.resourceName = resourceName;
            this.resourceType = resourceType;
            this.finalPrice = finalPrice;
            this.discountAmount = discountAmount;
            this.paymentStatus = paymentStatus;
            this.statusHistory = statusHistory != null ? statusHistory : List.of();
        }

        // Getters
        public Reservation getReservation() { return reservation; }
        public String getMemberName() { return memberName; }
        public String getResourceName() { return resourceName; }
        public ResourceType getResourceType() { return resourceType; }
        public Money getFinalPrice() { return finalPrice; }
        public Money getDiscountAmount() { return discountAmount; }
        public String getPaymentStatus() { return paymentStatus; }
        public List<String> getStatusHistory() { return statusHistory; }
    }

    /**
     * 예약 요약 정보
     */
    class ReservationSummary {
        private final String reservationId;
        private final String resourceName;
        private final TimeSlot timeSlot;
        private final ReservationStatus status;
        private final Money price;
        private final LocalDateTime createdAt;

        public ReservationSummary(String reservationId, String resourceName, TimeSlot timeSlot,
                                  ReservationStatus status, Money price, LocalDateTime createdAt) {
            this.reservationId = reservationId;
            this.resourceName = resourceName;
            this.timeSlot = timeSlot;
            this.status = status;
            this.price = price;
            this.createdAt = createdAt;
        }

        // Getters
        public String getReservationId() { return reservationId; }
        public String getResourceName() { return resourceName; }
        public TimeSlot getTimeSlot() { return timeSlot; }
        public ReservationStatus getStatus() { return status; }
        public Money getPrice() { return price; }
        public LocalDateTime getCreatedAt() { return createdAt; }
    }

    /**
     * 예약 통계
     */
    class ReservationStatistics {
        private final int totalReservations;
        private final int completedReservations;
        private final int cancelledReservations;
        private final int noShowReservations;
        private final Money totalSpent;
        private final Money totalSaved;
        private final double completionRate;
        private final double noShowRate;

        public ReservationStatistics(int totalReservations, int completedReservations,
                                     int cancelledReservations, int noShowReservations,
                                     Money totalSpent, Money totalSaved) {
            this.totalReservations = totalReservations;
            this.completedReservations = completedReservations;
            this.cancelledReservations = cancelledReservations;
            this.noShowReservations = noShowReservations;
            this.totalSpent = totalSpent;
            this.totalSaved = totalSaved;
            this.completionRate = totalReservations > 0 ?
                    (double) completedReservations / totalReservations : 0.0;
            this.noShowRate = totalReservations > 0 ?
                    (double) noShowReservations / totalReservations : 0.0;
        }

        // Getters
        public int getTotalReservations() { return totalReservations; }
        public int getCompletedReservations() { return completedReservations; }
        public int getCancelledReservations() { return cancelledReservations; }
        public int getNoShowReservations() { return noShowReservations; }
        public Money getTotalSpent() { return totalSpent; }
        public Money getTotalSaved() { return totalSaved; }
        public double getCompletionRate() { return completionRate; }
        public double getNoShowRate() { return noShowRate; }
    }
}