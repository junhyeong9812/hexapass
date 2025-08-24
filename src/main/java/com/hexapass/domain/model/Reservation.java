package com.hexapass.domain.model;

import com.hexapass.domain.common.TimeSlot;
import com.hexapass.domain.type.ReservationStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 예약을 나타내는 엔티티
 * 특정 회원이 특정 시간에 특정 리소스를 이용하기 위한 예약
 * reservationId를 기준으로 동일성 판단
 */
public class Reservation {

    private final String reservationId;
    private final String memberId;
    private final String resourceId;
    private final TimeSlot timeSlot;
    private final LocalDateTime createdAt;
    private ReservationStatus status;
    private LocalDateTime confirmedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;
    private String cancellationReason;
    private final List<StatusChangeHistory> statusHistory;
    private String notes; // 예약 관련 메모

    /**
     * 상태 변경 이력을 기록하는 내부 클래스
     */
    public static class StatusChangeHistory {
        private final ReservationStatus fromStatus;
        private final ReservationStatus toStatus;
        private final LocalDateTime changedAt;
        private final String reason;

        public StatusChangeHistory(ReservationStatus fromStatus, ReservationStatus toStatus,
                                   LocalDateTime changedAt, String reason) {
            this.fromStatus = fromStatus;
            this.toStatus = toStatus;
            this.changedAt = changedAt;
            this.reason = reason;
        }

        // Getter methods
        public ReservationStatus getFromStatus() { return fromStatus; }
        public ReservationStatus getToStatus() { return toStatus; }
        public LocalDateTime getChangedAt() { return changedAt; }
        public String getReason() { return reason; }

        @Override
        public String toString() {
            return String.format("%s -> %s (%s) %s",
                    fromStatus, toStatus, changedAt, reason != null ? "[" + reason + "]" : "");
        }
    }

    /**
     * private 생성자 - 팩토리 메서드를 통해서만 생성 가능
     */
    private Reservation(String reservationId, String memberId, String resourceId, TimeSlot timeSlot) {
        this.reservationId = validateNotBlank(reservationId, "예약 ID");
        this.memberId = validateNotBlank(memberId, "회원 ID");
        this.resourceId = validateNotBlank(resourceId, "리소스 ID");
        this.timeSlot = validateNotNull(timeSlot, "예약 시간대");
        this.createdAt = LocalDateTime.now();
        this.status = ReservationStatus.REQUESTED;
        this.statusHistory = new ArrayList<>();

        validateReservationTime();

        // 초기 상태 이력 기록
        addStatusHistory(null, ReservationStatus.REQUESTED, "예약 생성");
    }

    /**
     * 예약 생성 팩토리 메서드
     */
    public static Reservation create(String reservationId, String memberId,
                                     String resourceId, TimeSlot timeSlot) {
        return new Reservation(reservationId, memberId, resourceId, timeSlot);
    }

    /**
     * 메모가 포함된 예약 생성
     */
    public static Reservation createWithNotes(String reservationId, String memberId,
                                              String resourceId, TimeSlot timeSlot, String notes) {
        Reservation reservation = new Reservation(reservationId, memberId, resourceId, timeSlot);
        reservation.notes = notes;
        return reservation;
    }

    // =========================
    // 예약 상태 변경 메서드들
    // =========================

    /**
     * 예약 확정
     */
    public void confirm() {
        validateStatusTransition(ReservationStatus.CONFIRMED);

        ReservationStatus oldStatus = this.status;
        this.status = ReservationStatus.CONFIRMED;
        this.confirmedAt = LocalDateTime.now();

        addStatusHistory(oldStatus, ReservationStatus.CONFIRMED, "예약 확정");
    }

    /**
     * 사용 시작
     */
    public void startUsing() {
        validateStatusTransition(ReservationStatus.IN_USE);

        ReservationStatus oldStatus = this.status;
        this.status = ReservationStatus.IN_USE;
        this.startedAt = LocalDateTime.now();

        addStatusHistory(oldStatus, ReservationStatus.IN_USE, "사용 시작");
    }

    /**
     * 사용 완료
     */
    public void complete() {
        validateStatusTransition(ReservationStatus.COMPLETED);

        ReservationStatus oldStatus = this.status;
        this.status = ReservationStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();

        addStatusHistory(oldStatus, ReservationStatus.COMPLETED, "사용 완료");
    }

    /**
     * 예약 취소
     */
    public void cancel(String reason) {
        if (!status.isCancellable()) {
            throw new IllegalStateException("현재 상태에서는 취소할 수 없습니다: " + status);
        }

        validateNotBlank(reason, "취소 사유");

        ReservationStatus oldStatus = this.status;
        this.status = ReservationStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancellationReason = reason;

        addStatusHistory(oldStatus, ReservationStatus.CANCELLED, "예약 취소: " + reason);
    }

    /**
     * 자동 취소 (시스템에 의한 취소)
     */
    public void autoCancel(String systemReason) {
        cancel("시스템 자동 취소: " + systemReason);
    }

    // =========================
    // 예약 정보 확인 메서드들
    // =========================

    /**
     * 활성 예약인지 확인 (취소되지 않고 아직 완료되지 않은 상태)
     */
    public boolean isActive() {
        return status.isActive();
    }

    /**
     * 최종 상태인지 확인
     */
    public boolean isFinal() {
        return status.isFinal();
    }

    /**
     * 취소 가능한 상태인지 확인
     */
    public boolean isCancellable() {
        return status.isCancellable();
    }

    /**
     * 다른 예약과 시간 충돌하는지 확인
     */
    public boolean conflictsWith(Reservation other) {
        if (other == null) {
            return false;
        }

        // 같은 리소스이고 시간이 겹치는 경우 충돌
        return this.resourceId.equals(other.resourceId) &&
                this.timeSlot.overlaps(other.timeSlot);
    }

    /**
     * 지정된 시간대와 충돌하는지 확인
     */
    public boolean conflictsWith(TimeSlot otherTimeSlot) {
        return this.timeSlot.overlaps(otherTimeSlot);
    }

    /**
     * 노쇼(No-show) 여부 확인 - 예약 시간이 지났는데 사용하지 않은 경우
     */
    public boolean isNoShow() {
        if (status != ReservationStatus.CONFIRMED) {
            return false; // 확정된 예약만 노쇼 판정 가능
        }

        return timeSlot.isPast();
    }

    /**
     * 예약 시간까지 남은 시간 (분 단위)
     */
    public long getMinutesUntilReservation() {
        LocalDateTime now = LocalDateTime.now();
        if (timeSlot.getStartTime().isBefore(now)) {
            return 0; // 이미 지난 시간
        }

        return java.time.Duration.between(now, timeSlot.getStartTime()).toMinutes();
    }

    /**
     * 예약 변경 가능 시간인지 확인 (예약 시작 시간 1시간 전까지)
     */
    public boolean isModifiable() {
        if (isFinal()) {
            return false; // 최종 상태는 수정 불가
        }

        return getMinutesUntilReservation() > 60; // 1시간 전까지만 수정 가능
    }

    // =========================
    // 예약 정보 조회 메서드들
    // =========================

    /**
     * 예약 소요 시간 (분 단위)
     */
    public long getDurationMinutes() {
        return timeSlot.getDurationMinutes();
    }

    /**
     * 예약 정보 요약
     */
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(String.format("예약 %s - 회원 %s, 리소스 %s",
                reservationId, memberId, resourceId));
        summary.append(String.format(" | %s | %s", timeSlot, status.getDisplayName()));

        if (status == ReservationStatus.CANCELLED && cancellationReason != null) {
            summary.append(" [").append(cancellationReason).append("]");
        }

        if (notes != null && !notes.trim().isEmpty()) {
            summary.append(" | 메모: ").append(notes);
        }

        return summary.toString();
    }

    /**
     * 상태 변경 이력 조회
     */
    public List<StatusChangeHistory> getStatusHistory() {
        return List.copyOf(statusHistory); // 불변 복사본 반환
    }

    /**
     * 최근 상태 변경 정보
     */
    public StatusChangeHistory getLatestStatusChange() {
        if (statusHistory.isEmpty()) {
            return null;
        }
        return statusHistory.get(statusHistory.size() - 1);
    }

    // =========================
    // 예약 메타데이터 관리
    // =========================

    /**
     * 메모 설정
     */
    public void setNotes(String notes) {
        this.notes = notes != null ? notes.trim() : null;
    }

    /**
     * 메모 추가
     */
    public void addNotes(String additionalNotes) {
        if (additionalNotes == null || additionalNotes.trim().isEmpty()) {
            return;
        }

        if (this.notes == null || this.notes.isEmpty()) {
            this.notes = additionalNotes.trim();
        } else {
            this.notes = this.notes + " | " + additionalNotes.trim();
        }
    }

    // =========================
    // Object 메서드 오버라이드
    // =========================

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        Reservation that = (Reservation) obj;
        return Objects.equals(reservationId, that.reservationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reservationId);
    }

    @Override
    public String toString() {
        return String.format("Reservation{id='%s', member='%s', resource='%s', timeSlot=%s, status=%s}",
                reservationId, memberId, resourceId, timeSlot, status);
    }

    // =========================
    // Getter 메서드들
    // =========================

    public String getReservationId() {
        return reservationId;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getResourceId() {
        return resourceId;
    }

    public TimeSlot getTimeSlot() {
        return timeSlot;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public String getNotes() {
        return notes;
    }

    // =========================
    // 헬퍼 메서드들 (private)
    // =========================

    private void validateStatusTransition(ReservationStatus newStatus) {
        if (!status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    String.format("예약 상태를 %s에서 %s로 변경할 수 없습니다",
                            status.getDisplayName(), newStatus.getDisplayName()));
        }
    }

    private void addStatusHistory(ReservationStatus fromStatus, ReservationStatus toStatus, String reason) {
        StatusChangeHistory history = new StatusChangeHistory(fromStatus, toStatus, LocalDateTime.now(), reason);
        statusHistory.add(history);
    }

    private void validateReservationTime() {
        LocalDateTime now = LocalDateTime.now();
        if (timeSlot.getStartTime().isBefore(now)) {
            throw new IllegalArgumentException(
                    String.format("과거 시간으로는 예약할 수 없습니다. 예약 시간: %s, 현재 시간: %s",
                            timeSlot.getStartTime(), now));
        }

        // 예약 시간이 너무 멀리 있는 경우도 제한 (예: 1년 후)
        if (timeSlot.getStartTime().isAfter(now.plusDays(365))) {
            throw new IllegalArgumentException("예약은 최대 1년 후까지만 가능합니다");
        }
    }

    // =========================
    // 검증 메서드들 (private)
    // =========================

    private String validateNotBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + "은 null이거나 빈 값일 수 없습니다");
        }
        return value.trim();
    }

    private <T> T validateNotNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + "은 null일 수 없습니다");
        }
        return value;
    }
}