package com.hexapass.domain.port.inbound;

import com.hexapass.domain.common.Money;
import com.hexapass.domain.model.Reservation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 취소 관련 Use Case 인터페이스 (Inbound Port)
 * 예약 취소, 환불 처리, 취소 정책 관리 등의 비즈니스 로직을 정의
 */
public interface CancellationUseCase {

    /**
     * 예약 취소 미리보기 (실제 취소 전 수수료 안내)
     */
    CancellationPreview previewCancellation(PreviewCancellationCommand command);

    /**
     * 예약 취소 처리
     */
    CancellationResult cancelReservation(CancelReservationCommand command);

    /**
     * 대량 예약 취소 처리 (시스템 메인테넌스 등)
     */
    BulkCancellationResult cancelMultipleReservations(BulkCancelCommand command);

    /**
     * 취소 철회 (취소를 되돌리기)
     */
    CancellationResult reverseCancellation(ReverseCancellationCommand command);

    /**
     * 환불 처리
     */
    RefundResult processRefund(ProcessRefundCommand command);

    /**
     * 환불 상태 조회
     */
    RefundStatus getRefundStatus(String refundId);

    /**
     * 회원의 취소 이력 조회
     */
    List<CancellationHistory> getMemberCancellationHistory(
            String memberId,
            LocalDate from,
            LocalDate to
    );

    /**
     * 취소 통계 조회
     */
    CancellationStatistics getCancellationStatistics(
            LocalDate from,
            LocalDate to,
            String resourceId
    );

    /**
     * 노쇼 처리
     */
    NoShowResult processNoShow(ProcessNoShowCommand command);

    /**
     * 자동 취소 처리 (만료된 예약 등)
     */
    AutoCancellationResult processAutoCancellation(AutoCancellationCommand command);

    /**
     * 취소 가능 여부 확인
     */
    CancellationEligibilityResult checkCancellationEligibility(String reservationId);

    /**
     * 취소 정책 조회
     */
    List<CancellationPolicyInfo> getApplicableCancellationPolicies(String reservationId);

    // =========================
    // Command Objects
    // =========================

    /**
     * 취소 미리보기 명령
     */
    class PreviewCancellationCommand {
        private final String reservationId;
        private final String memberId;
        private final LocalDateTime cancellationTime;

        public PreviewCancellationCommand(String reservationId, String memberId,
                                          LocalDateTime cancellationTime) {
            this.reservationId = reservationId;
            this.memberId = memberId;
            this.cancellationTime = cancellationTime != null ? cancellationTime : LocalDateTime.now();
        }

        // Getters
        public String getReservationId() { return reservationId; }
        public String getMemberId() { return memberId; }
        public LocalDateTime getCancellationTime() { return cancellationTime; }
    }

    /**
     * 예약 취소 명령
     */
    class CancelReservationCommand {
        private final String reservationId;
        private final String memberId;
        private final String reason;
        private final CancellationType type;
        private final String cancelledBy;
        private final boolean forceCancel;
        private final String adminNotes;

        public CancelReservationCommand(String reservationId, String memberId, String reason,
                                        CancellationType type, String cancelledBy,
                                        boolean forceCancel, String adminNotes) {
            this.reservationId = reservationId;
            this.memberId = memberId;
            this.reason = reason;
            this.type = type;
            this.cancelledBy = cancelledBy;
            this.forceCancel = forceCancel;
            this.adminNotes = adminNotes;
        }

        public static CancelReservationCommand userCancellation(String reservationId,
                                                                String memberId, String reason) {
            return new CancelReservationCommand(reservationId, memberId, reason,
                    CancellationType.USER_REQUESTED, memberId, false, null);
        }

        public static CancelReservationCommand adminCancellation(String reservationId,
                                                                 String reason, String adminId, String notes) {
            return new CancelReservationCommand(reservationId, null, reason,
                    CancellationType.ADMIN_CANCELLED, adminId, true, notes);
        }

        public static CancelReservationCommand systemCancellation(String reservationId,
                                                                  String reason) {
            return new CancelReservationCommand(reservationId, null, reason,
                    CancellationType.SYSTEM_AUTO, "SYSTEM", true, null);
        }

        // Getters
        public String getReservationId() { return reservationId; }
        public String getMemberId() { return memberId; }
        public String getReason() { return reason; }
        public CancellationType getType() { return type; }
        public String getCancelledBy() { return cancelledBy; }
        public boolean isForceCancel() { return forceCancel; }
        public String getAdminNotes() { return adminNotes; }
    }

    /**
     * 대량 취소 명령
     */
    class BulkCancelCommand {
        private final List<String> reservationIds;
        private final String reason;
        private final CancellationType type;
        private final String cancelledBy;
        private final boolean sendNotifications;
        private final String batchId;

        public BulkCancelCommand(List<String> reservationIds, String reason, CancellationType type,
                                 String cancelledBy, boolean sendNotifications, String batchId) {
            this.reservationIds = reservationIds;
            this.reason = reason;
            this.type = type;
            this.cancelledBy = cancelledBy;
            this.sendNotifications = sendNotifications;
            this.batchId = batchId;
        }

        // Getters
        public List<String> getReservationIds() { return reservationIds; }
        public String getReason() { return reason; }
        public CancellationType getType() { return type; }
        public String getCancelledBy() { return cancelledBy; }
        public boolean isSendNotifications() { return sendNotifications; }
        public String getBatchId() { return batchId; }
    }

    /**
     * 취소 철회 명령
     */
    class ReverseCancellationCommand {
        private final String reservationId;
        private final String reason;
        private final String reversedBy;
        private final boolean refundReversed;

        public ReverseCancellationCommand(String reservationId, String reason,
                                          String reversedBy, boolean refundReversed) {
            this.reservationId = reservationId;
            this.reason = reason;
            this.reversedBy = reversedBy;
            this.refundReversed = refundReversed;
        }

        // Getters
        public String getReservationId() { return reservationId; }
        public String getReason() { return reason; }
        public String getReversedBy() { return reversedBy; }
        public boolean isRefundReversed() { return refundReversed; }
    }

    /**
     * 환불 처리 명령
     */
    class ProcessRefundCommand {
        private final String reservationId;
        private final Money refundAmount;
        private final String reason;
        private final RefundMethod method;
        private final String processedBy;
        private final boolean partialRefund;

        public ProcessRefundCommand(String reservationId, Money refundAmount, String reason,
                                    RefundMethod method, String processedBy, boolean partialRefund) {
            this.reservationId = reservationId;
            this.refundAmount = refundAmount;
            this.reason = reason;
            this.method = method;
            this.processedBy = processedBy;
            this.partialRefund = partialRefund;
        }

        // Getters
        public String getReservationId() { return reservationId; }
        public Money getRefundAmount() { return refundAmount; }
        public String getReason() { return reason; }
        public RefundMethod getMethod() { return method; }
        public String getProcessedBy() { return processedBy; }
        public boolean isPartialRefund() { return partialRefund; }
    }

    /**
     * 노쇼 처리 명령
     */
    class ProcessNoShowCommand {
        private final String reservationId;
        private final LocalDateTime noShowTime;
        private final String notes;
        private final boolean applyPenalty;
        private final String processedBy;

        public ProcessNoShowCommand(String reservationId, LocalDateTime noShowTime,
                                    String notes, boolean applyPenalty, String processedBy) {
            this.reservationId = reservationId;
            this.noShowTime = noShowTime != null ? noShowTime : LocalDateTime.now();
            this.notes = notes;
            this.applyPenalty = applyPenalty;
            this.processedBy = processedBy;
        }

        // Getters
        public String getReservationId() { return reservationId; }
        public LocalDateTime getNoShowTime() { return noShowTime; }
        public String getNotes() { return notes; }
        public boolean isApplyPenalty() { return applyPenalty; }
        public String getProcessedBy() { return processedBy; }
    }

    /**
     * 자동 취소 명령
     */
    class AutoCancellationCommand {
        private final LocalDateTime cutoffTime;
        private final List<String> targetStatuses;
        private final String reason;
        private final boolean dryRun;
        private final int batchSize;

        public AutoCancellationCommand(LocalDateTime cutoffTime, List<String> targetStatuses,
                                       String reason, boolean dryRun, int batchSize) {
            this.cutoffTime = cutoffTime;
            this.targetStatuses = targetStatuses;
            this.reason = reason;
            this.dryRun = dryRun;
            this.batchSize = batchSize;
        }

        // Getters
        public LocalDateTime getCutoffTime() { return cutoffTime; }
        public List<String> getTargetStatuses() { return targetStatuses; }
        public String getReason() { return reason; }
        public boolean isDryRun() { return dryRun; }
        public int getBatchSize() { return batchSize; }
    }

    // =========================
    // Result Objects
    // =========================

    /**
     * 취소 미리보기 결과
     */
    class CancellationPreview {
        private final boolean cancellable;
        private final Money cancellationFee;
        private final Money refundAmount;
        private final String policyDescription;
        private final List<String> restrictions;
        private final LocalDateTime deadline;

        public CancellationPreview(boolean cancellable, Money cancellationFee, Money refundAmount,
                                   String policyDescription, List<String> restrictions, LocalDateTime deadline) {
            this.cancellable = cancellable;
            this.cancellationFee = cancellationFee;
            this.refundAmount = refundAmount;
            this.policyDescription = policyDescription;
            this.restrictions = restrictions != null ? restrictions : List.of();
            this.deadline = deadline;
        }

        public static CancellationPreview allowed(Money fee, Money refund, String policyDescription) {
            return new CancellationPreview(true, fee, refund, policyDescription, null, null);
        }

        public static CancellationPreview denied(List<String> restrictions) {
            return new CancellationPreview(false, null, null, null, restrictions, null);
        }

        // Getters
        public boolean isCancellable() { return cancellable; }
        public Money getCancellationFee() { return cancellationFee; }
        public Money getRefundAmount() { return refundAmount; }
        public String getPolicyDescription() { return policyDescription; }
        public List<String> getRestrictions() { return restrictions; }
        public LocalDateTime getDeadline() { return deadline; }
    }

    /**
     * 취소 결과
     */
    class CancellationResult {
        private final boolean success;
        private final Reservation cancelledReservation;
        private final Money cancellationFee;
        private final Money refundAmount;
        private final String refundId;
        private final String errorMessage;
        private final LocalDateTime processedAt;

        public CancellationResult(boolean success, Reservation cancelledReservation,
                                  Money cancellationFee, Money refundAmount, String refundId,
                                  String errorMessage, LocalDateTime processedAt) {
            this.success = success;
            this.cancelledReservation = cancelledReservation;
            this.cancellationFee = cancellationFee;
            this.refundAmount = refundAmount;
            this.refundId = refundId;
            this.errorMessage = errorMessage;
            this.processedAt = processedAt != null ? processedAt : LocalDateTime.now();
        }

        public static CancellationResult success(Reservation reservation, Money fee,
                                                 Money refund, String refundId) {
            return new CancellationResult(true, reservation, fee, refund, refundId, null, null);
        }

        public static CancellationResult failure(String errorMessage) {
            return new CancellationResult(false, null, null, null, null, errorMessage, null);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public Reservation getCancelledReservation() { return cancelledReservation; }
        public Money getCancellationFee() { return cancellationFee; }
        public Money getRefundAmount() { return refundAmount; }
        public String getRefundId() { return refundId; }
        public String getErrorMessage() { return errorMessage; }
        public LocalDateTime getProcessedAt() { return processedAt; }
    }

    /**
     * 대량 취소 결과
     */
    class BulkCancellationResult {
        private final String batchId;
        private final int totalRequested;
        private final int successCount;
        private final int failureCount;
        private final Money totalRefundAmount;
        private final Money totalFeeAmount;
        private final List<String> successIds;
        private final List<FailedCancellation> failures;
        private final LocalDateTime completedAt;

        public BulkCancellationResult(String batchId, int totalRequested, int successCount, int failureCount,
                                      Money totalRefundAmount, Money totalFeeAmount, List<String> successIds,
                                      List<FailedCancellation> failures) {
            this.batchId = batchId;
            this.totalRequested = totalRequested;
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.totalRefundAmount = totalRefundAmount;
            this.totalFeeAmount = totalFeeAmount;
            this.successIds = successIds != null ? successIds : List.of();
            this.failures = failures != null ? failures : List.of();
            this.completedAt = LocalDateTime.now();
        }

        // Getters
        public String getBatchId() { return batchId; }
        public int getTotalRequested() { return totalRequested; }
        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public Money getTotalRefundAmount() { return totalRefundAmount; }
        public Money getTotalFeeAmount() { return totalFeeAmount; }
        public List<String> getSuccessIds() { return successIds; }
        public List<FailedCancellation> getFailures() { return failures; }
        public LocalDateTime getCompletedAt() { return completedAt; }
        public double getSuccessRate() {
            return totalRequested > 0 ? (double) successCount / totalRequested : 0.0;
        }
    }

    /**
     * 환불 결과
     */
    class RefundResult {
        private final boolean success;
        private final String refundId;
        private final Money refundAmount;
        private final RefundStatus status;
        private final String errorMessage;
        private final LocalDateTime processedAt;
        private final String transactionId;

        public RefundResult(boolean success, String refundId, Money refundAmount, RefundStatus status,
                            String errorMessage, LocalDateTime processedAt, String transactionId) {
            this.success = success;
            this.refundId = refundId;
            this.refundAmount = refundAmount;
            this.status = status;
            this.errorMessage = errorMessage;
            this.processedAt = processedAt != null ? processedAt : LocalDateTime.now();
            this.transactionId = transactionId;
        }

        public static RefundResult success(String refundId, Money refundAmount,
                                           RefundStatus status, String transactionId) {
            return new RefundResult(true, refundId, refundAmount, status, null, null, transactionId);
        }

        public static RefundResult failure(String errorMessage) {
            return new RefundResult(false, null, null, RefundStatus.FAILED, errorMessage, null, null);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getRefundId() { return refundId; }
        public Money getRefundAmount() { return refundAmount; }
        public RefundStatus getStatus() { return status; }
        public String getErrorMessage() { return errorMessage; }
        public LocalDateTime getProcessedAt() { return processedAt; }
        public String getTransactionId() { return transactionId; }
    }

    /**
     * 노쇼 결과
     */
    class NoShowResult {
        private final boolean success;
        private final Reservation reservation;
        private final Money penaltyAmount;
        private final String errorMessage;
        private final LocalDateTime processedAt;

        public NoShowResult(boolean success, Reservation reservation, Money penaltyAmount,
                            String errorMessage, LocalDateTime processedAt) {
            this.success = success;
            this.reservation = reservation;
            this.penaltyAmount = penaltyAmount;
            this.errorMessage = errorMessage;
            this.processedAt = processedAt != null ? processedAt : LocalDateTime.now();
        }

        public static NoShowResult success(Reservation reservation, Money penaltyAmount) {
            return new NoShowResult(true, reservation, penaltyAmount, null, null);
        }

        public static NoShowResult failure(String errorMessage) {
            return new NoShowResult(false, null, null, errorMessage, null);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public Reservation getReservation() { return reservation; }
        public Money getPenaltyAmount() { return penaltyAmount; }
        public String getErrorMessage() { return errorMessage; }
        public LocalDateTime getProcessedAt() { return processedAt; }
    }

    /**
     * 자동 취소 결과
     */
    class AutoCancellationResult {
        private final boolean success;
        private final int processedCount;
        private final int cancelledCount;
        private final int skippedCount;
        private final Money totalRefunded;
        private final List<String> cancelledReservationIds;
        private final List<String> errors;
        private final LocalDateTime completedAt;

        public AutoCancellationResult(boolean success, int processedCount, int cancelledCount,
                                      int skippedCount, Money totalRefunded,
                                      List<String> cancelledReservationIds, List<String> errors) {
            this.success = success;
            this.processedCount = processedCount;
            this.cancelledCount = cancelledCount;
            this.skippedCount = skippedCount;
            this.totalRefunded = totalRefunded;
            this.cancelledReservationIds = cancelledReservationIds != null ? cancelledReservationIds : List.of();
            this.errors = errors != null ? errors : List.of();
            this.completedAt = LocalDateTime.now();
        }

        // Getters
        public boolean isSuccess() { return success; }
        public int getProcessedCount() { return processedCount; }
        public int getCancelledCount() { return cancelledCount; }
        public int getSkippedCount() { return skippedCount; }
        public Money getTotalRefunded() { return totalRefunded; }
        public List<String> getCancelledReservationIds() { return cancelledReservationIds; }
        public List<String> getErrors() { return errors; }
        public LocalDateTime getCompletedAt() { return completedAt; }
    }

    /**
     * 취소 자격 결과
     */
    class CancellationEligibilityResult {
        private final boolean eligible;
        private final List<String> requirements;
        private final List<String> violations;
        private final CancellationPolicyInfo applicablePolicy;
        private final LocalDateTime cancellationDeadline;

        public CancellationEligibilityResult(boolean eligible, List<String> requirements,
                                             List<String> violations, CancellationPolicyInfo applicablePolicy,
                                             LocalDateTime cancellationDeadline) {
            this.eligible = eligible;
            this.requirements = requirements != null ? requirements : List.of();
            this.violations = violations != null ? violations : List.of();
            this.applicablePolicy = applicablePolicy;
            this.cancellationDeadline = cancellationDeadline;
        }

        public static CancellationEligibilityResult eligible(CancellationPolicyInfo policy,
                                                             LocalDateTime deadline) {
            return new CancellationEligibilityResult(true, null, null, policy, deadline);
        }

        public static CancellationEligibilityResult ineligible(List<String> violations,
                                                               CancellationPolicyInfo policy) {
            return new CancellationEligibilityResult(false, null, violations, policy, null);
        }

        // Getters
        public boolean isEligible() { return eligible; }
        public List<String> getRequirements() { return requirements; }
        public List<String> getViolations() { return violations; }
        public CancellationPolicyInfo getApplicablePolicy() { return applicablePolicy; }
        public LocalDateTime getCancellationDeadline() { return cancellationDeadline; }
    }

    // =========================
    // Supporting Classes
    // =========================

    /**
     * 취소 유형
     */
    enum CancellationType {
        USER_REQUESTED("회원 요청"),
        ADMIN_CANCELLED("관리자 취소"),
        SYSTEM_AUTO("시스템 자동"),
        NO_SHOW("노쇼"),
        EMERGENCY("응급상황"),
        MAINTENANCE("시설 점검");

        private final String displayName;

        CancellationType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 환불 방법
     */
    enum RefundMethod {
        ORIGINAL_PAYMENT("원결제수단"),
        BANK_TRANSFER("계좌이체"),
        CASH("현금"),
        CREDIT("크레딧"),
        POINTS("포인트");

        private final String displayName;

        RefundMethod(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * 환불 상태
     */
    enum RefundStatus {
        PENDING("처리중"),
        APPROVED("승인됨"),
        PROCESSING("진행중"),
        COMPLETED("완료"),
        FAILED("실패"),
        CANCELLED("취소됨");

        private final String displayName;

        RefundStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isFinal() {
            return this == COMPLETED || this == FAILED || this == CANCELLED;
        }
    }

    /**
     * 실패한 취소 정보
     */
    class FailedCancellation {
        private final String reservationId;
        private final String errorMessage;
        private final String errorCode;

        public FailedCancellation(String reservationId, String errorMessage, String errorCode) {
            this.reservationId = reservationId;
            this.errorMessage = errorMessage;
            this.errorCode = errorCode;
        }

        // Getters
        public String getReservationId() { return reservationId; }
        public String getErrorMessage() { return errorMessage; }
        public String getErrorCode() { return errorCode; }
    }

    /**
     * 취소 이력
     */
    class CancellationHistory {
        private final String reservationId;
        private final String resourceName;
        private final LocalDateTime reservationTime;
        private final LocalDateTime cancelledAt;
        private final CancellationType cancellationType;
        private final String reason;
        private final Money cancellationFee;
        private final Money refundAmount;
        private final RefundStatus refundStatus;

        public CancellationHistory(String reservationId, String resourceName, LocalDateTime reservationTime,
                                   LocalDateTime cancelledAt, CancellationType cancellationType, String reason,
                                   Money cancellationFee, Money refundAmount, RefundStatus refundStatus) {
            this.reservationId = reservationId;
            this.resourceName = resourceName;
            this.reservationTime = reservationTime;
            this.cancelledAt = cancelledAt;
            this.cancellationType = cancellationType;
            this.reason = reason;
            this.cancellationFee = cancellationFee;
            this.refundAmount = refundAmount;
            this.refundStatus = refundStatus;
        }

        // Getters
        public String getReservationId() { return reservationId; }
        public String getResourceName() { return resourceName; }
        public LocalDateTime getReservationTime() { return reservationTime; }
        public LocalDateTime getCancelledAt() { return cancelledAt; }
        public CancellationType getCancellationType() { return cancellationType; }
        public String getReason() { return reason; }
        public Money getCancellationFee() { return cancellationFee; }
        public Money getRefundAmount() { return refundAmount; }
        public RefundStatus getRefundStatus() { return refundStatus; }
    }

    /**
     * 취소 통계
     */
    class CancellationStatistics {
        private final int totalCancellations;
        private final int userCancellations;
        private final int adminCancellations;
        private final int systemCancellations;
        private final int noShows;
        private final Money totalRefunded;
        private final Money totalFees;
        private final double cancellationRate;
        private final double noShowRate;

        public CancellationStatistics(int totalCancellations, int userCancellations, int adminCancellations,
                                      int systemCancellations, int noShows, Money totalRefunded, Money totalFees,
                                      int totalReservations) {
            this.totalCancellations = totalCancellations;
            this.userCancellations = userCancellations;
            this.adminCancellations = adminCancellations;
            this.systemCancellations = systemCancellations;
            this.noShows = noShows;
            this.totalRefunded = totalRefunded;
            this.totalFees = totalFees;
            this.cancellationRate = totalReservations > 0 ?
                    (double) totalCancellations / totalReservations : 0.0;
            this.noShowRate = totalReservations > 0 ?
                    (double) noShows / totalReservations : 0.0;
        }

        // Getters
        public int getTotalCancellations() { return totalCancellations; }
        public int getUserCancellations() { return userCancellations; }
        public int getAdminCancellations() { return adminCancellations; }
        public int getSystemCancellations() { return systemCancellations; }
        public int getNoShows() { return noShows; }
        public Money getTotalRefunded() { return totalRefunded; }
        public Money getTotalFees() { return totalFees; }
        public double getCancellationRate() { return cancellationRate; }
        public double getNoShowRate() { return noShowRate; }
    }

    /**
     * 취소 정책 정보
     */
    class CancellationPolicyInfo {
        private final String policyId;
        private final String name;
        private final String description;
        private final int hoursBeforeReservation;
        private final Money flatFee;
        private final double percentageFee;
        private final boolean allowsFreeCancellation;
        private final int freeCancellationHours;

        public CancellationPolicyInfo(String policyId, String name, String description,
                                      int hoursBeforeReservation, Money flatFee, double percentageFee,
                                      boolean allowsFreeCancellation, int freeCancellationHours) {
            this.policyId = policyId;
            this.name = name;
            this.description = description;
            this.hoursBeforeReservation = hoursBeforeReservation;
            this.flatFee = flatFee;
            this.percentageFee = percentageFee;
            this.allowsFreeCancellation = allowsFreeCancellation;
            this.freeCancellationHours = freeCancellationHours;
        }

        // Getters
        public String getPolicyId() { return policyId; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public int getHoursBeforeReservation() { return hoursBeforeReservation; }
        public Money getFlatFee() { return flatFee; }
        public double getPercentageFee() { return percentageFee; }
        public boolean isAllowsFreeCancellation() { return allowsFreeCancellation; }
        public int getFreeCancellationHours() { return freeCancellationHours; }
    }
}