package com.hexapass.domain.type;

/**
 * 회원 상태를 나타내는 열거형
 */
public enum MemberStatus {
    ACTIVE("활성", "정상적으로 서비스를 이용할 수 있는 상태"),
    SUSPENDED("정지", "일시적으로 서비스 이용이 제한된 상태"),
    WITHDRAWN("탈퇴", "서비스에서 탈퇴한 상태");

    private final String displayName;
    private final String description;

    MemberStatus(String displayName, String description) {
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
     * 활성 상태인지 확인
     */
    public boolean isActive() {
        return this == ACTIVE;
    }

    /**
     * 서비스 이용 가능한 상태인지 확인
     */
    public boolean canUseService() {
        return this == ACTIVE;
    }

    /**
     * 다른 상태로 전환 가능한지 확인
     */
    public boolean canTransitionTo(MemberStatus newStatus) {
        switch (this) {
            case ACTIVE:
                return newStatus == SUSPENDED || newStatus == WITHDRAWN;
            case SUSPENDED:
                return newStatus == ACTIVE || newStatus == WITHDRAWN;
            case WITHDRAWN:
                return false; // 탈퇴 상태에서는 다른 상태로 전환 불가
            default:
                return false;
        }
    }
}
