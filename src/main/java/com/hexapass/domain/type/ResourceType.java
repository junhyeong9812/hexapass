package com.hexapass.domain.type;

/**
 * 예약 가능한 리소스 타입을 나타내는 열거형
 */
public enum ResourceType {
    // 헬스장 관련
    GYM("헬스장", "헬스 기구를 이용할 수 있는 공간"),
    POOL("수영장", "수영을 할 수 있는 풀장"),
    SAUNA("사우나", "사우나 시설"),

    // 스터디/업무 공간
    STUDY_ROOM("스터디룸", "개인 또는 그룹 스터디용 룸"),
    MEETING_ROOM("회의실", "회의나 미팅용 공간"),
    OFFICE_DESK("오피스 데스크", "개인 업무용 데스크"),

    // 스포츠 시설
    TENNIS_COURT("테니스 코트", "테니스 경기용 코트"),
    BADMINTON_COURT("배드민턴 코트", "배드민턴 경기용 코트"),
    BASKETBALL_COURT("농구 코트", "농구 경기용 코트"),

    // 교육/강의
    CLASS_ROOM("강의실", "강의나 교육용 공간"),
    SEMINAR_ROOM("세미나실", "세미나나 워크샵용 공간"),

    // 기타
    PARKING_SPACE("주차공간", "차량 주차용 공간");

    private final String displayName;
    private final String description;

    ResourceType(String displayName, String description) {
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
     * 헬스장 관련 리소스인지 확인
     */
    public boolean isFitnessRelated() {
        return this == GYM || this == POOL || this == SAUNA;
    }

    /**
     * 스터디/업무 공간인지 확인
     */
    public boolean isWorkspaceRelated() {
        return this == STUDY_ROOM || this == MEETING_ROOM || this == OFFICE_DESK;
    }

    /**
     * 스포츠 시설인지 확인
     */
    public boolean isSportsRelated() {
        return this == TENNIS_COURT || this == BADMINTON_COURT || this == BASKETBALL_COURT;
    }

    /**
     * 교육 시설인지 확인
     */
    public boolean isEducationRelated() {
        return this == CLASS_ROOM || this == SEMINAR_ROOM;
    }
}