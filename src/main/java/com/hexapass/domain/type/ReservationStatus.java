package com.hexapass.domain.type;

/**
 * 예약 상태를 나타내는 열거형
 */
public enum ReservationStatus {
    REQUESTED("예약요청", "예약이 요청되었지만 아직 확정되지 않은 상태"),
    CONFIRMED("예약확정", "예약이 확정되어 이용 가능한 상태"),
    IN_USE("사용중", "현재 예약된 리소스를 사용하고 있는 상태"),
    COMPLETED("사용완료", "예약된 서비스 이용이 정상적으로 완료된 상태"),
    CANCELLED("예약취소", "예약이 취소된 상태");

    private final String displayName;
    private final String description;

    ReservationStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 활성 예약 상태인지 확인 (취소되지 않고 아직 완료되지 않은 상태)
     */
    public boolean isActive() {
        return this == CONFIRMED || this == IN_USE;
    }

    /**
     * 최종 상태인지 확인 (더 이상 변경될 수 없는 상태)
     */
    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED;
    }

    /**
     * 취소 가능한 상태인지 확인
     */
    public boolean isCancellable() {
        return this == REQUESTED || this == CONFIRMED || this == IN_USE;
    }

    /**
     * 다른 상태로 전환 가능한지 확인
     */
    public boolean canTransitionTo(ReservationStatus newStatus) {
        switch (this) {
            case REQUESTED:
                return newStatus == CONFIRMED || newStatus == CANCELLED;
            case CONFIRMED:
                return newStatus == IN_USE || newStatus == CANCELLED;
            case IN_USE:
                return newStatus == COMPLETED || newStatus == CANCELLED;
            case COMPLETED:
            case CANCELLED:
                return false; // 최종 상태에서는 다른 상태로 전환 불가
            default:
                return false;
        }
    }
}