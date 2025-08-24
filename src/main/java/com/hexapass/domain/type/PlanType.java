package com.hexapass.domain.type;

/**
 * 멤버십 플랜 타입을 나타내는 열거형
 */
public enum PlanType {
    MONTHLY("월간권", 30, "한 달 단위로 자동 갱신되는 멤버십"),
    YEARLY("연간권", 365, "일 년 단위로 갱신되는 멤버십"),
    PERIOD("기간제", 0, "특정 기간 동안만 유효한 멤버십"); // 기간은 별도 지정

    private final String displayName;
    private final int defaultDays;
    private final String description;

    PlanType(String displayName, int defaultDays, String description) {
        this.displayName = displayName;
        this.defaultDays = defaultDays;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getDefaultDays() {
        return defaultDays;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 자동 갱신 타입인지 확인
     */
    public boolean isAutoRenewable() {
        return this == MONTHLY || this == YEARLY;
    }

    /**
     * 기간제 타입인지 확인
     */
    public boolean isPeriodType() {
        return this == PERIOD;
    }

    /**
     * 지정된 기간이 이 플랜 타입에 적합한지 확인
     */
    public boolean isValidDuration(int days) {
        if (days <= 0) {
            return false;
        }

        switch (this) {
            case MONTHLY:
                return days >= 28 && days <= 31; // 한 달 범위
            case YEARLY:
                return days >= 365 && days <= 366; // 일 년 범위 (윤년 고려)
            case PERIOD:
                return days >= 1; // 기간제는 1일 이상이면 OK
            default:
                return false;
        }
    }
}